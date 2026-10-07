package in.raahi.backend.service;

import in.raahi.backend.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Append-only audit trail. The table has a DB trigger that rejects UPDATE/DELETE. */
@Service
public class AuditService {

    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(UUID actorId, String actorRole, String action, String targetType, String targetId, String detail) {
        jdbc.update("INSERT INTO audit_logs(actor_id, actor_role, action, target_type, target_id, detail) VALUES (?,?,?,?,?,?)",
                actorId, actorRole == null ? "SYSTEM" : actorRole, action, targetType, targetId,
                detail == null ? null : (detail.length() > 2000 ? detail.substring(0, 2000) : detail));
    }

    public void log(AuthenticatedUser actor, String actorRole, String action, String targetType, String targetId, String detail) {
        log(actor == null ? null : actor.userId(), actorRole, action, targetType, targetId, detail);
    }
}
