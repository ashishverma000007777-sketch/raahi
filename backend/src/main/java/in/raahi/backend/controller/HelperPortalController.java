package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.HelperDtos.AvailabilityRequest;
import in.raahi.backend.entity.HelperApplication;
import in.raahi.backend.entity.Job;
import in.raahi.backend.entity.MechanicProfile;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.HelperApplicationRepository;
import in.raahi.backend.repository.JobRepository;
import in.raahi.backend.repository.MechanicProfileRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.service.CommissionMath;
import in.raahi.backend.service.CommissionService;
import in.raahi.backend.service.HelperEligibilityService;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Everything the helper app needs after onboarding: status, online/offline, earnings, commission, history. */
@RestController
@RequestMapping("/api/v1/helper")
public class HelperPortalController {

    private final UserRepository userRepository;
    private final HelperApplicationRepository applicationRepository;
    private final MechanicProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final HelperEligibilityService eligibility;
    private final CommissionService commissionService;
    private final JdbcTemplate jdbc;

    public HelperPortalController(UserRepository userRepository, HelperApplicationRepository applicationRepository,
                                  MechanicProfileRepository profileRepository, JobRepository jobRepository,
                                  HelperEligibilityService eligibility, CommissionService commissionService,
                                  JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.eligibility = eligibility;
        this.commissionService = commissionService;
        this.jdbc = jdbc;
    }

    /** One call that drives the whole helper UI: application state, can-go-online, commission, rating. */
    @GetMapping("/status")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> status(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        HelperApplication app = applicationRepository.findByUserId(user.getId()).orElse(null);
        MechanicProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);

