package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.JobDtos.*;
import in.raahi.backend.entity.Job;
import in.raahi.backend.entity.JobCancellationCompensation;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.notification.NotificationService;
import in.raahi.backend.repository.HelperApplicationRepository;
import in.raahi.backend.repository.JobCancellationCompensationRepository;
import in.raahi.backend.repository.JobRepository;
import in.raahi.backend.repository.MechanicProfileRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import in.raahi.backend.service.AuditService;
import in.raahi.backend.service.CancellationPolicyService;
import in.raahi.backend.service.CancellationProtectionPolicyService;
import in.raahi.backend.service.CommissionService;
import in.raahi.backend.service.HelperEligibilityService;
import in.raahi.backend.service.JobHistoryService;
import in.raahi.backend.service.SubscriptionPolicy;
import in.raahi.backend.websocket.WebSocketSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobControllerCancellationProtectionTest {

    private JobRepository jobRepository;
    private UserRepository userRepository;
    private JobCancellationCompensationRepository compensationRepository;
    private CancellationPolicyService cancellationPolicy;
    private CancellationProtectionPolicyService protectionPolicyService;
    private AuditService audit;
    private JobController controller;

    private UUID customerId;
    private UUID helperId;
    private UUID jobId;
    private User customer;
    private User helper;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        userRepository = mock(UserRepository.class);
        compensationRepository = mock(JobCancellationCompensationRepository.class);
        cancellationPolicy = mock(CancellationPolicyService.class);
        audit = mock(AuditService.class);
        protectionPolicyService = new CancellationProtectionPolicyService(true, 12.0, 50.0, 0.5, 0.5, 500.0, 3);

        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.allow(anyString(), anyInt(), any(Duration.class))).thenReturn(true);

        controller = new JobController(
                jobRepository, userRepository, mock(SubscriptionPolicy.class),
                mock(HelperEligibilityService.class), mock(CommissionService.class), cancellationPolicy,
                mock(JobHistoryService.class), mock(HelperApplicationRepository.class),
                mock(JdbcTemplate.class), mock(WebSocketSessionRegistry.class), mock(NotificationService.class),
                rateLimiter, mock(MechanicProfileRepository.class), compensationRepository,
                protectionPolicyService, audit, 200.0
        );

        customerId = UUID.randomUUID();
        helperId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        customer = new User();
        customer.setId(customerId);
        customer.setName("Alice Requester");
        customer.setRole(User.Role.DRIVER);
        customer.setStatus(User.Status.ACTIVE);

        helper = new User();
        helper.setId(helperId);
        helper.setName("Bob Helper");
        helper.setRole(User.Role.HELPER);
        helper.setStatus(User.Status.ACTIVE);
        helper.setLastLat(28.6100);
        helper.setLastLng(77.2000);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.findById(helperId)).thenReturn(Optional.of(helper));
        when(cancellationPolicy.record(any(), any(), any(), any(), any(), anyBoolean())).thenReturn("NONE");
    }

    private Job createJob(Job.Status status) {
        Job job = new Job();
        job.setId(jobId);
        job.setRequester(customer);
        job.setHelper(helper);
        job.setStatus(status);
        job.setReqLat(28.6500);
        job.setReqLng(77.2000);
        job.setEnRouteLat(28.6100);
        job.setEnRouteLng(77.2000);
        job.setEnRouteAt(Instant.now().minusSeconds(120));
        when(jobRepository.findByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.saveAndFlush(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(compensationRepository.findByJobId(jobId)).thenReturn(Optional.empty());
        when(compensationRepository.saveAndFlush(any(JobCancellationCompensation.class))).thenAnswer(i -> {
            JobCancellationCompensation c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
        return job;
    }

    @Test
    void customerCancelsBeforeHelperDispatch_NoCompensationGenerated() {
        Job job = createJob(Job.Status.MATCHED);

        CancelRequest req = new CancelRequest();
        req.reason = "Change of plans";
        req.reasonCode = "CUSTOMER_CANCELLED";

        ApiResponse<Object> resp = controller.cancel(new AuthenticatedUser(customerId, "DRIVER"), jobId, req);

        assertTrue(resp.success);
        assertEquals(Job.Status.CANCELLED, job.getStatus());
        assertEquals("CUSTOMER_CANCELLED", job.getCancellationReasonCode());
        verify(compensationRepository, never()).saveAndFlush(any());
    }

    @Test
    void customerCancelsAfterVerifiedEnRoute_ClaimQueuedForAdminReview_NoAutoApproval() {
        Job job = createJob(Job.Status.EN_ROUTE);

        CancelRequest req = new CancelRequest();
        req.reason = "Found alternate help";
        req.reasonCode = "CUSTOMER_CANCELLED";
        // Helper travelled ~2 km (from 28.61 to 28.628)
        req.lat = 28.6280;
        req.lng = 77.2000;

        ApiResponse<Object> resp = controller.cancel(new AuthenticatedUser(customerId, "DRIVER"), jobId, req);

        assertTrue(resp.success);
        assertEquals(Job.Status.CANCELLED, job.getStatus());
        assertEquals("CUSTOMER_CANCELLED", job.getCancellationReasonCode());

        ArgumentCaptor<JobCancellationCompensation> captor = ArgumentCaptor.forClass(JobCancellationCompensation.class);
        verify(compensationRepository).saveAndFlush(captor.capture());

        JobCancellationCompensation comp = captor.getValue();
        // Confirmed Business Policy: ALL claims require admin review
        assertEquals(JobCancellationCompensation.CompensationStatus.ELIGIBLE_PENDING_REVIEW, comp.getStatus());
        assertNull(comp.getApprovedAmount(), "Approved amount must remain null until admin approval");
        assertEquals(86.0, comp.getEstimatedLossAmount());
        assertEquals(2.0, comp.getActualTravelKm());
        assertEquals(1.0, comp.getReturnDistanceKm());
        assertEquals("GPS_VERIFIED", comp.getEvidenceType());
        // Settlement is explicitly separated: UNPAID
        assertEquals(JobCancellationCompensation.PayoutStatus.UNPAID, comp.getPayoutStatus());
        assertEquals("2026.10-admin-v1", comp.getPolicyVersion());
        assertEquals(50.0, comp.getBaseCalloutFee());
        assertEquals(12.0, comp.getBaseRatePerKm());
        assertEquals(0.5, comp.getReturnFactor());
    }

    @Test
    void customerCancelsEnRouteWithoutGpsEvidence_MarkedIneligibleWithPersistedReason() {
        Job job = createJob(Job.Status.EN_ROUTE);
        job.setEnRouteLat(null);
        job.setEnRouteLng(null);
        helper.setLastLat(null);
        helper.setLastLng(null);

        CancelRequest req = new CancelRequest();
        req.reasonCode = "CUSTOMER_CANCELLED";

        ApiResponse<Object> resp = controller.cancel(new AuthenticatedUser(customerId, "DRIVER"), jobId, req);

        assertTrue(resp.success);
        assertEquals(Job.Status.CANCELLED, job.getStatus());

        ArgumentCaptor<JobCancellationCompensation> captor = ArgumentCaptor.forClass(JobCancellationCompensation.class);
        verify(compensationRepository).saveAndFlush(captor.capture());

        JobCancellationCompensation comp = captor.getValue();
        assertEquals(JobCancellationCompensation.CompensationStatus.NOT_ELIGIBLE, comp.getStatus());
        assertEquals(0.0, comp.getApprovedAmount());
        assertEquals("UNVERIFIED", comp.getEvidenceType());
        assertTrue(comp.getEligibilityNotes().contains("No reliable GPS"));
    }

    @Test
    void helperCancelsForGenuineEmergency_ExemptFromPenaltyAndReopensRequest() {
        Job job = createJob(Job.Status.EN_ROUTE);

        CancelRequest req = new CancelRequest();
        req.reason = "Flat tire on my bike";
        req.reasonCode = "VEHICLE_EMERGENCY";

        ApiResponse<Object> resp = controller.cancel(new AuthenticatedUser(helperId, "HELPER"), jobId, req);

        assertTrue(resp.success);
        // Customer request re-opened to PENDING so another helper can accept
        assertEquals(Job.Status.PENDING, job.getStatus());
        assertNull(job.getHelper());

        // Verify cancellationPolicy was called with isEmergency = true
        verify(cancellationPolicy).record(eq(helper), eq(job), eq("HELPER"), eq(Job.Status.EN_ROUTE), eq("VEHICLE_EMERGENCY"), eq(true));
        // Helper is not paid compensation for cancelling their own job
        verify(compensationRepository, never()).saveAndFlush(any());
    }

    @Test
    void helperCancelsWithoutAcceptableReason_RecordedAsAbusiveAndReopensRequest() {
        Job job = createJob(Job.Status.EN_ROUTE);

        CancelRequest req = new CancelRequest();
        req.reason = "Do not want to go";
        req.reasonCode = "HELPER_CANCELLED";

        ApiResponse<Object> resp = controller.cancel(new AuthenticatedUser(helperId, "HELPER"), jobId, req);

        assertTrue(resp.success);
        assertEquals(Job.Status.PENDING, job.getStatus());
        assertNull(job.getHelper());

        verify(cancellationPolicy).record(eq(helper), eq(job), eq("HELPER"), eq(Job.Status.EN_ROUTE), eq("HELPER_CANCELLED"), eq(false));
    }

    @Test
    void customerUnreachable_JobCancelledAndCompensationEligibleForAdminReview() {
        Job job = createJob(Job.Status.EN_ROUTE);

        CancelRequest req = new CancelRequest();
        req.reason = "Called 5 times, phone switched off";
        req.reasonCode = "CUSTOMER_UNREACHABLE";
        req.lat = 28.6280;
        req.lng = 77.2000;

        ApiResponse<Object> resp = controller.cancel(new AuthenticatedUser(helperId, "HELPER"), jobId, req);

        assertTrue(resp.success);
        // Customer unreachable cancels the job rather than re-opening it
        assertEquals(Job.Status.CANCELLED, job.getStatus());
        assertEquals("CUSTOMER_UNREACHABLE", job.getCancellationReasonCode());

        // Travel loss compensation claim created for helper pending admin review
        ArgumentCaptor<JobCancellationCompensation> captor = ArgumentCaptor.forClass(JobCancellationCompensation.class);
        verify(compensationRepository).saveAndFlush(captor.capture());
        assertEquals(JobCancellationCompensation.CompensationStatus.ELIGIBLE_PENDING_REVIEW, captor.getValue().getStatus());
    }

    @Test
    void invalidTransition_NormalCancellationAfterArrivedThrowsConflict() {
        createJob(Job.Status.ARRIVED);

        CancelRequest req = new CancelRequest();
        req.reasonCode = "CUSTOMER_CANCELLED";

        ApiException e = assertThrows(ApiException.class,
                () -> controller.cancel(new AuthenticatedUser(customerId, "DRIVER"), jobId, req));
        assertEquals(HttpStatus.CONFLICT, e.status);
        assertEquals("CANCEL_NOT_ALLOWED", e.code);
    }

    @Test
    void unauthorizedUserCannotCancelJob() {
        createJob(Job.Status.EN_ROUTE);

        UUID strangerId = UUID.randomUUID();
        User stranger = new User();
        stranger.setId(strangerId);
        when(userRepository.findById(strangerId)).thenReturn(Optional.of(stranger));

        ApiException e = assertThrows(ApiException.class,
                () -> controller.cancel(new AuthenticatedUser(strangerId, "DRIVER"), jobId, new CancelRequest()));
        assertEquals(HttpStatus.FORBIDDEN, e.status);
    }

    @Test
    void duplicateCompensationClaimPrevented_IdempotentRetries() {
        Job job = createJob(Job.Status.EN_ROUTE);

        // Simulate that compensation already exists in repository
        JobCancellationCompensation existing = new JobCancellationCompensation();
        existing.setId(UUID.randomUUID());
        existing.setStatus(JobCancellationCompensation.CompensationStatus.ELIGIBLE_PENDING_REVIEW);
        when(compensationRepository.findByJobId(jobId)).thenReturn(Optional.of(existing));

        CancelRequest req = new CancelRequest();
        req.reasonCode = "CUSTOMER_CANCELLED";

        ApiResponse<Object> resp = controller.cancel(new AuthenticatedUser(customerId, "DRIVER"), jobId, req);
        assertTrue(resp.success);

        // Second compensation row was NOT inserted
        verify(compensationRepository, never()).saveAndFlush(any());
    }
}
