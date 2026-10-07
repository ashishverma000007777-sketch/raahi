package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.DailyDtos.*;
import in.raahi.backend.entity.*;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.places.PlacesProvider;
import in.raahi.backend.repository.*;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/daily")
public class DailyController {

    private final DailyStreakRepository streakRepository;
    private final DailyCheckinRepository checkinRepository;
    private final HighwayAlertRepository alertRepository;
    private final HighwayAlertVoteRepository voteRepository;
    private final DailyTipRepository tipRepository;
    private final UserRepository userRepository;
    private final PlacesProvider placesProvider;
    private final RateLimiter rateLimiter;

    public DailyController(DailyStreakRepository streakRepository,
                           DailyCheckinRepository checkinRepository,
                           HighwayAlertRepository alertRepository,
                           HighwayAlertVoteRepository voteRepository,
                           DailyTipRepository tipRepository,
                           UserRepository userRepository,
                           PlacesProvider placesProvider,
                           RateLimiter rateLimiter) {
        this.streakRepository = streakRepository;
        this.checkinRepository = checkinRepository;
        this.alertRepository = alertRepository;
        this.voteRepository = voteRepository;
        this.tipRepository = tipRepository;
        this.userRepository = userRepository;
        this.placesProvider = placesProvider;
        this.rateLimiter = rateLimiter;
    }

    private static final java.time.ZoneId INDIA_ZONE = java.time.ZoneId.of("Asia/Kolkata");

    // ---- Streak ----

    @GetMapping("/streak")
    @Transactional(readOnly = true)
    public ApiResponse<StreakDto> streak(@AuthenticationPrincipal AuthenticatedUser principal) {
        Optional<DailyStreak> existing = streakRepository.findByUserId(principal.userId());
        return ApiResponse.ok(toStreakDto(existing.orElse(null)));
    }

