package in.raahi.backend.controller;

import in.raahi.backend.ai.AiTamperScreeningProvider;
import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.HelperDtos.*;
import in.raahi.backend.entity.HelperApplication;
import in.raahi.backend.entity.MechanicProfile;
import in.raahi.backend.entity.User;
import in.raahi.backend.entity.VerificationDocument;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.HelperApplicationRepository;
import in.raahi.backend.repository.MechanicProfileRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.repository.VerificationDocumentRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class HelperController {

    private static final long MAX_DOC_BYTES = 8 * 1024 * 1024; // 8MB per image

    private final HelperApplicationRepository applicationRepository;
    private final VerificationDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MechanicProfileRepository mechanicProfileRepository;
    private final AiTamperScreeningProvider aiTamperScreeningProvider;
    private final RateLimiter rateLimiter;
    private final in.raahi.backend.service.AuditService audit;
    private final in.raahi.backend.service.AdminGuard adminGuard;

    public HelperController(HelperApplicationRepository applicationRepository,
                             VerificationDocumentRepository documentRepository,
                             UserRepository userRepository,
                             MechanicProfileRepository mechanicProfileRepository,
                             AiTamperScreeningProvider aiTamperScreeningProvider,
                             RateLimiter rateLimiter,
                             in.raahi.backend.service.AuditService audit,
                             in.raahi.backend.service.AdminGuard adminGuard) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.mechanicProfileRepository = mechanicProfileRepository;
        this.aiTamperScreeningProvider = aiTamperScreeningProvider;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.adminGuard = adminGuard;
    }

    // ---- Applicant-facing ----

    @PostMapping(value = "/api/v1/helper/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ApiResponse<HelperApplicationDto> apply(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer experienceYears,
            @RequestParam(required = false) String serviceArea,
            @RequestParam(required = false) String services,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String vehicleBrand,
            @RequestParam(required = false) String vehicleModel,
            @RequestParam(required = false) String vehicleVariant,
            @RequestParam(required = false) String vehicleReg,
            @RequestParam(required = false) Integer serviceRadiusKm,
            @RequestParam(required = false) String payoutUpi,
            @RequestParam(required = false) String payoutHolder,
            @RequestParam(required = false) String payoutBankAcct,
            @RequestParam(required = false) String payoutIfsc,
            @RequestParam("aadhaarFront") MultipartFile aadhaarFront,
            @RequestParam("aadhaarBack") MultipartFile aadhaarBack,
            @RequestParam("selfie") MultipartFile selfie
    ) throws IOException {
        String rateLimitKey = "helper:apply:" + principal.userId();
        if (!rateLimiter.allow(rateLimitKey, 5, Duration.ofHours(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many application attempts. Please wait.");
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        if (applicationRepository.findByUserId(user.getId())
                .filter(a -> a.getStatus() == HelperApplication.Status.PENDING).isPresent()) {
            throw ApiException.conflict("APPLICATION_PENDING", "You already have an application under review");
        }

        applicationRepository.findByUserId(user.getId()).ifPresent(a -> {
            if (a.getStatus() == HelperApplication.Status.APPROVED) {
                throw ApiException.conflict("ALREADY_APPROVED", "You are already an approved helper");
            }
            if (a.getStatus() == HelperApplication.Status.SUSPENDED) {
                throw ApiException.forbidden("HELPER_SUSPENDED", "Your helper account is suspended. Contact support.");
            }
        });
        String normServices = validateOnboarding(experienceYears, serviceArea, services, vehicleType, vehicleBrand,
                vehicleModel, vehicleReg, serviceRadiusKm, payoutUpi, payoutBankAcct, payoutIfsc, payoutHolder);
        HelperApplication application = applicationRepository.findByUserId(user.getId()).orElseGet(HelperApplication::new);
        application.setUser(user);
        application.setEmail(email);
        application.setExperienceYears(experienceYears);
        application.setServiceArea(serviceArea.trim());
        application.setServices(normServices);
        application.setEquipment(equipment == null ? null : equipment.trim());
        application.setVehicleType(vehicleType.trim());
        application.setVehicleBrand(vehicleBrand.trim());
        application.setVehicleModel(vehicleModel.trim());
        application.setVehicleVariant(vehicleVariant == null ? null : vehicleVariant.trim());
        application.setVehicleReg(vehicleReg.trim().toUpperCase());
        application.setServiceRadiusKm(serviceRadiusKm);
        application.setPayoutUpi(payoutUpi == null || payoutUpi.isBlank() ? null : payoutUpi.trim());
        application.setPayoutHolder(payoutHolder == null ? null : payoutHolder.trim());
        application.setPayoutBankAcct(payoutBankAcct == null || payoutBankAcct.isBlank() ? null : payoutBankAcct.trim());
        application.setPayoutIfsc(payoutIfsc == null || payoutIfsc.isBlank() ? null : payoutIfsc.trim().toUpperCase());
        application.setSuspensionReason(null);
        application.setStatus(HelperApplication.Status.PENDING);
        application.setRejectionReason(null);
        application.setReviewedAt(null);
        application.setReviewedBy(null);

        application.setAadhaarFront(storeDocument(user, VerificationDocument.DocType.AADHAAR_FRONT, aadhaarFront));
        application.setAadhaarBack(storeDocument(user, VerificationDocument.DocType.AADHAAR_BACK, aadhaarBack));
        if (selfie != null && !selfie.isEmpty()) {
            application.setSelfie(storeDocument(user, VerificationDocument.DocType.SELFIE, selfie));
        }

        application = applicationRepository.save(application);
        return ApiResponse.ok(toDto(application, false));
    }

    @GetMapping("/api/v1/helper/application/me")
    @Transactional(readOnly = true)
    public ApiResponse<HelperApplicationDto> myApplication(@AuthenticationPrincipal AuthenticatedUser principal) {
        HelperApplication application = applicationRepository.findByUserId(principal.userId())
                .orElseThrow(() -> ApiException.notFound("NO_APPLICATION", "No application submitted yet"));
        return ApiResponse.ok(toDto(application, false));
    }

    // Serves the raw image bytes for one document. Authorized only to the document's owner
    // or an ADMIN reviewing the application — never any other authenticated user.
    @GetMapping("/api/v1/helper/documents/{docId}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getDocument(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID docId) {
        String rateLimitKey = "helper:doc:view:" + principal.userId();
        if (!rateLimiter.allow(rateLimitKey, 30, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many document download requests. Please wait.");
        }

        VerificationDocument doc = documentRepository.findByIdWithUser(docId)
                .orElseThrow(() -> ApiException.notFound("DOC_NOT_FOUND", "Document not found"));

        boolean isOwner = doc.getUser().getId().equals(principal.userId());
        boolean isAdmin = "ADMIN".equals(principal.role());
        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("FORBIDDEN", "Not authorized to view this document");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getDocType().name().toLowerCase() + "_" + doc.getId() + "\"")
                .body(doc.getContent());
    }

    // ---- Admin-facing ----

    @GetMapping("/api/v1/admin/helper-applications")
    @Transactional(readOnly = true)
    public ApiResponse<List<HelperApplicationDto>> queue(@AuthenticationPrincipal AuthenticatedUser principal) {
        adminGuard.require(principal);
        List<HelperApplication> pending = applicationRepository.findByStatusOrderBySubmittedAtAsc(HelperApplication.Status.PENDING);
        return ApiResponse.ok(pending.stream().map(a -> toDto(a, true)).collect(Collectors.toList()));
    }

    @GetMapping("/api/v1/admin/helper-applications/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<HelperApplicationDto> getForReview(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        adminGuard.require(principal);
        HelperApplication application = applicationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found"));
        return ApiResponse.ok(toDto(application, true));
    }

    @PostMapping("/api/v1/admin/helper-applications/{id}/approve")
    @Transactional
    public ApiResponse<HelperApplicationDto> approve(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        adminGuard.require(principal);
        String rateLimitKey = "admin:review:" + principal.userId();
        if (!rateLimiter.allow(rateLimitKey, 60, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many review requests");
        }

        HelperApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found"));
        if (application.getStatus() != HelperApplication.Status.PENDING) {
            throw ApiException.conflict("ALREADY_REVIEWED", "This application was already reviewed");
        }

        application.setStatus(HelperApplication.Status.APPROVED);
        application.setReviewedAt(Instant.now());
        application.setReviewedBy(userRepository.getReferenceById(principal.userId()));
        applicationRepository.save(application);

        User user = application.getUser();
        user.setRole(User.Role.HELPER);
        user.setVerified(true);
        userRepository.save(user);

        // Synchronize MechanicProfile so approved helper immediately appears in nearby discovery
        MechanicProfile profile = mechanicProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    MechanicProfile mp = new MechanicProfile();
                    mp.setUser(user);
                    return mp;
                });
        profile.setVerificationStatus(MechanicProfile.VerificationStatus.APPROVED);
        profile.setAvailable(true);
        mechanicProfileRepository.save(profile);

        return ApiResponse.ok(toDto(application, true));
    }

    @PostMapping("/api/v1/admin/helper-applications/{id}/reject")
    @Transactional
    public ApiResponse<HelperApplicationDto> reject(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @PathVariable UUID id, @Valid @RequestBody RejectRequest req) {
        adminGuard.require(principal);
        String rateLimitKey = "admin:review:" + principal.userId();
        if (!rateLimiter.allow(rateLimitKey, 60, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many review requests");
        }

        HelperApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found"));
        if (application.getStatus() != HelperApplication.Status.PENDING) {
            throw ApiException.conflict("ALREADY_REVIEWED", "This application was already reviewed");
        }

        application.setStatus(HelperApplication.Status.REJECTED);
        application.setRejectionReason(req.reason);
        application.setReviewedAt(Instant.now());
        application.setReviewedBy(userRepository.getReferenceById(principal.userId()));
        applicationRepository.save(application);

        // If a profile exists, ensure it is marked REJECTED and unavailable
        mechanicProfileRepository.findByUserId(application.getUser().getId()).ifPresent(p -> {
            p.setVerificationStatus(MechanicProfile.VerificationStatus.REJECTED);
            p.setAvailable(false);
            mechanicProfileRepository.save(p);
        });

        return ApiResponse.ok(toDto(application, true));
    }

    private static final java.util.regex.Pattern REG = java.util.regex.Pattern.compile("^[A-Za-z]{2}[0-9]{1,2}[A-Za-z]{0,3}[0-9]{4}$");
    private static final java.util.regex.Pattern UPI = java.util.regex.Pattern.compile("^[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}$");
    private static final java.util.regex.Pattern IFSC = java.util.regex.Pattern.compile("^[A-Za-z]{4}0[A-Za-z0-9]{6}$");
    private static final java.util.regex.Pattern SERVICE = java.util.regex.Pattern.compile("^[A-Z_]{2,30}$");

    /** Server-side validation of the onboarding form; returns the normalized services list. */
    private String validateOnboarding(Integer experienceYears, String serviceArea, String services, String vehicleType,
                                      String vehicleBrand, String vehicleModel, String vehicleReg, Integer radiusKm,
                                      String upi, String bankAcct, String ifsc, String holder) {
        if (experienceYears == null || experienceYears < 0 || experienceYears > 60) {
            throw ApiException.badRequest("INVALID_EXPERIENCE", "Experience must be between 0 and 60 years");
        }
        if (serviceArea == null || serviceArea.isBlank() || serviceArea.length() > 120) {
            throw ApiException.badRequest("INVALID_SERVICE_AREA", "Service area is required");
        }
        if (services == null || services.isBlank()) {
            throw ApiException.badRequest("INVALID_SERVICES", "Select at least one service");
        }
        java.util.List<String> list = java.util.Arrays.stream(services.split(","))
                .map(x -> x.trim().toUpperCase()).filter(x -> !x.isEmpty()).distinct().collect(Collectors.toList());
        if (list.isEmpty() || list.size() > 10 || list.stream().anyMatch(x -> !SERVICE.matcher(x).matches())) {
            throw ApiException.badRequest("INVALID_SERVICES", "Select between 1 and 10 valid services");
        }
        if (vehicleType == null || vehicleType.isBlank() || vehicleBrand == null || vehicleBrand.isBlank()
                || vehicleModel == null || vehicleModel.isBlank()) {
            throw ApiException.badRequest("INVALID_VEHICLE", "Service vehicle type, brand and model are required");
        }
        if (vehicleType.length() > 30 || vehicleBrand.length() > 60 || vehicleModel.length() > 60) {
            throw ApiException.badRequest("INVALID_VEHICLE", "Vehicle details are too long");
        }
        if (vehicleReg == null || !REG.matcher(vehicleReg.trim().replace(" ", "")).matches()) {
            throw ApiException.badRequest("INVALID_VEHICLE_REG", "Enter a valid vehicle registration number");
        }
        if (radiusKm == null || radiusKm < 1 || radiusKm > 100) {
            throw ApiException.badRequest("INVALID_RADIUS", "Service radius must be between 1 and 100 km");
        }
        boolean hasUpi = upi != null && !upi.isBlank();
        boolean hasBank = bankAcct != null && !bankAcct.isBlank();
        if (!hasUpi && !hasBank) {
            throw ApiException.badRequest("INVALID_PAYOUT", "Add a UPI ID or bank account");
        }
        if (hasUpi && !UPI.matcher(upi.trim()).matches()) {
            throw ApiException.badRequest("INVALID_UPI", "Enter a valid UPI ID");
        }
        if (hasBank) {
            if (!bankAcct.trim().matches("^[0-9]{9,18}$")) {
                throw ApiException.badRequest("INVALID_BANK_ACCOUNT", "Enter a valid bank account number");
            }
            if (ifsc == null || !IFSC.matcher(ifsc.trim()).matches()) {
                throw ApiException.badRequest("INVALID_IFSC", "Enter a valid IFSC code");
            }
            if (holder == null || holder.isBlank() || holder.length() > 100) {
                throw ApiException.badRequest("INVALID_HOLDER", "Account holder name is required");
            }
        }
        return String.join(",", list);
    }

    private VerificationDocument storeDocument(User user, VerificationDocument.DocType type, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw ApiException.badRequest("MISSING_DOCUMENT", type + " is required");
        }
        if (file.getSize() > MAX_DOC_BYTES) {
            throw ApiException.badRequest("DOCUMENT_TOO_LARGE", type + " exceeds the 8MB limit");
        }

        byte[] bytes = file.getBytes();
        String validatedContentType = detectAndValidateMimeType(type, bytes);

        AiTamperScreeningProvider.ScreeningResult screening = aiTamperScreeningProvider.screen(bytes, validatedContentType);

        VerificationDocument doc = new VerificationDocument();
        doc.setUser(user);
        doc.setDocType(type);
        doc.setContent(bytes);
        doc.setContentType(validatedContentType);
        doc.setAiStatus(screening.status());
        doc.setAiNote(screening.note());
        return documentRepository.save(doc);
    }

    private String detectAndValidateMimeType(VerificationDocument.DocType type, byte[] bytes) {
        if (bytes == null || bytes.length < 100) {
            throw ApiException.badRequest("INVALID_DOCUMENT_CONTENT", "Uploaded file is too small or corrupted (minimum 100 bytes required)");
        }
        // JPEG magic bytes: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // PNG magic bytes: 89 50 4E 47 (0x89 'P' 'N' 'G')
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        // WebP magic bytes: RIFF ... WEBP
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        // PDF magic bytes: %PDF- (0x25 0x50 0x44 0x46)
        if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            if (type == VerificationDocument.DocType.SELFIE) {
                throw ApiException.badRequest("INVALID_DOCUMENT_FORMAT", "Selfie must be an image (JPEG, PNG, or WebP), not a PDF document");
            }
            return "application/pdf";
        }
        throw ApiException.badRequest("INVALID_DOCUMENT_SIGNATURE",
                "Uploaded file must be a genuine JPEG, PNG, WebP image" + (type == VerificationDocument.DocType.SELFIE ? "" : " or PDF document"));
    }

    private HelperApplicationDto toDto(HelperApplication a, boolean includeApplicant) {
        HelperApplicationDto dto = new HelperApplicationDto();
        dto.id = a.getId().toString();
        dto.status = a.getStatus().name();
        dto.email = a.getEmail();
        dto.rejectionReason = a.getRejectionReason();
        dto.submittedAt = a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : null;
        dto.reviewedAt = a.getReviewedAt() != null ? a.getReviewedAt().toString() : null;
        dto.aadhaarFront = toDocSummary(a.getAadhaarFront());
        dto.aadhaarBack = toDocSummary(a.getAadhaarBack());
        dto.selfie = toDocSummary(a.getSelfie());
        dto.userId = a.getUser().getId().toString();
        dto.suspensionReason = a.getSuspensionReason();
        dto.experienceYears = a.getExperienceYears();
        dto.serviceArea = a.getServiceArea();
        dto.services = a.getServices();
        dto.equipment = a.getEquipment();
        dto.vehicleType = a.getVehicleType();
        dto.vehicleBrand = a.getVehicleBrand();
        dto.vehicleModel = a.getVehicleModel();
        dto.vehicleVariant = a.getVehicleVariant();
        dto.vehicleReg = a.getVehicleReg();
        dto.serviceRadiusKm = a.getServiceRadiusKm();
        dto.payoutUpi = a.getPayoutUpi();
        dto.payoutHolder = a.getPayoutHolder();
        String acct = a.getPayoutBankAcct();
        dto.payoutBankAcctMasked = acct == null ? null : "XXXX" + acct.substring(Math.max(0, acct.length() - 4));
        dto.payoutIfsc = a.getPayoutIfsc();
        if (includeApplicant) {
            dto.applicantName = a.getUser().getName();
            dto.applicantPhone = a.getUser().getPhone();
        }
        return dto;
    }

    private DocSummary toDocSummary(VerificationDocument doc) {
        if (doc == null) return null;
        DocSummary s = new DocSummary();
        s.id = doc.getId().toString();
        s.aiStatus = doc.getAiStatus().name();
        s.aiNote = doc.getAiNote();
        return s;
    }
}
