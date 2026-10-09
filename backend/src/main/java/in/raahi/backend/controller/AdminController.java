package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.HelperDtos.*;
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
import in.raahi.backend.service.AdminGuard;
import in.raahi.backend.service.AuditService;
import in.raahi.backend.service.CommissionService;
import in.raahi.backend.service.JobHistoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Collectors;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Admin API. Every method re-checks ADMIN against the database (AdminGuard) and every mutation
 * writes an immutable audit_logs row. Helper KYC approve/reject lives in HelperController
 * (/admin/helper-applications/...) and is audited there.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminGuard guard;
    private final AuditService audit;
    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final HelperApplicationRepository applicationRepository;
    private final MechanicProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final JobHistoryService history;
    private final CommissionService commission;

    public AdminController(AdminGuard guard, AuditService audit, JdbcTemplate jdbc, UserRepository userRepository,
                           HelperApplicationRepository applicationRepository, MechanicProfileRepository profileRepository,
                           JobRepository jobRepository, JobHistoryService history, CommissionService commission) {
        this.guard = guard;
        this.audit = audit;
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.history = history;
        this.commission = commission;
    }

    // ---------------------------------------------------------------- overview
    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> overview(@AuthenticationPrincipal AuthenticatedUser p) {
        guard.require(p);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pendingKyc", count("SELECT COUNT(*) FROM helper_applications WHERE status = 'PENDING'"));
        m.put("approvedHelpers", count("SELECT COUNT(*) FROM helper_applications WHERE status = 'APPROVED'"));
        m.put("suspendedHelpers", count("SELECT COUNT(*) FROM helper_applications WHERE status = 'SUSPENDED'"));
        m.put("activeJobs", count("SELECT COUNT(*) FROM jobs WHERE status IN ('MATCHED','ARRIVED','IN_PROGRESS','WORK_DONE')"));
        m.put("openReports", count("SELECT COUNT(*) FROM job_reports WHERE status = 'OPEN'"));
        m.put("abusiveCancellations30d", count("SELECT COUNT(*) FROM cancellation_events WHERE abusive = TRUE AND excused = FALSE AND created_at > NOW() - INTERVAL '30 days'"));
        m.put("helpersOwingCommission", count("SELECT COUNT(*) FROM (SELECT helper_id FROM commission_ledger GROUP BY helper_id HAVING SUM(amount) < 0) t"));
        return ApiResponse.ok(m);
    }

    // ---------------------------------------------------------------- users / blocks
    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> users(@AuthenticationPrincipal AuthenticatedUser p,
                                                         @RequestParam(required = false) String role,
                                                         @RequestParam(required = false) String q) {
        guard.require(p);
        String like = q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase() + "%";
        String r = role == null || role.isBlank() ? "%" : role.trim().toUpperCase();
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT id, name, phone, role, status, blocked_until, block_reason, cancellation_violations, rating_avg, total_helps, created_at FROM users WHERE role LIKE ? AND (LOWER(COALESCE(name,'')) LIKE ? OR COALESCE(phone,'') LIKE ?) ORDER BY created_at DESC LIMIT 100",
                r, like, like));
    }

    @PostMapping("/users/{id}/block")
    @Transactional
    public ApiResponse<Object> block(@AuthenticationPrincipal AuthenticatedUser p, @PathVariable UUID id,
                                     @Valid @RequestBody BlockRequest req) {
        User admin = guard.require(p);
        User target = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        if (target.getRole() == User.Role.ADMIN) {
            throw ApiException.forbidden("FORBIDDEN", "Admin accounts cannot be blocked here");
        }
        if (req.days == null) {
            target.setStatus(User.Status.SUSPENDED);
        } else {
            target.setBlockedUntil(Instant.now().plus(req.days, ChronoUnit.DAYS));
        }
        target.setBlockReason(req.reason);
        userRepository.save(target);
        audit.log(admin.getId(), "ADMIN", req.days == null ? "USER_SUSPENDED" : "USER_BLOCKED", "USER", id.toString(),
                req.reason + (req.days == null ? "" : " (" + req.days + " days)"));
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/users/{id}/unblock")
    @Transactional
    public ApiResponse<Object> unblock(@AuthenticationPrincipal AuthenticatedUser p, @PathVariable UUID id,
                                       @Valid @RequestBody ReasonRequest req) {
        User admin = guard.require(p);
        User target = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        target.setStatus(User.Status.ACTIVE);
        target.setBlockedUntil(null);
        target.setBlockReason(null);
        userRepository.save(target);
        audit.log(admin.getId(), "ADMIN", "USER_UNBLOCKED", "USER", id.toString(), req.reason);
        return ApiResponse.ok(Map.of("success", true));
    }

    // ---------------------------------------------------------------- helpers
    @GetMapping("/helpers")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> helpers(@AuthenticationPrincipal AuthenticatedUser p,
                                                          @RequestParam(required = false) String status) {
        guard.require(p);
        String st = status == null || status.isBlank() ? "%" : status.trim().toUpperCase();
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT a.id AS application_id, a.user_id, u.name, u.phone, a.status, a.service_area, a.services, a.submitted_at, a.reviewed_at, a.suspension_reason FROM helper_applications a JOIN users u ON u.id = a.user_id WHERE a.status LIKE ? ORDER BY a.submitted_at DESC LIMIT 100", st));
    }

    @PostMapping("/helpers/{userId}/suspend")
    @Transactional
    public ApiResponse<Object> suspendHelper(@AuthenticationPrincipal AuthenticatedUser p, @PathVariable UUID userId,
                                             @Valid @RequestBody ReasonRequest req) {
        User admin = guard.require(p);
        HelperApplication app = applicationRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("NO_APPLICATION", "Helper application not found"));
        app.setStatus(HelperApplication.Status.SUSPENDED);
        app.setSuspensionReason(req.reason);
        applicationRepository.save(app);
        profileRepository.findByUserId(userId).ifPresent(prof -> {
            prof.setVerificationStatus(MechanicProfile.VerificationStatus.SUSPENDED);
            prof.setAvailable(false);
            profileRepository.save(prof);
        });
        audit.log(admin.getId(), "ADMIN", "HELPER_SUSPENDED", "USER", userId.toString(), req.reason);
        return ApiResponse.ok(Map.of("success", true));
    }

    @PostMapping("/helpers/{userId}/reinstate")
    @Transactional
    public ApiResponse<Object> reinstateHelper(@AuthenticationPrincipal AuthenticatedUser p, @PathVariable UUID userId,
                                               @Valid @RequestBody ReasonRequest req) {
        User admin = guard.require(p);
        HelperApplication app = applicationRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("NO_APPLICATION", "Helper application not found"));
        if (app.getStatus() != HelperApplication.Status.SUSPENDED) {
            throw ApiException.conflict("NOT_SUSPENDED", "Helper is not suspended");
        }
        app.setStatus(HelperApplication.Status.APPROVED);
        app.setSuspensionReason(null);
        applicationRepository.save(app);
        profileRepository.findByUserId(userId).ifPresent(prof -> {
            prof.setVerificationStatus(MechanicProfile.VerificationStatus.APPROVED);
            profileRepository.save(prof);
        });
        audit.log(admin.getId(), "ADMIN", "HELPER_REINSTATED", "USER", userId.toString(), req.reason);
        return ApiResponse.ok(Map.of("success", true));
    }

    // ---------------------------------------------------------------- jobs
    @GetMapping("/jobs")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> jobs(@AuthenticationPrincipal AuthenticatedUser p,
                                                       @RequestParam(required = false) String status) {
        guard.require(p);
        Job.Status st = null;
        if (status != null && !status.isBlank()) {
            try { st = Job.Status.valueOf(status.trim().toUpperCase()); }
            catch (IllegalArgumentException e) { throw ApiException.badRequest("INVALID_STATUS", "Unknown job status"); }
        }
        List<Map<String, Object>> out = jobRepository.findAllForAdmin(st, PageRequest.of(0, 100)).stream().map(j -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.getId().toString());
            m.put("status", j.getStatus().name());
            m.put("problemType", j.getProblemType());
            m.put("requester", j.getRequester().getName());
            m.put("helper", j.getHelper() == null ? null : j.getHelper().getName());
            m.put("amount", j.getFinalAmount() != null ? j.getFinalAmount() : j.getRewardAmount());
            m.put("rated", j.isRated());
            m.put("createdAt", j.getCreatedAt() == null ? null : j.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(out);
    }

    @GetMapping("/jobs/{id}/history")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> jobHistory(@AuthenticationPrincipal AuthenticatedUser p, @PathVariable UUID id) {
        guard.require(p);
        return ApiResponse.ok(history.list(id));
    }

    // ---------------------------------------------------------------- complaints / disputes
    @GetMapping("/reports")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> reports(@AuthenticationPrincipal AuthenticatedUser p,
                                                          @RequestParam(required = false, defaultValue = "OPEN") String status) {
        guard.require(p);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT r.id, r.job_id, r.reporter_id, u.name AS reporter_name, r.kind, r.message, r.status, r.resolution, r.created_at FROM job_reports r JOIN users u ON u.id = r.reporter_id WHERE r.status = ? ORDER BY r.created_at DESC LIMIT 100",
                status.toUpperCase()));
    }

    @PostMapping("/reports/{id}/resolve")
    @Transactional
    public ApiResponse<Object> resolveReport(@AuthenticationPrincipal AuthenticatedUser p, @PathVariable UUID id,
                                             @Valid @RequestBody ResolveReportRequest req) {
        User admin = guard.require(p);
        int n = jdbc.update("UPDATE job_reports SET status = 'RESOLVED', resolution = ?, resolved_by = ?, resolved_at = NOW() WHERE id = ? AND status = 'OPEN'",
                req.resolution, admin.getId(), id);
        if (n == 0) throw ApiException.conflict("NOT_OPEN", "Report not found or already resolved");
        audit.log(admin.getId(), "ADMIN", "REPORT_RESOLVED", "JOB_REPORT", id.toString(), req.resolution);
        return ApiResponse.ok(Map.of("success", true));
    }

    // ---------------------------------------------------------------- cancellation abuse
    @GetMapping("/cancellations")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> cancellations(@AuthenticationPrincipal AuthenticatedUser p,
                                                                @RequestParam(required = false, defaultValue = "true") boolean abusiveOnly) {
        guard.require(p);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT c.id, c.user_id, u.name, u.phone, c.job_id, c.actor_role, c.stage, c.abusive, c.excused, c.excuse_note, c.created_at, u.cancellation_violations FROM cancellation_events c JOIN users u ON u.id = c.user_id WHERE (? = FALSE OR c.abusive = TRUE) ORDER BY c.created_at DESC LIMIT 100",
                abusiveOnly));
    }

    /** Legitimate, support-reviewed exception: the cancellation stops counting toward the threshold. */
    @PostMapping("/cancellations/{id}/excuse")
    @Transactional
    public ApiResponse<Object> excuse(@AuthenticationPrincipal AuthenticatedUser p, @PathVariable UUID id,
                                      @Valid @RequestBody ExcuseRequest req) {
        User admin = guard.require(p);
        int n = jdbc.update("UPDATE cancellation_events SET excused = TRUE, excused_by = ?, excuse_note = ? WHERE id = ? AND excused = FALSE",
                admin.getId(), req.note, id);
        if (n == 0) throw ApiException.conflict("NOT_EXCUSABLE", "Cancellation not found or already excused");
        audit.log(admin.getId(), "ADMIN", "CANCELLATION_EXCUSED", "CANCELLATION", id.toString(), req.note);
        return ApiResponse.ok(Map.of("success", true));
    }

    // ---------------------------------------------------------------- ratings / reports
    @GetMapping("/ratings")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> ratings(@AuthenticationPrincipal AuthenticatedUser p,
                                                          @RequestParam(required = false, defaultValue = "5") int maxStars) {
        guard.require(p);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT r.id, r.job_id, r.helper_id, h.name AS helper_name, r.stars, r.comment, r.created_at FROM job_ratings r JOIN users h ON h.id = r.helper_id WHERE r.stars <= ? ORDER BY r.created_at DESC LIMIT 100",
                Math.min(5, Math.max(1, maxStars))));
    }

    // ---------------------------------------------------------------- commission / payments
    @GetMapping("/commission/balances")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> balances(@AuthenticationPrincipal AuthenticatedUser p) {
        guard.require(p);
        return ApiResponse.ok(commission.balances(200));
    }

    @GetMapping("/commission/ledger")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> ledger(@AuthenticationPrincipal AuthenticatedUser p,
                                                         @RequestParam(required = false) UUID helperId) {
        guard.require(p);
        return ApiResponse.ok(commission.allEntries(helperId, 200));
    }

    /** Records that a helper paid Raahi their commission (outside the app). Adds a +amount ledger row. */
    @PostMapping("/commission/settle")
    @Transactional
    public ApiResponse<Object> settle(@AuthenticationPrincipal AuthenticatedUser p, @Valid @RequestBody SettleRequest req) {
        User admin = guard.require(p);
        UUID helperId;
        try { helperId = UUID.fromString(req.helperId); }
        catch (IllegalArgumentException e) { throw ApiException.badRequest("INVALID_HELPER", "Invalid helper id"); }
        User helper = userRepository.findById(helperId).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Helper not found"));
        if (helper.getRole() != User.Role.HELPER && helper.getRole() != User.Role.MECHANIC) {
            throw ApiException.badRequest("NOT_A_HELPER", "User is not a helper");
        }
        commission.settle(helperId, req.amount, req.method, req.reference, admin.getId());
        audit.log(admin.getId(), "ADMIN", "COMMISSION_SETTLED", "USER", helperId.toString(),
                req.amount + " via " + req.method + (req.reference == null ? "" : " ref " + req.reference));
        return ApiResponse.ok(Map.of("success", true, "balance", commission.balance(helperId)));
    }

    // ---------------------------------------------------------------- audit
    @GetMapping("/audit-logs")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> auditLogs(@AuthenticationPrincipal AuthenticatedUser p,
                                                            @RequestParam(required = false, defaultValue = "100") int limit) {
        guard.require(p);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT id, actor_id, actor_role, action, target_type, target_id, detail, created_at FROM audit_logs ORDER BY created_at DESC LIMIT ?",
                Math.min(500, Math.max(1, limit))));
    }


    // OSM mechanic leads: imported as unverified leads, never as active mechanics.
    @GetMapping("/mechanic-leads")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> mechanicLeads(
            @AuthenticationPrincipal AuthenticatedUser p,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q) {
        guard.require(p);
        String st = status == null || status.isBlank() ? "%" : status.trim().toUpperCase();
        String term = q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase() + "%";
        return ApiResponse.ok(jdbc.queryForList("""
            SELECT id, osm_type, osm_id, shop_name, phone, website, address,
                   latitude, longitude, status, osm_url, updated_at
            FROM osm_mechanic_leads
            WHERE status LIKE ?
              AND (LOWER(shop_name) LIKE ? OR LOWER(COALESCE(address,'')) LIKE ?
                   OR COALESCE(phone,'') LIKE ?)
            ORDER BY shop_name LIMIT 500
            """, st, term, term, term));
    }

    @PostMapping("/mechanic-leads/sync")
    @Transactional
    public ApiResponse<Map<String, Object>> syncMechanicLeads(
            @AuthenticationPrincipal AuthenticatedUser p) {
        User admin = guard.require(p);
        String query = "[out:json][timeout:25];("
            + "nwr[\"shop\"=\"car_repair\"](30.55,76.60,30.85,76.95);"
            + "nwr[\"craft\"=\"car_repair\"](30.55,76.60,30.85,76.95);"
            + "nwr[\"shop\"=\"car\"](30.55,76.60,30.85,76.95);"
            + "nwr[\"amenity\"=\"car_repair\"](30.55,76.60,30.85,76.95);"
            + ");out center tags 500;";
        JsonNode response = null;
        Exception lastError = null;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(25000);
        RestTemplate osmClient = new RestTemplate(factory);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.USER_AGENT, "RaahiApp/2.0 (contact@raahi.in)");
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        for (String endpoint : List.of(
                "https://overpass.private.coffee/api/interpreter",
                "https://overpass-api.de/api/interpreter",
                "https://overpass.kumi.systems/api/interpreter")) {
            try {
                response = osmClient.postForObject(
                        endpoint, new HttpEntity<>(body, headers), JsonNode.class);
                if (response != null && response.has("elements")) break;
                response = null;
            } catch (Exception ex) {
                lastError = ex;
                response = null;
            }
        }
        if (response == null || !response.has("elements")) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "OVERPASS_UNAVAILABLE", "OSM provider unavailable; try again later");
        }

        int imported = 0;
        for (JsonNode element : response.path("elements")) {
            JsonNode tags = element.path("tags");
            String name = tags.path("name").asText("").trim();
            if (name.isBlank()) continue;
            String type = element.path("type").asText("");
            long osmId = element.path("id").asLong(0);
            if (osmId <= 0 || !List.of("node", "way", "relation").contains(type)) continue;

            double lat = element.has("lat") ? element.path("lat").asDouble()
                    : element.path("center").path("lat").asDouble();
            double lng = element.has("lon") ? element.path("lon").asDouble()
                    : element.path("center").path("lon").asDouble();
            if (!Double.isFinite(lat) || !Double.isFinite(lng)
                    || lat < 30.4 || lat > 31.0 || lng < 76.4 || lng > 77.1) continue;

            String phone = tags.path("contact:phone").asText(tags.path("phone").asText(""));
            String website = tags.path("contact:website").asText(tags.path("website").asText(""));
            String address = tags.path("addr:full").asText("");
            if (address.isBlank()) {
                address = (tags.path("addr:housenumber").asText("") + " "
                        + tags.path("addr:street").asText("") + " "
                        + tags.path("addr:suburb").asText("") + " "
                        + tags.path("addr:city").asText("")).trim();
            }
            String osmUrl = "https://www.openstreetmap.org/" + type + "/" + osmId;
            jdbc.update("""
                INSERT INTO osm_mechanic_leads
                    (osm_type, osm_id, shop_name, phone, website, address, latitude, longitude, osm_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (osm_type, osm_id) DO UPDATE SET
                    shop_name = EXCLUDED.shop_name,
                    phone = COALESCE(NULLIF(EXCLUDED.phone,''), osm_mechanic_leads.phone),
                    website = COALESCE(NULLIF(EXCLUDED.website,''), osm_mechanic_leads.website),
                    address = COALESCE(NULLIF(EXCLUDED.address,''), osm_mechanic_leads.address),
                    latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude,
                    osm_url = EXCLUDED.osm_url, updated_at = NOW()
                """, type, osmId, name, phone, website, address, lat, lng, osmUrl);
            imported++;
        }
        audit.log(admin.getId(), "ADMIN", "OSM_MECHANIC_LEADS_SYNCED",
                "MECHANIC_LEADS", null, "Processed " + imported + " OSM mechanic leads in Chandigarh Tricity");
        return ApiResponse.ok(Map.of("processed", imported, "source", "OpenStreetMap Overpass",
                "note", "Leads are unverified; no mechanic accounts were created or approved."));
    }

    @PostMapping("/mechanic-leads/{id}/status")
    @Transactional
    public ApiResponse<Map<String, Object>> updateMechanicLeadStatus(
            @AuthenticationPrincipal AuthenticatedUser p, @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        User admin = guard.require(p);
        String status = body == null ? "" : body.getOrDefault("status", "").trim().toUpperCase();
        if (!List.of("NEW", "CONTACTED", "VERIFIED", "REJECTED").contains(status)) {
            throw ApiException.badRequest("INVALID_LEAD_STATUS", "Status must be NEW, CONTACTED, VERIFIED or REJECTED");
        }
        int changed = jdbc.update(
                "UPDATE osm_mechanic_leads SET status = ?, updated_at = NOW() WHERE id = ?",
                status, id);
        if (changed == 0) throw ApiException.notFound("LEAD_NOT_FOUND", "Mechanic lead not found");
        audit.log(admin.getId(), "ADMIN", "OSM_MECHANIC_LEAD_STATUS_UPDATED",
                "MECHANIC_LEAD", id.toString(), status);
        return ApiResponse.ok(Map.of("success", true, "status", status));
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