    @PostMapping("/streak/checkin")
    @Transactional
    public ApiResponse<StreakDto> checkin(@AuthenticationPrincipal AuthenticatedUser principal) {
        String rateLimitKey = "daily:checkin:" + principal.userId();
        if (!rateLimiter.allow(rateLimitKey, 10, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many check-in requests. Please wait.");
        }

        User user = userRepository.findByIdForUpdate(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        LocalDate today = LocalDate.now(INDIA_ZONE);

        // 1. Enforce unique check-in at DB constraint level
        if (checkinRepository.existsByUserIdAndCheckinDate(principal.userId(), today)) {
            throw ApiException.conflict("ALREADY_CHECKED_IN", "Already checked in today");
        }

        try {
            DailyCheckin checkinRecord = new DailyCheckin();
            checkinRecord.setUser(user);
            checkinRecord.setCheckinDate(today);
            checkinRepository.saveAndFlush(checkinRecord);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("ALREADY_CHECKED_IN", "Already checked in today");
        }

        // 2. Lock the user's DailyStreak row to serialize concurrent streak updates
        DailyStreak streak = streakRepository.findByUserIdForUpdate(principal.userId()).orElseGet(() -> {
            DailyStreak s = new DailyStreak();
            s.setUser(user);
            return s;
        });

        if (today.equals(streak.getLastCheckin())) {
            throw ApiException.conflict("ALREADY_CHECKED_IN", "Already checked in today");
        }

        boolean wasYesterday = streak.getLastCheckin() != null && streak.getLastCheckin().equals(today.minusDays(1));
        int newStreak = wasYesterday ? streak.getCurrentStreak() + 1 : 1;
        streak.setCurrentStreak(newStreak);
        streak.setLongestStreak(Math.max(newStreak, streak.getLongestStreak()));
        streak.setTotalCheckins(streak.getTotalCheckins() + 1);
        streak.setLastCheckin(today);
        streak.setUpdatedAt(Instant.now());
        streak = streakRepository.save(streak);

        StreakDto dto = toStreakDto(streak);
        dto.reward = (newStreak % 7 == 0) ? "7_day_milestone" : null;
        return ApiResponse.ok(dto);
    }

    // ---- Highway Alerts ----

    @GetMapping("/alerts")
    @Transactional(readOnly = true)
    public ApiResponse<List<AlertDto>> alerts(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestParam(required = false) String type) {
        List<HighwayAlert> alerts = alertRepository.findActive(type, Instant.now());
        return ApiResponse.ok(alerts.stream().map(a -> toAlertDto(a, principal.userId())).collect(Collectors.toList()));
    }

    @PostMapping("/alerts")
    @Transactional
    public ApiResponse<AlertDto> createAlert(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody CreateAlertRequest req) {
        String rateLimitKey = "daily:alert:create:" + principal.userId();
        if (!rateLimiter.allow(rateLimitKey, 10, Duration.ofHours(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many alerts created. Please wait before creating another.");
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        HighwayAlert alert = new HighwayAlert();
        alert.setUser(user);
        alert.setType(req.type);
        alert.setMessage(req.message);
        alert.setLocation(req.location);
        alert.setLat(req.lat);
        alert.setLng(req.lng);
        alert = alertRepository.save(alert);

        return ApiResponse.ok(toAlertDto(alert, principal.userId()));
    }

    @PostMapping("/alerts/{id}/vote")
    @Transactional
    public ApiResponse<AlertDto> vote(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable UUID id, @Valid @RequestBody VoteRequest req) {
        if (!req.vote.equals("up") && !req.vote.equals("down")) {
            throw ApiException.badRequest("INVALID_VOTE", "vote must be 'up' or 'down'");
        }

        String rateLimitKey = "daily:alert:vote:" + principal.userId();
        if (!rateLimiter.allow(rateLimitKey, 30, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Voting too fast. Please slow down.");
        }

        HighwayAlert alert = alertRepository.findByIdWithUser(id)
                .orElseThrow(() -> ApiException.notFound("ALERT_NOT_FOUND", "Alert not found"));

        if (voteRepository.findByAlertIdAndUserId(id, principal.userId()).isPresent()) {
            throw ApiException.conflict("ALREADY_VOTED", "You already voted on this alert");
        }

        HighwayAlertVote voteRow = new HighwayAlertVote();
        voteRow.setAlertId(id);
        voteRow.setUserId(principal.userId());
        voteRow.setVote(req.vote);
        try {
            voteRepository.saveAndFlush(voteRow);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("ALREADY_VOTED", "You already voted on this alert");
        }

        if (req.vote.equals("up")) {
            alertRepository.incrementUpvotes(id);
        } else {
            alertRepository.incrementDownvotes(id);
        }
        HighwayAlert updated = alertRepository.findByIdWithUser(id).orElse(alert);

        return ApiResponse.ok(toAlertDto(updated, principal.userId()));
    }

    // ---- Tips ----

    @GetMapping("/tips")
    public ApiResponse<TipDto> todaysTip() {
        List<DailyTip> tips = tipRepository.findAll();
        if (tips.isEmpty()) {
            throw ApiException.notFound("NO_TIPS", "No tips configured");
        }
        int idx = LocalDate.now(INDIA_ZONE).getDayOfYear() % tips.size();
        DailyTip tip = tips.get(idx);
        TipDto dto = new TipDto();
        dto.title = tip.getTitle();
        dto.body = tip.getBody();
        return ApiResponse.ok(dto);
    }

    // ---- Nearby Places (real OSM Overpass data, no fake fallback) ----

    @GetMapping("/places")
    public ApiResponse<List<PlaceDto>> places(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestParam Double lat, @RequestParam Double lng,
                                               @RequestParam(defaultValue = "dhaba") String type) {
        String userKey = principal != null ? principal.userId().toString() : "anon";
        if (!rateLimiter.allow("daily:places:" + userKey, 20, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many nearby places requests. Please slow down.");
        }

        if (lat == null || lng == null || Double.isNaN(lat) || Double.isNaN(lng) || Double.isInfinite(lat) || Double.isInfinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw ApiException.badRequest("INVALID_COORDS", "Valid lat/lng required");
        }
        try {
            List<PlacesProvider.Place> places = placesProvider.nearby(type, lat, lng, 5.0);
            return ApiResponse.ok(places.stream().map(this::toPlaceDto).collect(Collectors.toList()));
        } catch (PlacesProvider.PlacesUnavailableException e) {
            throw ApiException.serviceUnavailable("PLACES_UNAVAILABLE", "Could not load nearby places right now. Try again shortly.");
        }
    }

    private StreakDto toStreakDto(DailyStreak s) {
        StreakDto dto = new StreakDto();
        if (s == null) {
            dto.currentStreak = 0; dto.longestStreak = 0; dto.totalCheckins = 0;
            dto.lastCheckin = null; dto.canCheckin = true;
            return dto;
        }
        dto.currentStreak = s.getCurrentStreak();
        dto.longestStreak = s.getLongestStreak();
        dto.totalCheckins = s.getTotalCheckins();
        dto.lastCheckin = s.getLastCheckin() != null ? s.getLastCheckin().toString() : null;
        dto.canCheckin = !LocalDate.now(INDIA_ZONE).equals(s.getLastCheckin());
        return dto;
    }

    private AlertDto toAlertDto(HighwayAlert a, UUID viewerId) {
        AlertDto dto = new AlertDto();
        dto.id = a.getId().toString();
        dto.type = a.getType();
        dto.message = a.getMessage();
        dto.location = a.getLocation();
        dto.lat = a.getLat();
        dto.lng = a.getLng();
        dto.upvotes = a.getUpvotes();
        dto.downvotes = a.getDownvotes();
        dto.createdAt = a.getCreatedAt().toString();
        dto.postedBy = a.getUser().getName();
        dto.myVote = voteRepository.findByAlertIdAndUserId(a.getId(), viewerId).map(HighwayAlertVote::getVote).orElse(null);
        return dto;
    }

    private PlaceDto toPlaceDto(PlacesProvider.Place p) {
        PlaceDto dto = new PlaceDto();
        dto.id = p.id();
        dto.name = p.name();
        dto.lat = p.lat();
        dto.lng = p.lng();
        dto.distanceKm = p.distanceKm();
        return dto;
    }
}
