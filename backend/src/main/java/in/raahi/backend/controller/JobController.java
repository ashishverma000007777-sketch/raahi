package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.JobDtos.*;
import in.raahi.backend.entity.HelperApplication;
import in.raahi.backend.entity.Job;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.notification.NotificationService;
import in.raahi.backend.repository.HelperApplicationRepository;
import in.raahi.backend.repository.JobRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import in.raahi.backend.service.CancellationPolicyService;
import in.raahi.backend.service.CommissionMath;
import in.raahi.backend.service.CommissionService;
import in.raahi.backend.service.GeoUtil;
import in.raahi.backend.service.HelperEligibilityService;
import in.raahi.backend.service.JobHistoryService;
import in.raahi.backend.service.JobStateMachine;
import in.raahi.backend.service.SubscriptionPolicy;
import in.raahi.backend.websocket.WebSocketSessionRegistry;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Job lifecycle (wire names): PENDING(requested) -> MATCHED(accepted) -> ARRIVED -> IN_PROGRESS
 * -> WORK_DONE -> COMPLETED -> rated (job.rated = true).
 * Every transition goes through JobStateMachine, runs under a row lock, and is written to the
 * immutable job_status_history with actor + timestamp + verification/location data.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private static final EnumSet<Job.Status> ACTIVE_HELPER_STATES =
            EnumSet.of(Job.Status.MATCHED, Job.Status.ARRIVED, Job.Status.IN_PROGRESS, Job.Status.WORK_DONE);

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final SubscriptionPolicy subscriptionPolicy;
    private final HelperEligibilityService eligibility;
    private final CommissionService commissionService;
    private final CancellationPolicyService cancellationPolicy;
    private final JobHistoryService history;
    private final HelperApplicationRepository applicationRepository;
    private final JdbcTemplate jdbc;
    private final WebSocketSessionRegistry sessionRegistry;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;
    private final double arrivalRadiusM;

    public JobController(JobRepository jobRepository, UserRepository userRepository,
                         SubscriptionPolicy subscriptionPolicy, HelperEligibilityService eligibility,
                         CommissionService commissionService, CancellationPolicyService cancellationPolicy,
                         JobHistoryService history, HelperApplicationRepository applicationRepository,
                         JdbcTemplate jdbc, WebSocketSessionRegistry sessionRegistry,
                         NotificationService notificationService, RateLimiter rateLimiter,
                         @Value("${raahi.job.arrival-radius-m:200}") double arrivalRadiusM) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.subscriptionPolicy = subscriptionPolicy;
        this.eligibility = eligibility;
        this.commissionService = commissionService;
        this.cancellationPolicy = cancellationPolicy;
        this.history = history;
        this.applicationRepository = applicationRepository;
        this.jdbc = jdbc;
        this.sessionRegistry = sessionRegistry;
        this.notificationService = notificationService;
        this.rateLimiter = rateLimiter;
        this.arrivalRadiusM = arrivalRadiusM;
    }

    // ------------------------------------------------------------------ create / read

    @PostMapping
    @Transactional
    public ApiResponse<JobDto> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @Valid @RequestBody CreateJobRequest req) {
        limit("jobs:create:" + principal.userId(), 5, Duration.ofMinutes(1), "Too many job requests. Please wait.");

        if (req.lat == null || req.lng == null || req.lat < -90 || req.lat > 90 || req.lng < -180 || req.lng > 180) {
            throw ApiException.badRequest("INVALID_COORDS", "lat and lng out of valid range");
        }
        if (req.rewardAmount != null && (req.rewardAmount < 0 || req.rewardAmount > 50000)) {
            throw ApiException.badRequest("INVALID_REWARD", "Reward amount must be between 0 and 50,000");
        }
        if (req.problemType != null && req.problemType.length() > 50) {
            throw ApiException.badRequest("TYPE_TOO_LONG", "Problem type exceeds 50 characters");
        }
        if (req.problemDesc != null && req.problemDesc.length() > 1000) {
            throw ApiException.badRequest("DESC_TOO_LONG", "Problem description exceeds 1000 characters");
        }
        if (req.highwayName != null && req.highwayName.length() > 100) {
            throw ApiException.badRequest("HIGHWAY_NAME_TOO_LONG", "Highway name exceeds 100 characters");
        }

        User requester = load(principal.userId());
        cancellationPolicy.requireNotBlocked(requester);
        // Subscription gating lives in SubscriptionPolicy (off by default for the MVP; see its javadoc).
        subscriptionPolicy.requireForJobCreation(requester);

        Job job = new Job();
        job.setRequester(requester);
        job.setProblemType(req.problemType);
        job.setProblemDesc(req.problemDesc);
        job.setReqLat(req.lat);
        job.setReqLng(req.lng);
        job.setRewardAmount(req.rewardAmount == null ? 0.0 : req.rewardAmount);
        job.setHelperOtp(randomOtp());
        job.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        job = jobRepository.saveAndFlush(job);

        history.record(job, null, Job.Status.PENDING, requester.getId(), "REQUESTER", null, req.lat, req.lng, "request created");
        return ApiResponse.ok(toDto(job, principal.userId(), null, null));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<JobDto>> available(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestParam(required = false) Double lat,
                                               @RequestParam(required = false) Double lng) {
        // Open requests (requester location, problem) are only visible to approved, active helpers.
        eligibility.requireApprovedActive(principal);
        boolean haveLocation = lat != null && lng != null && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
        double radiusKm = applicationRepository.findByUserId(principal.userId())
                .map(HelperApplication::getServiceRadiusKm).filter(r -> r != null && r > 0).orElse(25).doubleValue();

        List<JobDto> out = jobRepository.findAvailable(Instant.now(), principal.userId()).stream()
                .map(j -> {
                    Double km = haveLocation ? GeoUtil.distanceMeters(lat, lng, j.getReqLat(), j.getReqLng()) / 1000.0 : null;
                    return toDto(j, principal.userId(), km, null);
                })
                .filter(d -> !haveLocation || (d.distanceKm != null && d.distanceKm <= radiusKm))
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        if (haveLocation) {
            out.sort(Comparator.comparingDouble((JobDto d) -> d.distanceKm));
        }
        return ApiResponse.ok(out);
    }

    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public ApiResponse<List<JobDto>> mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        List<Job> jobs = jobRepository.findMine(principal.userId());
        return ApiResponse.ok(jobs.stream().map(j -> toDto(j, principal.userId(), null, null)).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<JobDto> get(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        Job job = jobRepository.findByIdWithParticipants(id)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found"));
        if (job.getStatus() != Job.Status.PENDING) {
            boolean isAdmin = isAdmin(principal);
            if (!isParticipant(job, principal.userId()) && !isAdmin) {
                throw ApiException.forbidden("FORBIDDEN", "Not authorized to view this job");
            }
        }
        return ApiResponse.ok(toDto(job, principal.userId(), null, null));
    }

    @GetMapping("/{id}/history")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> statusHistory(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                @PathVariable UUID id) {
        Job job = jobRepository.findByIdWithParticipants(id)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found"));
        if (!isParticipant(job, principal.userId()) && !isAdmin(principal)) {
            throw ApiException.forbidden("FORBIDDEN", "Not authorized to view this job");
        }
        return ApiResponse.ok(history.list(id));
    }

    // ------------------------------------------------------------------ helper actions

    /** Row lock inside the transaction: two helpers accepting at once cannot both win (second gets 409). */
    @PostMapping("/{id}/accept")
    @Transactional
    public ApiResponse<JobDto> accept(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        limit("jobs:accept:" + principal.userId(), 10, Duration.ofMinutes(1), "Accepting jobs too quickly");

        // Approved + ACTIVE + not blocked + approved profile + no commission due. Role from the DB.
        User helper = eligibility.requireCanAccept(principal);

        Job job = lock(id);
        if (job.getStatus() != Job.Status.PENDING) {
            throw ApiException.conflict("JOB_TAKEN", "Job already taken by someone else");
        }
        if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(Instant.now())) {
            transition(job, Job.Status.EXPIRED, null, "SYSTEM", null, null, null, "expired before acceptance");
            throw ApiException.conflict("JOB_EXPIRED", "This job has expired");
        }
        if (job.getRequester().getId().equals(principal.userId())) {
            throw ApiException.badRequest("SELF_ACCEPT", "Cannot accept your own job");
        }
        if (jobRepository.countByHelperAndStatuses(helper.getId(), ACTIVE_HELPER_STATES) > 0) {
            throw ApiException.conflict("ACTIVE_JOB_EXISTS", "Finish your current job before accepting another");
        }

        job.setHelper(helper);
        job = transition(job, Job.Status.MATCHED, helper.getId(), "HELPER", null, null, null, "accepted");

        push(job, job.getRequester(), "Helper found!", helper.getName() + " accepted your roadside help request.");
        return ApiResponse.ok(toDto(job, principal.userId(), null, null));
    }

    /**
     * Arrival = GPS proximity to the request location and/or the arrival OTP the customer shows.
     * Either proof is enough; the method used is stored with the history row.
     */
    @PostMapping("/{id}/arrive")
    @Transactional
    public ApiResponse<JobDto> arrive(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id,
                                      @Valid @RequestBody ArriveRequest req) {
        limit("jobs:arrive:" + principal.userId(), 10, Duration.ofMinutes(1), "Too many arrival attempts. Please wait.");
        Job job = lock(id);
        requireAssignedHelper(job, principal);
        JobStateMachine.require(job.getStatus(), Job.Status.ARRIVED);

        boolean haveCoords = req.lat != null && req.lng != null;
        boolean gpsOk = haveCoords && GeoUtil.distanceMeters(req.lat, req.lng, job.getReqLat(), job.getReqLng()) <= arrivalRadiusM;
        boolean otpProvided = req.otp != null && !req.otp.isBlank();
        boolean otpOk = otpProvided && job.getOtpAttempts() < 5 && job.getHelperOtp() != null && job.getHelperOtp().equals(req.otp.trim());

        if (!gpsOk && !otpOk) {
            if (otpProvided) {
                if (job.getOtpAttempts() >= 5) {
                    throw ApiException.conflict("OTP_LOCKED", "Too many incorrect attempts. Reach the location or contact support.");
                }
                job.setOtpAttempts(job.getOtpAttempts() + 1);
                jobRepository.save(job);
                throw ApiException.badRequest("INVALID_OTP", "Incorrect arrival OTP");
            }
            throw ApiException.badRequest("NOT_AT_LOCATION",
                    "You are not within " + (int) arrivalRadiusM + " m of the request. Move closer or enter the customer's arrival OTP.");
        }

        String method = gpsOk && otpOk ? "GPS_OTP" : (gpsOk ? "GPS" : "OTP");
        job.setArrivedAt(Instant.now());
        job.setArrivalMethod(method);
        if (haveCoords) {
            job.setArrivalLat(req.lat);
            job.setArrivalLng(req.lng);
        }
        job = transition(job, Job.Status.ARRIVED, principal.userId(), "HELPER", method, req.lat, req.lng, "helper arrived");

        push(job, job.getRequester(), "Helper has arrived",
                "Your helper is at your location. Normal cancellation is no longer available; use Report Issue if something is wrong.");
        return ApiResponse.ok(toDto(job, principal.userId(), null, null));
    }

    @PostMapping("/{id}/start")
    @Transactional
    public ApiResponse<JobDto> start(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        Job job = lock(id);
        requireAssignedHelper(job, principal);
        JobStateMachine.require(job.getStatus(), Job.Status.IN_PROGRESS);
        job = transition(job, Job.Status.IN_PROGRESS, principal.userId(), "HELPER", null, null, null, "work started");
        push(job, job.getRequester(), "Work started", "Your helper has started working on your vehicle.");
        return ApiResponse.ok(toDto(job, principal.userId(), null, null));
    }

    /** Helper marks the work completed, sets the final amount + how the customer paid; customer then confirms with an OTP. */
    @PostMapping("/{id}/work-done")
    @Transactional
    public ApiResponse<JobDto> workDone(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id,
                                        @Valid @RequestBody WorkDoneRequest req) {
        Job job = lock(id);
        requireAssignedHelper(job, principal);
        JobStateMachine.require(job.getStatus(), Job.Status.WORK_DONE);

        double amount = req.finalAmount != null ? req.finalAmount
                : (job.getRewardAmount() == null ? 0.0 : job.getRewardAmount());
        if (amount < 0 || amount > 50000) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Final amount must be between 0 and 50,000");
        }
        job.setFinalAmount(CommissionMath.round2(amount));
        job.setPaymentMode(req.paymentMode);
        job.setWorkDoneAt(Instant.now());
        job.setCompletionOtp(randomOtp());
        job.setCompletionOtpAttempts(0);
        job = transition(job, Job.Status.WORK_DONE, principal.userId(), "HELPER", null, null, null,
                "work done, amount " + job.getFinalAmount() + " via " + req.paymentMode);

        push(job, job.getRequester(), "Work completed?",
                "Your helper says the work is done (\u20B9" + job.getFinalAmount() + "). Confirm with the completion code in the app.");
        return ApiResponse.ok(toDto(job, principal.userId(), null, null));
    }

    // ------------------------------------------------------------------ customer actions

    /** Customer confirms completion by giving the helper the completion OTP shown in the customer app. */
    @PostMapping("/{id}/confirm-completion")
    @Transactional
    public ApiResponse<JobDto> confirmCompletion(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable UUID id, @Valid @RequestBody VerifyOtpRequest req) {
        limit("jobs:confirm:" + principal.userId(), 5, Duration.ofMinutes(1), "Too many attempts. Please wait.");
        Job job = lock(id);
        if (!job.getRequester().getId().equals(principal.userId())) {
            throw ApiException.forbidden("FORBIDDEN", "Only the customer can confirm completion");
        }
        JobStateMachine.require(job.getStatus(), Job.Status.COMPLETED);
        if (job.getCompletionOtpAttempts() >= 5) {
            throw ApiException.conflict("OTP_LOCKED", "Too many incorrect attempts. Use Report Issue / Contact Support.");
        }
        if (job.getCompletionOtp() == null || !job.getCompletionOtp().equals(req.otp.trim())) {
            job.setCompletionOtpAttempts(job.getCompletionOtpAttempts() + 1);
            jobRepository.save(job);
            throw ApiException.badRequest("INVALID_OTP", "Incorrect completion code");
        }

        job.setCompletedAt(Instant.now());
        job = transition(job, Job.Status.COMPLETED, principal.userId(), "REQUESTER", "COMPLETION_OTP", null, null, "customer confirmed");

        User helper = job.getHelper();
        helper.setTotalHelps((helper.getTotalHelps() == null ? 0 : helper.getTotalHelps()) + 1);
        userRepository.save(helper);

        double amount = job.getFinalAmount() != null ? job.getFinalAmount()
                : (job.getRewardAmount() == null ? 0.0 : job.getRewardAmount());
        double commission = commissionService.recordCommission(helper.getId(), job.getId(), amount, job.getPaymentMode());

        push(job, helper, "Job completed",
                commission > 0 ? "Job confirmed. Raahi commission \u20B9" + commission + " is due. Settle it to keep accepting jobs."
                               : "Job confirmed by the customer.");
        return ApiResponse.ok(toDto(job, principal.userId(), null, commission));
    }

    @PostMapping("/{id}/rate")
    @Transactional
    public ApiResponse<Object> rate(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id,
                                    @Valid @RequestBody RateRequest req) {
        Job job = lock(id);
        if (!job.getRequester().getId().equals(principal.userId())) {
            throw ApiException.forbidden("FORBIDDEN", "Only the customer can rate this job");
        }
        if (job.getStatus() != Job.Status.COMPLETED || job.getHelper() == null) {
            throw ApiException.conflict("INVALID_STATE", "Only completed jobs can be rated");
        }
        if (job.isRated()) {
            throw ApiException.conflict("ALREADY_RATED", "You already rated this job");
        }
        jdbc.update("INSERT INTO job_ratings(job_id, rater_id, helper_id, stars, comment) VALUES (?,?,?,?,?)",
                job.getId(), principal.userId(), job.getHelper().getId(), req.stars, req.comment);
        Double avg = jdbc.queryForObject("SELECT AVG(stars) FROM job_ratings WHERE helper_id = ?", Double.class, job.getHelper().getId());
        User helper = job.getHelper();
        helper.setRatingAvg(avg == null ? 0.0 : CommissionMath.round2(avg));
        userRepository.save(helper);
        job.setRated(true);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        history.record(job, Job.Status.COMPLETED, Job.Status.COMPLETED, principal.userId(), "REQUESTER", null, null, null, "RATED " + req.stars);
        return ApiResponse.ok(Map.of("success", true));
    }

    // ------------------------------------------------------------------ cancel / report

    @PostMapping("/{id}/cancel")
    @Transactional
    public ApiResponse<Object> cancel(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id,
                                      @RequestBody(required = false) CancelRequest req) {
        Job job = lock(id);
        boolean isRequester = job.getRequester().getId().equals(principal.userId());
        boolean isAssignedHelper = job.getHelper() != null && job.getHelper().getId().equals(principal.userId());
        if (!isRequester && !isAssignedHelper) {
            throw ApiException.forbidden("FORBIDDEN", "Not a participant on this job");
        }

        Job.Status stage = job.getStatus();
        if (!JobStateMachine.cancellable(stage)) {
            if (stage == Job.Status.ARRIVED || stage == Job.Status.IN_PROGRESS || stage == Job.Status.WORK_DONE) {
                throw ApiException.conflict("CANCEL_NOT_ALLOWED",
                        "The helper has already arrived, so this job can no longer be cancelled. Use Report Issue / Contact Support.");
            }
            throw ApiException.conflict("ALREADY_CLOSED", "This job is already " + stage.name().toLowerCase());
        }

        String reason = req == null || req.reason == null ? null : req.reason.trim();
        User actor = load(principal.userId());
        String actorRole = isRequester ? "REQUESTER" : "HELPER";
        String outcome = "NONE";
        if (stage == Job.Status.MATCHED) {
            outcome = cancellationPolicy.record(actor, job, actorRole, stage);
        }

        if (isAssignedHelper) {
            // The helper backed out before arriving: re-open the request for other helpers.
            User dropped = job.getHelper();
            job.setHelper(null);
            job.setOtpAttempts(0);
            Instant minExpiry = Instant.now().plus(10, ChronoUnit.MINUTES);
            if (job.getExpiresAt() == null || job.getExpiresAt().isBefore(minExpiry)) job.setExpiresAt(minExpiry);
            transition(job, Job.Status.PENDING, principal.userId(), "HELPER", null, null, null,
                    "helper cancelled before arrival; request re-opened" + (reason == null ? "" : ": " + reason));
            push(job, job.getRequester(), "Helper cancelled", "Your helper cancelled. We are looking for another helper.");
        } else {
            User helper = job.getHelper();
            job.setCancelledBy(principal.userId());
            job.setCancelledAt(Instant.now());
            job.setCancelReason(reason);
            transition(job, Job.Status.CANCELLED, principal.userId(), "REQUESTER", null, null, null,
                    "customer cancelled" + (reason == null ? "" : ": " + reason));
            if (helper != null) {
                push(job, helper, "Job cancelled", "The customer cancelled this roadside help job.");
            }
        }
        return ApiResponse.ok(Map.of("success", true, "penalty", outcome));
    }

    /** Available at every stage for participants; this is what replaces cancellation after ARRIVED. */
    @PostMapping("/{id}/report")
    @Transactional
    public ApiResponse<Object> report(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id,
                                      @Valid @RequestBody ReportRequest req) {
        limit("jobs:report:" + principal.userId(), 5, Duration.ofHours(1), "Too many reports. Please wait.");
        Job job = jobRepository.findByIdWithParticipants(id)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found"));
        if (!isParticipant(job, principal.userId())) {
            throw ApiException.forbidden("FORBIDDEN", "Not a participant on this job");
        }
        String kind = req.kind == null ? "ISSUE" : req.kind;
        jdbc.update("INSERT INTO job_reports(job_id, reporter_id, kind, message) VALUES (?,?,?,?)",
                job.getId(), principal.userId(), kind, req.message.trim());
        history.record(job, job.getStatus(), job.getStatus(), principal.userId(),
                job.getRequester().getId().equals(principal.userId()) ? "REQUESTER" : "HELPER", null, null, null, "REPORT " + kind);
        return ApiResponse.ok(Map.of("success", true));
    }

    // ------------------------------------------------------------------ helpers

    private Job lock(UUID id) {
        return jobRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found"));
    }

    private User load(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }

    private void requireAssignedHelper(Job job, AuthenticatedUser principal) {
        if (job.getHelper() == null || !job.getHelper().getId().equals(principal.userId())) {
            throw ApiException.forbidden("FORBIDDEN", "Only the assigned helper can do this");
        }
        // A helper suspended/blocked mid-job can still be stopped here.
        User helper = load(principal.userId());
        if (helper.getStatus() != User.Status.ACTIVE) {
            throw ApiException.forbidden("ACCOUNT_NOT_ACTIVE", "Your account is not active");
        }
    }

    /** Validates the transition, writes it, and records immutable history. */
    private Job transition(Job job, Job.Status to, UUID actorId, String actorRole, String method,
                           Double lat, Double lng, String note) {
        Job.Status from = job.getStatus();
        JobStateMachine.require(from, to);
        job.setStatus(to);
        job.setUpdatedAt(Instant.now());
        job = jobRepository.saveAndFlush(job);
        history.record(job, from, to, actorId, actorRole, method, lat, lng, note);
        return job;
    }

    private void push(Job job, User target, String title, String body) {
        if (target == null) return;
        sessionRegistry.sendToUser(target.getId(), "job:status_change",
                Map.of("jobId", job.getId().toString(), "status", job.getStatus().name()));
        notificationService.send(target, title, body, Map.of("jobId", job.getId().toString(), "type", "job_status_change"));
    }

    private void limit(String key, int max, Duration window, String message) {
        if (!rateLimiter.allow(key, max, window)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", message);
        }
    }

    private boolean isParticipant(Job job, UUID userId) {
        return job.getRequester().getId().equals(userId)
                || (job.getHelper() != null && job.getHelper().getId().equals(userId));
    }

    private boolean isAdmin(AuthenticatedUser principal) {
        return userRepository.findById(principal.userId())
                .map(u -> u.getRole() == User.Role.ADMIN && u.getStatus() == User.Status.ACTIVE).orElse(false);
    }

    private static String randomOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private JobDto toDto(Job j, UUID viewerId, Double distanceKm, Double commission) {
        JobDto dto = new JobDto();
        dto.id = j.getId().toString();
        dto.status = (j.getStatus() == Job.Status.PENDING && j.getExpiresAt() != null && j.getExpiresAt().isBefore(Instant.now()))
                ? "EXPIRED" : j.getStatus().name();
        dto.problemType = j.getProblemType();
        dto.problemDesc = j.getProblemDesc();
        dto.lat = j.getReqLat();
        dto.lng = j.getReqLng();
        dto.rewardAmount = j.getRewardAmount();
        dto.requesterName = j.getRequester().getName();
        dto.createdAt = j.getCreatedAt() == null ? null : j.getCreatedAt().toString();
        boolean viewerIsRequester = j.getRequester().getId().equals(viewerId);
        boolean viewerIsHelper = j.getHelper() != null && j.getHelper().getId().equals(viewerId);
        dto.viewerRole = viewerIsRequester ? "REQUESTER" : (viewerIsHelper ? "HELPER" : null);
        if (j.getHelper() != null) {
            dto.helperName = j.getHelper().getName();
            dto.helperRatingAvg = j.getHelper().getRatingAvg() == null ? 0 : j.getHelper().getRatingAvg();
            dto.helperTotalHelps = j.getHelper().getTotalHelps() == null ? 0 : j.getHelper().getTotalHelps();
            dto.helperVerified = j.getHelper().isVerified();
            dto.helperPhone = (viewerIsRequester || viewerIsHelper) ? j.getHelper().getPhone() : null;
        }
        dto.requesterPhone = viewerIsHelper ? j.getRequester().getPhone() : null;
        // Arrival OTP: only the requester, and only while the helper is still on the way.
        dto.helperOtp = (viewerIsRequester && j.getStatus() == Job.Status.MATCHED) ? j.getHelperOtp() : null;
        // Completion OTP: only the requester, only while waiting for their confirmation.
        dto.completionOtp = (viewerIsRequester && j.getStatus() == Job.Status.WORK_DONE) ? j.getCompletionOtp() : null;
        dto.cancelAllowed = JobStateMachine.cancellable(j.getStatus());
        dto.arrivalMethod = j.getArrivalMethod();
        dto.arrivedAt = j.getArrivedAt() == null ? null : j.getArrivedAt().toString();
        dto.finalAmount = j.getFinalAmount();
        dto.paymentMode = j.getPaymentMode();
        dto.rated = j.isRated();
        dto.distanceKm = distanceKm == null ? null : CommissionMath.round2(distanceKm);
        if (viewerIsHelper && j.getStatus() == Job.Status.COMPLETED) {
            double amt = j.getFinalAmount() != null ? j.getFinalAmount() : (j.getRewardAmount() == null ? 0.0 : j.getRewardAmount());
            dto.commissionAmount = commission != null ? commission : CommissionMath.commission(amt, commissionService.rate());
        }
        return dto;
    }
}