        String state = app == null ? "NONE" : app.getStatus().name();
        if (profile != null && profile.getVerificationStatus() == MechanicProfile.VerificationStatus.SUSPENDED) {
            state = "SUSPENDED";
        }
        double balance = commissionService.balance(user.getId());
        boolean approved = "APPROVED".equals(state)
                && (user.getRole() == User.Role.HELPER || user.getRole() == User.Role.MECHANIC);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("applicationStatus", state);
        out.put("rejectionReason", app == null ? null : app.getRejectionReason());
        out.put("suspensionReason", app == null ? null : app.getSuspensionReason());
        out.put("accountActive", user.getStatus() == User.Status.ACTIVE);
        out.put("blockedUntil", user.isTemporarilyBlocked() ? user.getBlockedUntil().toString() : null);
        out.put("blockReason", user.isTemporarilyBlocked() ? user.getBlockReason() : null);
        out.put("online", profile != null && profile.isAvailable());
        out.put("commissionBalance", balance);
        out.put("commissionDue", CommissionMath.owesMoney(balance));
        out.put("commissionRate", commissionService.rate());
        boolean canAccept = approved && user.getStatus() == User.Status.ACTIVE && !user.isTemporarilyBlocked()
                && commissionService.canAcceptWithBalance(balance);
        out.put("canAcceptJobs", canAccept);
        out.put("ratingAvg", user.getRatingAvg() == null ? 0.0 : user.getRatingAvg());
        out.put("totalHelps", user.getTotalHelps() == null ? 0 : user.getTotalHelps());
        out.put("name", user.getName());
        out.put("phone", user.getPhone());
        out.put("services", app == null ? null : app.getServices());
        out.put("serviceRadiusKm", app == null ? null : app.getServiceRadiusKm());
        return ApiResponse.ok(out);
    }

    /** Online/offline. Going online requires APPROVED + ACTIVE + not blocked. Going offline is always allowed. */
    @PutMapping("/availability")
    @Transactional
    public ApiResponse<Map<String, Object>> availability(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @Valid @RequestBody AvailabilityRequest req) {
        MechanicProfile profile;
        if (Boolean.TRUE.equals(req.online)) {
            User user = eligibility.requireApprovedActive(principal);
            if (req.lat == null || req.lng == null) {
                throw ApiException.badRequest("LOCATION_REQUIRED", "Location is required to go online");
            }
            profile = profileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> ApiException.forbidden("HELPER_NOT_APPROVED", "Your helper account is not approved yet"));
            profile.setCurrentLat(req.lat);
            profile.setCurrentLng(req.lng);
            profile.setAvailable(true);
        } else {
            profile = profileRepository.findByUserId(principal.userId())
                    .orElseThrow(() -> ApiException.notFound("NO_HELPER_PROFILE", "No helper profile"));
            profile.setAvailable(false);
        }
        profileRepository.save(profile);
        return ApiResponse.ok(Map.of("online", profile.isAvailable()));
    }

    @GetMapping("/commission")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> commission(@AuthenticationPrincipal AuthenticatedUser principal) {
        requireHelperRole(principal);
        double balance = commissionService.balance(principal.userId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("balance", balance);
        out.put("due", CommissionMath.owesMoney(balance) ? Math.abs(balance) : 0.0);
        out.put("rate", commissionService.rate());
        out.put("entries", commissionService.entries(principal.userId(), 100));
        return ApiResponse.ok(out);
    }

    /** Earnings summary: customer money paid directly to the helper, minus Raahi commission. */
    @GetMapping("/earnings")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> earnings(@AuthenticationPrincipal AuthenticatedUser principal) {
        requireHelperRole(principal);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT COUNT(*) AS jobs, COALESCE(SUM(COALESCE(final_amount, reward_amount, 0)), 0) AS gross FROM jobs WHERE helper_id = ? AND status = 'COMPLETED'",
                principal.userId());
        double gross = CommissionMath.round2(((Number) row.get("gross")).doubleValue());
        Double commissionSum = jdbc.queryForObject(
                "SELECT COALESCE(-SUM(amount), 0) FROM commission_ledger WHERE helper_id = ? AND entry_type = 'COMMISSION'",
                Double.class, principal.userId());
        double commission = CommissionMath.round2(commissionSum == null ? 0.0 : commissionSum);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("completedJobs", ((Number) row.get("jobs")).longValue());
        out.put("grossEarnings", gross);
        out.put("totalCommission", commission);
        out.put("netEarnings", CommissionMath.round2(gross - commission));
        out.put("commissionBalance", commissionService.balance(principal.userId()));
        return ApiResponse.ok(out);
    }

    @GetMapping("/ratings")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> ratings(@AuthenticationPrincipal AuthenticatedUser principal) {
        requireHelperRole(principal);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT job_id, stars, comment, created_at FROM job_ratings WHERE helper_id = ? ORDER BY created_at DESC LIMIT 50",
                principal.userId()));
    }

    /** Past jobs of this helper (completed + cancelled/expired), newest first. */
    @GetMapping("/jobs/history")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> history(@AuthenticationPrincipal AuthenticatedUser principal) {
        requireHelperRole(principal);
        List<Map<String, Object>> out = jobRepository.findByHelper(principal.userId()).stream()
                .filter(j -> j.getStatus() == Job.Status.COMPLETED || j.getStatus() == Job.Status.CANCELLED
                        || j.getStatus() == Job.Status.EXPIRED)
                .limit(100)
                .map(j -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("jobId", j.getId().toString());
                    m.put("status", j.getStatus().name());
                    m.put("problemType", j.getProblemType());
                    m.put("amount", j.getFinalAmount() != null ? j.getFinalAmount() : j.getRewardAmount());
                    m.put("paymentMode", j.getPaymentMode());
                    m.put("rated", j.isRated());
                    m.put("createdAt", j.getCreatedAt() == null ? null : j.getCreatedAt().toString());
                    m.put("completedAt", j.getCompletedAt() == null ? null : j.getCompletedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(out);
    }

    private void requireHelperRole(AuthenticatedUser principal) {
        User u = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        if (u.getRole() != User.Role.HELPER && u.getRole() != User.Role.MECHANIC) {
            throw ApiException.forbidden("FORBIDDEN", "Helper account required");
        }
    }
}
