package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.MechanicDtos.*;
import in.raahi.backend.entity.MechanicProfile;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.MechanicProfileRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/mechanics")
public class MechanicController {

    private final MechanicProfileRepository mechanicRepository;
    private final UserRepository userRepository;
    private final in.raahi.backend.security.RateLimiter rateLimiter;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public MechanicController(MechanicProfileRepository mechanicRepository, UserRepository userRepository,
                              in.raahi.backend.security.RateLimiter rateLimiter,
                              org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.mechanicRepository = mechanicRepository;
        this.userRepository = userRepository;
        this.rateLimiter = rateLimiter;
        this.jdbc = jdbc;
    }

    // No fallback to fake/mock mechanics when this list is empty — the old Node backend did
    // that (_getMockMechanics) and it's explicitly banned. Empty list means empty list.
    @GetMapping("/nearby")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ApiResponse<List<MechanicDto>> nearby(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam Double lat, @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "20") Double radius) {

        String userKey = principal != null ? principal.userId().toString() : "anon";
        if (!rateLimiter.allow("mechanics:nearby:" + userKey, 30, java.time.Duration.ofMinutes(1))) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Nearby mechanics requests too frequent. Please slow down.");
        }

        if (lat == null || lng == null || Double.isNaN(lat) || Double.isNaN(lng) || Double.isInfinite(lat) || Double.isInfinite(lng)) {
            throw ApiException.badRequest("MISSING_COORDS", "lat and lng required");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw ApiException.badRequest("INVALID_COORDS", "lat/lng out of range");
        }

        List<MechanicProfile> profiles = mechanicRepository.findNearby(lat, lng, radius);
        List<MechanicDto> dtos = profiles.stream()
                .filter(p -> p.getUser().getRole() == User.Role.MECHANIC)
                .map(p -> toDto(p, lat, lng))
                .collect(Collectors.toList());
        return ApiResponse.ok(dtos);
    }

    @GetMapping("/nearby-shops")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ApiResponse<List<java.util.Map<String, Object>>> nearbyShops(
            @RequestParam Double lat, @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "20") Double radius) {
        if (lat == null || lng == null || !Double.isFinite(lat) || !Double.isFinite(lng)
                || lat < -90 || lat > 90 || lng < -180 || lng > 180)
            throw ApiException.badRequest("INVALID_COORDS", "Valid lat/lng required");
        double r = radius == null ? 20.0 : radius;
        if (!Double.isFinite(r) || r <= 0 || r > 100)
            throw ApiException.badRequest("INVALID_RADIUS", "Radius must be between 0 and 100 km");
        return ApiResponse.ok(jdbc.queryForList("""
            SELECT id::text AS id, shop_name AS name, phone, address,
                   latitude AS lat, longitude AS lng, osm_url AS "osmUrl",
                   status, distance_km AS "distanceKm"
            FROM (
              SELECT id, shop_name, phone, address, latitude, longitude, osm_url, status,
                6371.0 * 2 * ASIN(SQRT(
                  POWER(SIN(RADIANS(latitude - ?) / 2), 2) +
                  COS(RADIANS(?)) * COS(RADIANS(latitude)) *
                  POWER(SIN(RADIANS(longitude - ?) / 2), 2)
                )) AS distance_km
              FROM osm_mechanic_leads WHERE status <> 'REJECTED'
            ) nearby
            WHERE distance_km <= ? ORDER BY distance_km LIMIT 200
            """, lat, lat, lng, r));
    }

    @GetMapping("/{userId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ApiResponse<MechanicDto> get(@PathVariable UUID userId) {
        MechanicProfile p = mechanicRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("MECHANIC_NOT_FOUND", "Mechanic not found"));
        return ApiResponse.ok(toDto(p, p.getCurrentLat(), p.getCurrentLng()));
    }

    // Only a MECHANIC/HELPER account updates its own location — enforced via role check,
    // never trusts a userId in the request body.
    @PutMapping("/location")
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<Object> updateLocation(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody UpdateLocationRequest req) {
        if (!rateLimiter.allow("mechanic:location:" + principal.userId(), 60, java.time.Duration.ofMinutes(1))) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Location updates too frequent. Please wait.");
        }

        if (req.lat == null || req.lng == null || Double.isNaN(req.lat) || Double.isNaN(req.lng) || Double.isInfinite(req.lat) || Double.isInfinite(req.lng)
                || req.lat < -90 || req.lat > 90 || req.lng < -180 || req.lng > 180) {
            throw ApiException.badRequest("INVALID_COORDS", "Valid lat/lng required");
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        if (user.getRole() != User.Role.MECHANIC && user.getRole() != User.Role.HELPER) {
            throw ApiException.forbidden("FORBIDDEN", "Only mechanics/helpers can update service location");
        }
        if (user.getStatus() != User.Status.ACTIVE) {
            throw ApiException.forbidden("ACCOUNT_NOT_ACTIVE", "Your account is not active");
        }

        MechanicProfile profile = mechanicRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("HELPER_NOT_APPROVED", "Your helper account is not approved yet"));
        if (profile.getVerificationStatus() != MechanicProfile.VerificationStatus.APPROVED) {
            throw ApiException.forbidden("HELPER_NOT_APPROVED", "Only approved helpers can update service location");
        }
        profile.setCurrentLat(req.lat);
        profile.setCurrentLng(req.lng);
        mechanicRepository.save(profile);

        return ApiResponse.ok(java.util.Map.of("updated", true));
    }

    private MechanicDto toDto(MechanicProfile p, Double refLat, Double refLng) {
        MechanicDto dto = new MechanicDto();
        dto.userId = p.getUser().getId().toString();
        dto.name = p.getUser().getName();
        dto.phone = p.getUser().getPhone();
        dto.shopName = p.getShopName();
        dto.specializations = p.getSpecializations();
        dto.isAvailable = p.isAvailable();
        dto.lat = p.getCurrentLat();
        dto.lng = p.getCurrentLng();
        dto.ratingAvg = p.getUser().getRatingAvg() == null ? 0 : p.getUser().getRatingAvg();
        dto.distanceKm = (refLat != null && refLng != null && p.getCurrentLat() != null)
                ? haversineKm(refLat, refLng, p.getCurrentLat(), p.getCurrentLng())
                : -1;
        return dto;
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
