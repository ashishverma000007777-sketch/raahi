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

        controller = new HelperController(
                applicationRepository,
                documentRepository,
                userRepository,
                mechanicProfileRepository,
                aiTamperScreeningProvider,
                rateLimiter,
                mock(in.raahi.backend.service.AuditService.class),
                mock(in.raahi.backend.service.AdminGuard.class)
        );

        ownerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();

        owner = new User();
        owner.setId(ownerId);
        owner.setName("Owner User");

        when(rateLimiter.allow(anyString(), anyInt(), any())).thenReturn(true);
    }

    @Test
    void testGetDocumentForbiddenForStranger() {
        UUID docId = UUID.randomUUID();
        VerificationDocument doc = new VerificationDocument();
        doc.setId(docId);
        doc.setUser(owner);
        doc.setContentType("image/png");
        doc.setContent(new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        when(documentRepository.findByIdWithUser(docId)).thenReturn(Optional.of(doc));

        AuthenticatedUser stranger = new AuthenticatedUser(strangerId, "DRIVER");

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

        ResponseEntity<byte[]> response = controller.getDocument(adminPrincipal, docId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(content, response.getBody());
    }

    @Test
    void testApplyRejectsFakeFileSignature() {
        AuthenticatedUser principal = new AuthenticatedUser(ownerId, "DRIVER");
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
}
