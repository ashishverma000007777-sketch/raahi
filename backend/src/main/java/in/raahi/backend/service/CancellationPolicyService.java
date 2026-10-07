package in.raahi.backend.service;

import in.raahi.backend.entity.Job;
import in.raahi.backend.entity.User;
import in.raahi.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Tracks cancellations and applies escalating blocks for abusive ones.
 * "Abusive" = cancelling after a helper accepted but before arrival (MATCHED stage), by either side.
 * Reaching the threshold inside the window:
 *   1st violation -> 1 day block, 2nd -> 7 day block, 3rd+ -> SUSPENDED until an admin reviews.
 * Admins can excuse an individual cancellation (legitimate, support-reviewed) so it stops counting.
 */
@Service
public class CancellationPolicyService {

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final AuditService audit;
    private final int threshold;
    private final int windowDays;

    public CancellationPolicyService(JdbcTemplate jdbc, UserRepository userRepository, AuditService audit,
                                     @Value("${raahi.cancellation.threshold:4}") int threshold,
                                     @Value("${raahi.cancellation.window-days:30}") int windowDays) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.audit = audit;
        this.threshold = Math.max(1, threshold);
        this.windowDays = Math.max(1, windowDays);
    }

    /** Pure decision helper (unit tested): how long to block for the Nth violation, or null = suspend. */
    public static Long blockDaysForViolation(int violationNumber) {
        if (violationNumber <= 1) return 1L;
        if (violationNumber == 2) return 7L;
        return null;
    }

    /** Records the cancellation and returns a short outcome string (NONE / BLOCKED_1D / BLOCKED_7D / SUSPENDED). */
    public String record(User user, Job job, String actorRole, Job.Status stage) {
        boolean abusive = stage == Job.Status.MATCHED;
        jdbc.update("INSERT INTO cancellation_events(user_id, job_id, actor_role, stage, abusive) VALUES (?,?,?,?,?)",
                user.getId(), job.getId(), actorRole, stage.name(), abusive);
        if (!abusive) return "NONE";

        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        if (user.getLastViolationAt() != null && user.getLastViolationAt().isAfter(since)) {
            since = user.getLastViolationAt();
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cancellation_events WHERE user_id = ? AND abusive = TRUE AND excused = FALSE AND created_at > ?",
                Integer.class, user.getId(), java.sql.Timestamp.from(since));
        if (count == null || count < threshold) return "NONE";

        int violation = user.getCancellationViolations() + 1;
        user.setCancellationViolations(violation);
        user.setLastViolationAt(Instant.now());
        Long days = blockDaysForViolation(violation);
        String outcome;
        if (days != null) {
            user.setBlockedUntil(Instant.now().plus(days, ChronoUnit.DAYS));
            user.setBlockReason("Repeated cancellations after a helper accepted");
            outcome = days == 1 ? "BLOCKED_1D" : "BLOCKED_7D";
        } else {
            user.setStatus(User.Status.SUSPENDED);
            user.setBlockReason("Repeated cancellation abuse - under admin review");
            outcome = "SUSPENDED";
        }
        userRepository.save(user);
        audit.log((UUID) null, "SYSTEM", "AUTO_" + outcome, "USER", user.getId().toString(),
                "cancellation violation #" + violation + " (" + count + " abusive cancellations)");
        return outcome;
    }

    public void requireNotBlocked(User user) {
        if (user.isTemporarilyBlocked()) {
            throw in.raahi.backend.exception.ApiException.forbidden("ACCOUNT_BLOCKED",
                    "Your account is temporarily blocked until " + user.getBlockedUntil() + " because of repeated cancellations. Contact support if this is a mistake.");
        }
    }
}
