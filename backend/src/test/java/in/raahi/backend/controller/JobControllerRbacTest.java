package in.raahi.backend.controller;

import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.notification.NotificationService;
import in.raahi.backend.repository.HelperApplicationRepository;
import in.raahi.backend.repository.JobRepository;
import in.raahi.backend.repository.MechanicProfileRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import in.raahi.backend.service.CancellationPolicyService;
import in.raahi.backend.service.CommissionService;
import in.raahi.backend.service.HelperEligibilityService;
import in.raahi.backend.service.JobHistoryService;
import in.raahi.backend.service.SubscriptionPolicy;
import in.raahi.backend.websocket.WebSocketSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Source-level RBAC tests. Not yet executed: needs a real Maven build. */
class JobControllerRbacTest {

    private JobRepository jobRepository;
    private UserRepository userRepository;
    private MechanicProfileRepository profileRepository;
    private CommissionService commissionService;
    private JobController controller;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        userRepository = mock(UserRepository.class);
        profileRepository = mock(MechanicProfileRepository.class);
        commissionService = mock(CommissionService.class);
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.allow(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        CancellationPolicyService cancellation = mock(CancellationPolicyService.class);
        HelperEligibilityService eligibility = new HelperEligibilityService(userRepository, profileRepository, commissionService, cancellation);
        controller = new JobController(jobRepository, userRepository, mock(SubscriptionPolicy.class), eligibility,
                commissionService, cancellation, mock(JobHistoryService.class), mock(HelperApplicationRepository.class),
                mock(JdbcTemplate.class), mock(WebSocketSessionRegistry.class), mock(NotificationService.class),
                rateLimiter, 200.0);
        userId = UUID.randomUUID();
    }

    private User user(User.Role role, User.Status status) {
        User u = new User();
        u.setId(userId);
        u.setRole(role);
        u.setStatus(status);
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        return u;
    }

    private void assertAcceptForbidden(String expectedCode) {
        ApiException e = assertThrows(ApiException.class,
                () -> controller.accept(new AuthenticatedUser(userId, "HELPER"), UUID.randomUUID()));
        assertEquals(HttpStatus.FORBIDDEN, e.status);
        assertEquals(expectedCode, e.code);
        verify(jobRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void driverCannotAcceptJob() {
        user(User.Role.DRIVER, User.Status.ACTIVE);
        assertAcceptForbidden("FORBIDDEN");
    }

    @Test
    void forgedJwtRoleDoesNotHelp() {
        user(User.Role.DRIVER, User.Status.ACTIVE);
        assertAcceptForbidden("FORBIDDEN");
    }

    @Test
    void suspendedHelperCannotAccept() {
        user(User.Role.HELPER, User.Status.SUSPENDED);
        assertAcceptForbidden("ACCOUNT_NOT_ACTIVE");
    }

    @Test
    void helperWithoutApprovedProfileCannotAccept() {
        user(User.Role.HELPER, User.Status.ACTIVE);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertAcceptForbidden("HELPER_NOT_APPROVED");
    }

    @Test
    void helperWithCommissionDueCannotAccept() {
        user(User.Role.HELPER, User.Status.ACTIVE);
        in.raahi.backend.entity.MechanicProfile p = new in.raahi.backend.entity.MechanicProfile();
        p.setVerificationStatus(in.raahi.backend.entity.MechanicProfile.VerificationStatus.APPROVED);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(p));
        doThrow(ApiException.forbidden("COMMISSION_DUE", "due")).when(commissionService).requireNoDue(userId);
        assertAcceptForbidden("COMMISSION_DUE");
    }

    @Test
    void driverCannotListOpenJobs() {
        user(User.Role.DRIVER, User.Status.ACTIVE);
        ApiException e = assertThrows(ApiException.class,
                () -> controller.available(new AuthenticatedUser(userId, "DRIVER"), null, null));
        assertEquals(HttpStatus.FORBIDDEN, e.status);
    }
}
