package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.TripDtos.*;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.service.TripService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;
    private final in.raahi.backend.security.RateLimiter rateLimiter;

    public TripController(TripService tripService, in.raahi.backend.security.RateLimiter rateLimiter) {
        this.tripService = tripService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ApiResponse<TripDto> createTrip(@AuthenticationPrincipal AuthenticatedUser principal,
                                           @Valid @RequestBody CreateTripRequest req) {
        if (!rateLimiter.allow("trips:create:" + principal.userId(), 15, java.time.Duration.ofHours(1))) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many trips created. Please wait before creating another trip.");
        }
        return ApiResponse.ok(tripService.createTrip(principal.userId(), req));
    }

    @GetMapping
    public ApiResponse<List<TripDto>> listTrips(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(tripService.listUserTrips(principal.userId()));
    }

    @GetMapping("/active")
    public ApiResponse<TripDto> getActiveTrip(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(tripService.getActiveTrip(principal.userId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<TripDto> getTrip(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @PathVariable UUID id) {
        return ApiResponse.ok(tripService.getTrip(principal.userId(), id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<TripDto> startTrip(@AuthenticationPrincipal AuthenticatedUser principal,
                                          @PathVariable UUID id,
                                          @RequestBody(required = false) StartTripRequest req) {
        return ApiResponse.ok(tripService.startTrip(principal.userId(), id, req));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<TripDto> completeTrip(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @PathVariable UUID id,
                                             @RequestBody(required = false) CompleteTripRequest req) {
        return ApiResponse.ok(tripService.completeTrip(principal.userId(), id, req));
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<TripSummaryDto> getSummary(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @PathVariable UUID id) {
        return ApiResponse.ok(tripService.getTripSummary(principal.userId(), id));
    }
}
