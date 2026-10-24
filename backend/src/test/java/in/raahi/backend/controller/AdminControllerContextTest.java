package in.raahi.backend.controller;

import in.raahi.backend.repository.HelperApplicationRepository;
import in.raahi.backend.repository.JobCancellationCompensationRepository;
import in.raahi.backend.repository.JobRepository;
import in.raahi.backend.repository.MechanicProfileRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.service.AdminGuard;
import in.raahi.backend.service.AuditService;
import in.raahi.backend.service.CommissionService;
import in.raahi.backend.service.JobHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AdminControllerContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(AdminGuard.class, () -> mock(AdminGuard.class))
            .withBean(AuditService.class, () -> mock(AuditService.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(HelperApplicationRepository.class, () -> mock(HelperApplicationRepository.class))
            .withBean(MechanicProfileRepository.class, () -> mock(MechanicProfileRepository.class))
            .withBean(JobRepository.class, () -> mock(JobRepository.class))
            .withBean(JobHistoryService.class, () -> mock(JobHistoryService.class))
            .withBean(CommissionService.class, () -> mock(CommissionService.class))
            .withBean(JobCancellationCompensationRepository.class, () -> mock(JobCancellationCompensationRepository.class))
            .withUserConfiguration(AdminController.class);

    @Test
    @DisplayName("Spring ApplicationContext unambiguously instantiates AdminController with @Autowired constructor")
    void adminControllerIsInstantiatedSuccessfullyBySpringContext() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AdminController.class);
            AdminController controller = context.getBean(AdminController.class);
            assertThat(controller).isNotNull();
        });
    }
}
