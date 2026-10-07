package in.raahi.backend.service;

import in.raahi.backend.entity.Job;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable job status history: who moved the job, when, and with what verification/location. */
@Service
public class JobHistoryService {

    private final JdbcTemplate jdbc;

    public JobHistoryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(UUID jobId, String from, String to, UUID actorId, String actorRole,
                       String verificationMethod, Double lat, Double lng, String note) {
        jdbc.update("INSERT INTO job_status_history(job_id, from_status, to_status, actor_id, actor_role, verification_method, lat, lng, note) VALUES (?,?,?,?,?,?,?,?,?)",
                jobId, from, to, actorId, actorRole, verificationMethod, lat, lng, note);
    }

    public void record(Job job, Job.Status from, Job.Status to, UUID actorId, String actorRole,
                       String verificationMethod, Double lat, Double lng, String note) {
        record(job.getId(), from == null ? null : from.name(), to.name(), actorId, actorRole, verificationMethod, lat, lng, note);
    }

    public List<Map<String, Object>> list(UUID jobId) {
        return jdbc.queryForList("SELECT from_status, to_status, actor_id, actor_role, verification_method, lat, lng, note, created_at FROM job_status_history WHERE job_id = ? ORDER BY created_at ASC", jobId);
    }
}
