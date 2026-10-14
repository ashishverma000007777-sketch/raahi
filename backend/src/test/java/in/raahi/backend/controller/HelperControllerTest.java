package in.raahi.backend.controller;

import in.raahi.backend.entity.HelperApplication;
import in.raahi.backend.entity.User;
import in.raahi.backend.entity.VerificationDocument;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.ai.AiTamperScreeningProvider;
import in.raahi.backend.repository.HelperApplicationRepository;
import in.raahi.backend.repository.MechanicProfileRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.repository.VerificationDocumentRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HelperControllerTest {

    private HelperApplicationRepository applicationRepository;
    private VerificationDocumentRepository documentRepository;
    private UserRepository userRepository;
    private MechanicProfileRepository mechanicProfileRepository;
    private AiTamperScreeningProvider aiTamperScreeningProvider;
    private RateLimiter rateLimiter;
    private HelperController controller;

    private in.raahi.backend.service.AdminGuard adminGuard;
    private in.raahi.backend.service.AuditService audit;

    private UUID ownerId;
    private UUID strangerId;
    private User owner;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(HelperApplicationRepository.class);
        documentRepository = mock(VerificationDocumentRepository.class);
        userRepository = mock(UserRepository.class);
        mechanicProfileRepository = mock(MechanicProfileRepository.class);
        aiTamperScreeningProvider = mock(AiTamperScreeningProvider.class);
        rateLimiter = mock(RateLimiter.class);
        adminGuard = mock(in.raahi.backend.service.AdminGuard.class);
        audit = mock(in.raahi.backend.service.AuditService.class);

        controller = new HelperController(
                applicationRepository,
                documentRepository,
                userRepository,
                mechanicProfileRepository,
                aiTamperScreeningProvider,
                rateLimiter,
                audit,
                adminGuard
        );

        ownerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();

        owner = new User();
        owner.setId(ownerId);
        owner.setName("Owner User");
        owner.setPhone("+919999999999");
        owner.setEmail("owner@raahi.in");
        owner.setEmailVerified(true);

        when(rateLimiter.allow(anyString(), anyInt(), any())).thenReturn(true);
    }

    @Test
    void testGetDocumentForbiddenForStranger() {
        UUID docId = UUID.randomUUID();
        VerificationDocument doc = new VerificationDocument();
        doc.setId(docId);
        doc.setUser(owner);
        doc.setDocType(VerificationDocument.DocType.AADHAAR_FRONT);
        doc.setContentType("image/png");
        doc.setContent(new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        when(documentRepository.findByIdWithUser(docId)).thenReturn(Optional.of(doc));

        AuthenticatedUser stranger = new AuthenticatedUser(strangerId, "DRIVER");
        when(adminGuard.require(stranger)).thenThrow(ApiException.forbidden("FORBIDDEN", "Admin access required"));

        ApiException ex = assertThrows(ApiException.class, () -> controller.getDocument(stranger, docId));
        assertEquals(HttpStatus.FORBIDDEN, ex.status);
        assertEquals("FORBIDDEN", ex.code);
    }

    @Test
    void testGetDocumentAllowedForOwner() {
        UUID docId = UUID.randomUUID();
        byte[] content = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        VerificationDocument doc = new VerificationDocument();
        doc.setId(docId);
        doc.setUser(owner);
        doc.setDocType(VerificationDocument.DocType.AADHAAR_FRONT);
        doc.setContentType("image/png");
        doc.setContent(content);

        when(documentRepository.findByIdWithUser(docId)).thenReturn(Optional.of(doc));

        AuthenticatedUser ownerPrincipal = new AuthenticatedUser(ownerId, "DRIVER");

        ResponseEntity<byte[]> response = controller.getDocument(ownerPrincipal, docId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(content, response.getBody());
        assertTrue(response.getHeaders().getFirst("Cache-Control").contains("no-store"));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
    }

    @Test
    void testGetDocumentAllowedForAdmin() {
        UUID docId = UUID.randomUUID();
        byte[] content = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        VerificationDocument doc = new VerificationDocument();
        doc.setId(docId);
        doc.setUser(owner);
        doc.setDocType(VerificationDocument.DocType.AADHAAR_FRONT);
        doc.setContentType("image/png");
        doc.setContent(content);

        when(documentRepository.findByIdWithUser(docId)).thenReturn(Optional.of(doc));

        AuthenticatedUser adminPrincipal = new AuthenticatedUser(strangerId, "ADMIN");
        User adminUser = new User();
        adminUser.setId(strangerId);
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setStatus(User.Status.ACTIVE);
        when(adminGuard.require(adminPrincipal)).thenReturn(adminUser);

        ResponseEntity<byte[]> response = controller.getDocument(adminPrincipal, docId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(content, response.getBody());
    }

    @Test
    void testGetAdminDocumentAuthorized() {
        UUID docId = UUID.randomUUID();
        byte[] content = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        VerificationDocument doc = new VerificationDocument();
        doc.setId(docId);
        doc.setUser(owner);
        doc.setDocType(VerificationDocument.DocType.AADHAAR_BACK);
        doc.setContentType("image/png");
        doc.setContent(content);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        AuthenticatedUser adminPrincipal = new AuthenticatedUser(strangerId, "ADMIN");
        User adminUser = new User();
        adminUser.setId(strangerId);
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setStatus(User.Status.ACTIVE);
        when(adminGuard.require(adminPrincipal)).thenReturn(adminUser);

        ResponseEntity<byte[]> response = controller.getAdminDocument(adminPrincipal, docId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(content, response.getBody());
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("default-src 'none'", response.getHeaders().getFirst("Content-Security-Policy"));
    }

    @Test
    void testGetAdminDocumentForbiddenForNonAdmin() {
        UUID docId = UUID.randomUUID();
        AuthenticatedUser stranger = new AuthenticatedUser(strangerId, "DRIVER");
        when(adminGuard.require(stranger)).thenThrow(ApiException.forbidden("FORBIDDEN", "Admin access required"));

        ApiException ex = assertThrows(ApiException.class, () -> controller.getAdminDocument(stranger, docId));
        assertEquals(HttpStatus.FORBIDDEN, ex.status);
    }

    @Test
    void testApplySucceedsWithUnverifiedEmail() throws java.io.IOException {
        AuthenticatedUser principal = new AuthenticatedUser(ownerId, "DRIVER");
        owner.setEmailVerified(false);
        owner.setPhone("+919999999999");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(aiTamperScreeningProvider.screen(any(), any())).thenReturn(
                new in.raahi.backend.ai.AiTamperScreeningProvider.ScreeningResult(
                        in.raahi.backend.entity.VerificationDocument.AiStatus.LIKELY_UNMODIFIED, "clean"));
        when(documentRepository.save(any(VerificationDocument.class))).thenAnswer(i -> {
            VerificationDocument d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        byte[] validJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0};
        byte[] content = new byte[200];
        System.arraycopy(validJpeg, 0, content, 0, validJpeg.length);

        MockMultipartFile file1 = new MockMultipartFile("aadhaarFront", "aadhaar.jpg", "image/jpeg", content);
        MockMultipartFile file2 = new MockMultipartFile("aadhaarBack", "aadhaar_back.jpg", "image/jpeg", content);

        when(applicationRepository.save(any(HelperApplication.class))).thenAnswer(i -> {
            HelperApplication a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        var res = controller.apply(principal, "test@raahi.in", 5, "Chandigarh", "FLAT_TYRE", "Jack", "Van",
                "Maruti", "Eeco", null, "CH01AB1234", 15, "owner@upi", null, null, null,
                file1, file2, null);

        assertNotNull(res);
        assertTrue(res.success);
        assertEquals("PENDING", res.data.status);
        assertFalse(res.data.emailVerified);
    }

    @Test
    void testApplyWithUnverifiedEmailStillEnforcesKycDocuments() {
        AuthenticatedUser principal = new AuthenticatedUser(ownerId, "DRIVER");
        owner.setEmailVerified(false);
        owner.setPhone("+919999999999");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        // Missing document (empty)
        MockMultipartFile emptyFile = new MockMultipartFile("aadhaarFront", "aadhaar.jpg", "image/jpeg", new byte[0]);
        ApiException ex = assertThrows(ApiException.class, () ->
                controller.apply(principal, "test@raahi.in", 5, "Chandigarh", "FLAT_TYRE", "Jack", "Van",
                        "Maruti", "Eeco", null, "CH01AB1234", 15, "owner@upi", null, null, null,
                        emptyFile, emptyFile, null));
        assertEquals("MISSING_DOCUMENT", ex.code);
    }

    @Test
    void testApplyWithUnverifiedEmailStillEnforcesVehicleAndPayout() {
        AuthenticatedUser principal = new AuthenticatedUser(ownerId, "DRIVER");
        owner.setEmailVerified(false);
        owner.setPhone("+919999999999");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        byte[] validJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0};
        byte[] content = new byte[200];
        System.arraycopy(validJpeg, 0, content, 0, validJpeg.length);
        MockMultipartFile file1 = new MockMultipartFile("aadhaarFront", "aadhaar.jpg", "image/jpeg", content);
        MockMultipartFile file2 = new MockMultipartFile("aadhaarBack", "aadhaar_back.jpg", "image/jpeg", content);

        // Invalid registration number
        ApiException ex = assertThrows(ApiException.class, () ->
                controller.apply(principal, "test@raahi.in", 5, "Chandigarh", "FLAT_TYRE", "Jack", "Van",
                        "Maruti", "Eeco", null, "INVALID_REG", 15, "owner@upi", null, null, null,
                        file1, file2, null));
        assertEquals("INVALID_VEHICLE_REG", ex.code);
    }

    @Test
    void testApplyWithUnverifiedEmailRejectsInvalidPayoutDetails() {
        AuthenticatedUser principal = new AuthenticatedUser(ownerId, "DRIVER");
        owner.setEmailVerified(false);
        owner.setPhone("+919999999999");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        byte[] validJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0};
        byte[] content = new byte[200];
        System.arraycopy(validJpeg, 0, content, 0, validJpeg.length);
        MockMultipartFile file1 = new MockMultipartFile("aadhaarFront", "aadhaar.jpg", "image/jpeg", content);
        MockMultipartFile file2 = new MockMultipartFile("aadhaarBack", "aadhaar_back.jpg", "image/jpeg", content);

        // Invalid UPI format
        ApiException ex = assertThrows(ApiException.class, () ->
                controller.apply(principal, "test@raahi.in", 5, "Chandigarh", "FLAT_TYRE", "Jack", "Van",
                        "Maruti", "Eeco", null, "CH01AB1234", 15, "invalid_upi_id", null, null, null,
                        file1, file2, null));
        assertEquals("INVALID_UPI", ex.code);
    }

    @Test
    void testApplyRequiresPhoneVerified() {
        AuthenticatedUser principal = new AuthenticatedUser(ownerId, "DRIVER");
        owner.setEmailVerified(true);
        owner.setPhone(null);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile("aadhaarFront", "aadhaar.jpg", "image/jpeg", new byte[100]);
        ApiException ex = assertThrows(ApiException.class, () ->
                controller.apply(principal, "test@raahi.in", 5, "Chandigarh", "FLAT_TYRE", "Jack", "Van",
                        "Maruti", "Eeco", null, "CH01AB1234", 15, "owner@upi", null, null, null,
                        file, file, file));
        assertEquals("PHONE_NOT_VERIFIED", ex.code);
    }

    @Test
    void testApplyRejectsFakeFileSignature() {
        AuthenticatedUser principal = new AuthenticatedUser(ownerId, "DRIVER");
        owner.setEmailVerified(true);
        owner.setPhone("+919999999999");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        // Fake image with text content (> 100 bytes to pass minimum file size check)
        byte[] fakeBytes = ("<html><head><title>Fake Document</title></head><body>" + "X".repeat(120) + "</body></html>").getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile("aadhaarFront", "aadhaar.jpg", "image/jpeg", fakeBytes);

        byte[] validBackBytes = new byte[256];
        validBackBytes[0] = (byte) 0xFF;
        validBackBytes[1] = (byte) 0xD8;
        validBackBytes[2] = (byte) 0xFF;
        validBackBytes[3] = (byte) 0xE0;
        MockMultipartFile validBack = new MockMultipartFile("aadhaarBack", "aadhaar_back.jpg", "image/jpeg", validBackBytes);

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.apply(principal, "test@raahi.in", 5, "Chandigarh", "FLAT_TYRE,BATTERY", "Jack", "Van",
                        "Maruti", "Eeco", null, "CH01AB1234", 15, "owner@upi", null, null, null,
                        fakeFile, validBack, validBack));

        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
        assertEquals("INVALID_DOCUMENT_SIGNATURE", ex.code);
    }

    @Test
    void testApproveEnforcesPolicyAndLogsAudit() {
        UUID appId = UUID.randomUUID();
        HelperApplication app = new HelperApplication();
        app.setId(appId);
        app.setUser(owner);
        app.setStatus(HelperApplication.Status.PENDING);

        VerificationDocument front = new VerificationDocument();
        front.setId(UUID.randomUUID());
        front.setDocType(VerificationDocument.DocType.AADHAAR_FRONT);
        front.setAiStatus(VerificationDocument.AiStatus.UNAVAILABLE);
        app.setAadhaarFront(front);

        VerificationDocument back = new VerificationDocument();
        back.setId(UUID.randomUUID());
        back.setDocType(VerificationDocument.DocType.AADHAAR_BACK);
        back.setAiStatus(VerificationDocument.AiStatus.LIKELY_UNMODIFIED);
        app.setAadhaarBack(back);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        AuthenticatedUser adminPrincipal = new AuthenticatedUser(strangerId, "ADMIN");
        User adminUser = new User();
        adminUser.setId(strangerId);
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setStatus(User.Status.ACTIVE);
        when(adminGuard.require(adminPrincipal)).thenReturn(adminUser);

        var res = controller.approve(adminPrincipal, appId);
        assertEquals("APPROVED", res.data.status);
        assertEquals(User.Role.HELPER, owner.getRole());
        assertTrue(owner.isVerified());

        verify(audit).log(eq(strangerId), eq("ADMIN"), eq("HELPER_APPROVED"), eq("HELPER_APPLICATION"), eq(appId.toString()), anyString());
    }

    @Test
    void testApproveSucceedsWithUnverifiedEmail() {
        UUID appId = UUID.randomUUID();
        owner.setEmailVerified(false);
        owner.setPhone("+919999999999");
        HelperApplication app = new HelperApplication();
        app.setId(appId);
        app.setUser(owner);
        app.setStatus(HelperApplication.Status.PENDING);
        app.setAadhaarFront(new VerificationDocument());
        app.setAadhaarBack(new VerificationDocument());

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        AuthenticatedUser adminPrincipal = new AuthenticatedUser(strangerId, "ADMIN");
        User adminUser = new User();
        adminUser.setId(strangerId);
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setStatus(User.Status.ACTIVE);
        when(adminGuard.require(adminPrincipal)).thenReturn(adminUser);
        when(applicationRepository.save(any(HelperApplication.class))).thenAnswer(i -> i.getArgument(0));

        var res = controller.approve(adminPrincipal, appId);
        assertNotNull(res);
        assertTrue(res.success);
        assertEquals("APPROVED", res.data.status);
        assertEquals(User.Role.HELPER, owner.getRole());
        assertTrue(owner.isVerified());
        assertFalse(owner.isEmailVerified());
    }

    @Test
    void testApproveForbiddenForNonAdmin() {
        UUID appId = UUID.randomUUID();
        AuthenticatedUser driver = new AuthenticatedUser(strangerId, "DRIVER");
        when(adminGuard.require(driver)).thenThrow(ApiException.forbidden("FORBIDDEN", "Admin access required"));

        ApiException ex = assertThrows(ApiException.class, () -> controller.approve(driver, appId));
        assertEquals(HttpStatus.FORBIDDEN, ex.status);
        assertEquals("FORBIDDEN", ex.code);
    }

    @Test
    void testApproveRejectsSuspiciousDocument() {
        UUID appId = UUID.randomUUID();
        HelperApplication app = new HelperApplication();
        app.setUser(owner);
        app.setStatus(HelperApplication.Status.PENDING);

        VerificationDocument front = new VerificationDocument();
        front.setAiStatus(VerificationDocument.AiStatus.SUSPICIOUS);
        app.setAadhaarFront(front);
        app.setAadhaarBack(new VerificationDocument());

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        AuthenticatedUser adminPrincipal = new AuthenticatedUser(strangerId, "ADMIN");
        User adminUser = new User();
        adminUser.setId(strangerId);
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setStatus(User.Status.ACTIVE);
        when(adminGuard.require(adminPrincipal)).thenReturn(adminUser);

        ApiException ex = assertThrows(ApiException.class, () -> controller.approve(adminPrincipal, appId));
        assertEquals("UNRESOLVED_DOCUMENT_RISK", ex.code);
    }

    @Test
    void testRejectRequiresReasonAndLogsAudit() {
        UUID appId = UUID.randomUUID();
        HelperApplication app = new HelperApplication();
        app.setId(appId);
        app.setUser(owner);
        app.setStatus(HelperApplication.Status.PENDING);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        AuthenticatedUser adminPrincipal = new AuthenticatedUser(strangerId, "ADMIN");
        User adminUser = new User();
        adminUser.setId(strangerId);
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setStatus(User.Status.ACTIVE);
        when(adminGuard.require(adminPrincipal)).thenReturn(adminUser);

        var blankReq = new in.raahi.backend.dto.HelperDtos.RejectRequest();
        blankReq.reason = "   ";
        ApiException ex = assertThrows(ApiException.class, () -> controller.reject(adminPrincipal, appId, blankReq));
        assertEquals("REJECTION_REASON_REQUIRED", ex.code);

        var validReq = new in.raahi.backend.dto.HelperDtos.RejectRequest();
        validReq.reason = "Incomplete profile details";
        var res = controller.reject(adminPrincipal, appId, validReq);
        assertEquals("REJECTED", res.data.status);
        assertEquals("Incomplete profile details", res.data.rejectionReason);

        verify(audit).log(eq(strangerId), eq("ADMIN"), eq("HELPER_REJECTED"), eq("HELPER_APPLICATION"), eq(appId.toString()), eq("Incomplete profile details"));
    }

    @Test
    void testRequestResubmissionRequiresReasonAndAllowsReapply() {
        UUID appId = UUID.randomUUID();
        HelperApplication app = new HelperApplication();
        app.setId(appId);
        app.setUser(owner);
        app.setStatus(HelperApplication.Status.PENDING);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        AuthenticatedUser adminPrincipal = new AuthenticatedUser(strangerId, "ADMIN");
        User adminUser = new User();
        adminUser.setId(strangerId);
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setStatus(User.Status.ACTIVE);
        when(adminGuard.require(adminPrincipal)).thenReturn(adminUser);

        var req = new in.raahi.backend.dto.HelperDtos.RejectRequest();
        req.reason = "Aadhaar back is blurry. Please upload a clear photo.";
        var res = controller.requestResubmission(adminPrincipal, appId, req);

        assertEquals("REJECTED", res.data.status);
        assertTrue(res.data.rejectionReason.contains("Resubmission requested"));
        verify(audit).log(eq(strangerId), eq("ADMIN"), eq("HELPER_RESUBMISSION_REQUESTED"), eq("HELPER_APPLICATION"), eq(appId.toString()), eq("Aadhaar back is blurry. Please upload a clear photo."));
    }
}
