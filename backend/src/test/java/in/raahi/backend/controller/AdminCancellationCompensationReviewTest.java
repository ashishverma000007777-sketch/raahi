package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.JobDtos.ApproveCompensationRequest;
import in.raahi.backend.dto.JobDtos.RejectCompensationRequest;
import in.raahi.backend.entity.Job;
import in.raahi.backend.entity.JobCancellationCompensation;
import in.raahi.backend.entity.JobCancellationCompensation.CompensationStatus;
import in.raahi.backend.entity.JobCancellationCompensation.PayoutStatus;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.*;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.service.AdminGuard;
import in.raahi.backend.service.AuditService;
import in.raahi.backend.service.JobHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCancellationCompensationReviewTest {

    private UserRepository userRepository;
    private JobCancellationCompensationRepository compensationRepository;
    private AuditService audit;
    private AdminController controller;

    private UUID adminId;
    private UUID nonAdminId;
    private User admin;
    private User nonAdmin;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        compensationRepository = mock(JobCancellationCompensationRepository.class);
        audit = mock(AuditService.class);
        AdminGuard guard = new AdminGuard(userRepository);

        controller = new AdminController(
                guard, audit, mock(JdbcTemplate.class), userRepository,
                mock(HelperApplicationRepository.class), mock(MechanicProfileRepository.class),
                mock(JobRepository.class), mock(JobHistoryService.class), mock(in.raahi.backend.service.CommissionService.class),
                compensationRepository
        );

        adminId = UUID.randomUUID();
        admin = new User();
        admin.setId(adminId);
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.Status.ACTIVE);

        nonAdminId = UUID.randomUUID();
        nonAdmin = new User();
        nonAdmin.setId(nonAdminId);
        nonAdmin.setRole(User.Role.DRIVER);
        nonAdmin.setStatus(User.Status.ACTIVE);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(nonAdminId)).thenReturn(Optional.of(nonAdmin));
    }

    private JobCancellationCompensation createPendingClaim(UUID claimId, double estimatedAmount) {
        JobCancellationCompensation comp = new JobCancellationCompensation();
        comp.setId(claimId);
        Job job = new Job();
        job.setId(UUID.randomUUID());
        comp.setJob(job);

        User helper = new User();
        helper.setId(UUID.randomUUID());
        comp.setHelper(helper);

        User requester = new User();
        requester.setId(UUID.randomUUID());
        comp.setRequester(requester);

        comp.setStatus(CompensationStatus.ELIGIBLE_PENDING_REVIEW);
        comp.setEstimatedLossAmount(estimatedAmount);
        comp.setPayoutStatus(PayoutStatus.UNPAID);
        comp.setPolicyVersion("2026.10-admin-v1");

        when(compensationRepository.findByIdWithDetails(claimId)).thenReturn(Optional.of(comp));
        when(compensationRepository.findById(claimId)).thenReturn(Optional.of(comp));
        return comp;
    }

    @Test
    void nonAdminCannotApproveOrRejectClaims() {
        UUID claimId = UUID.randomUUID();
        createPendingClaim(claimId, 120.0);

        ApiException exApprove = assertThrows(ApiException.class, () ->
                controller.approveCompensation(new AuthenticatedUser(nonAdminId, "DRIVER"), claimId, new ApproveCompensationRequest()));
        assertEquals(HttpStatus.FORBIDDEN, exApprove.status);

        ApiException exReject = assertThrows(ApiException.class, () -> {
            RejectCompensationRequest req = new RejectCompensationRequest();
            req.note = "Test";
            controller.rejectCompensation(new AuthenticatedUser(nonAdminId, "DRIVER"), claimId, req);
        });
        assertEquals(HttpStatus.FORBIDDEN, exReject.status);
    }

    @Test
    void adminApproveTransitionsPendingClaimToAdminApproved_AndDoesNotAlterPayoutStatus() {
        UUID claimId = UUID.randomUUID();
        JobCancellationCompensation comp = createPendingClaim(claimId, 120.0);

        when(compensationRepository.transitionPendingStatus(eq(claimId), eq(CompensationStatus.ADMIN_APPROVED),
                eq(120.0), eq(admin), any(Instant.class), anyString(), any(Instant.class))).thenReturn(1);

        ApproveCompensationRequest req = new ApproveCompensationRequest();
        req.approvedAmount = 120.0;
        req.note = "Verified with dispatch logs";

        ApiResponse<Object> resp = controller.approveCompensation(new AuthenticatedUser(adminId, "ADMIN"), claimId, req);

        assertTrue(resp.success);
        Map<?, ?> data = (Map<?, ?>) resp.data;
        assertEquals(120.0, data.get("approvedAmount"));

        // Audit log verified
        verify(audit).log(eq(adminId), eq("ADMIN"), eq("COMPENSATION_ADMIN_APPROVED"), eq("CANCELLATION_COMPENSATION"),
                eq(claimId.toString()), contains("Approved ₹120.0"));

        // Wallet payout status remains strictly UNPAID until verified ledger integration
        assertEquals(PayoutStatus.UNPAID, comp.getPayoutStatus());
    }

    @Test
    void adminApprovalIsIdempotentOnRetry() {
        UUID claimId = UUID.randomUUID();
        JobCancellationCompensation comp = createPendingClaim(claimId, 120.0);
        comp.setStatus(CompensationStatus.ADMIN_APPROVED);
        comp.setApprovedAmount(120.0);

        ApproveCompensationRequest req = new ApproveCompensationRequest();
        req.approvedAmount = 120.0;

        ApiResponse<Object> resp = controller.approveCompensation(new AuthenticatedUser(adminId, "ADMIN"), claimId, req);

        assertTrue(resp.success);
        Map<?, ?> data = (Map<?, ?>) resp.data;
        assertEquals(true, data.get("alreadyApproved"));
        verify(compensationRepository, never()).transitionPendingStatus(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectedClaimCannotBeApproved() {
        UUID claimId = UUID.randomUUID();
        JobCancellationCompensation comp = createPendingClaim(claimId, 120.0);
        comp.setStatus(CompensationStatus.REJECTED);

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.approveCompensation(new AuthenticatedUser(adminId, "ADMIN"), claimId, new ApproveCompensationRequest()));

        assertEquals(HttpStatus.CONFLICT, ex.status);
        assertEquals("ALREADY_REJECTED", ex.code);
    }

    @Test
    void adminRejectTransitionsClaimToRejectedWithNote() {
        UUID claimId = UUID.randomUUID();
        createPendingClaim(claimId, 120.0);

        when(compensationRepository.transitionPendingStatus(eq(claimId), eq(CompensationStatus.REJECTED),
                eq(0.0), eq(admin), any(Instant.class), eq("Helper did not travel"), any(Instant.class))).thenReturn(1);

        RejectCompensationRequest req = new RejectCompensationRequest();
        req.note = "Helper did not travel";

        ApiResponse<Object> resp = controller.rejectCompensation(new AuthenticatedUser(adminId, "ADMIN"), claimId, req);

        assertTrue(resp.success);
        verify(audit).log(eq(adminId), eq("ADMIN"), eq("COMPENSATION_ADMIN_REJECTED"), eq("CANCELLATION_COMPENSATION"),
                eq(claimId.toString()), contains("Helper did not travel"));
    }

    @Test
    void approvedClaimCannotBeRejected() {
        UUID claimId = UUID.randomUUID();
        JobCancellationCompensation comp = createPendingClaim(claimId, 120.0);
        comp.setStatus(CompensationStatus.ADMIN_APPROVED);

        RejectCompensationRequest req = new RejectCompensationRequest();
        req.note = "Try to reject";

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.rejectCompensation(new AuthenticatedUser(adminId, "ADMIN"), claimId, req));

        assertEquals(HttpStatus.CONFLICT, ex.status);
        assertEquals("ALREADY_APPROVED", ex.code);
    }

    @Test
    void concurrentConflictInApprovalThrowsConflict() {
        UUID claimId = UUID.randomUUID();
        JobCancellationCompensation comp = createPendingClaim(claimId, 120.0);

        // Atomic transition returned 0 because another transaction changed the status
        when(compensationRepository.transitionPendingStatus(any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        // Now database shows it was already rejected by concurrent admin
        JobCancellationCompensation concurrentComp = new JobCancellationCompensation();
        concurrentComp.setId(claimId);
        concurrentComp.setStatus(CompensationStatus.REJECTED);
        when(compensationRepository.findById(claimId)).thenReturn(Optional.of(concurrentComp));

        ApproveCompensationRequest req = new ApproveCompensationRequest();
        req.approvedAmount = 120.0;

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.approveCompensation(new AuthenticatedUser(adminId, "ADMIN"), claimId, req));

        assertEquals(HttpStatus.CONFLICT, ex.status);
        assertEquals("CONCURRENT_DECISION", ex.code);
    }
}
