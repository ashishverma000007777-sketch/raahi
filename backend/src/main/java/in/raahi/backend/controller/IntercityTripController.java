package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.IntercityTripDtos.*;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import in.raahi.backend.service.IntercityTripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/intercity/trips")
public class IntercityTripController {

    private final IntercityTripService tripService;
    private final RateLimiter rateLimiter;

    public IntercityTripController(IntercityTripService tripService, RateLimiter rateLimiter) {
        this.tripService = tripService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ApiResponse<IntercityTripDto> createTrip(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateIntercityTripRequest req
    ) {
        String rateKey = "intercity:create:" + principal.userId();
        if (!rateLimiter.allow(rateKey, 20, Duration.ofHours(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many trips created. Please wait.");
        }
        return ApiResponse.ok(tripService.createTrip(principal.userId(), req));
    }

    @GetMapping("/search")
    public ApiResponse<List<IntercityTripDto>> searchTrips(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String pickupCity,
            @RequestParam(required = false) String destCity,
            @RequestParam(required = false) String vehicleCategory,
            @RequestParam(required = false) Instant fromTime
    ) {
        return ApiResponse.ok(tripService.searchTrips(principal.userId(), pickupCity, destCity, vehicleCategory, fromTime));
    }

    @GetMapping({"/status", "/marketplace/status"})
    public ApiResponse<MarketplaceStatusDto> getMarketplaceStatus(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(tripService.getMarketplaceStatus(principal.userId()));
    }

    @PutMapping({"/status", "/marketplace/status"})
    public ApiResponse<MarketplaceStatusDto> updateMarketplaceStatus(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody UpdateMarketplaceStatusRequest req
    ) {
        return ApiResponse.ok(tripService.updateMarketplaceStatus(principal.userId(), req));
    }

    @GetMapping("/active")
    public ApiResponse<IntercityTripDto> getActiveTrip(@AuthenticationPrincipal AuthenticatedUser principal) {
        IntercityTripDto active = tripService.getActiveTrip(principal.userId());
        return ApiResponse.ok(active);
    }

    @GetMapping("/agent")
    public ApiResponse<List<IntercityTripDto>> getAgentTrips(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(tripService.getAgentTrips(principal.userId()));
    }

    @GetMapping("/driver")
    public ApiResponse<List<IntercityTripDto>> getDriverTrips(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(tripService.getDriverTrips(principal.userId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<IntercityTripDto> getTrip(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(tripService.getTripDetails(principal.userId(), id));
    }

    @GetMapping("/{id}/applications")
    public ApiResponse<List<IntercityTripApplicationDto>> getApplications(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(tripService.getTripApplications(principal.userId(), id));
    }

    @PostMapping("/{id}/apply")
    public ApiResponse<IntercityTripApplicationDto> applyForTrip(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestBody(required = false) ApplyTripRequest req
    ) {
        String rateKey = "intercity:apply:" + principal.userId();
        if (!rateLimiter.allow(rateKey, 30, Duration.ofHours(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many trip applications submitted.");
        }
        return ApiResponse.ok(tripService.applyForTrip(principal.userId(), id, req));
    }

    @PostMapping("/{id}/select-driver/{driverId}")
    public ApiResponse<IntercityTripDto> selectDriver(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @PathVariable UUID driverId
    ) {
        return ApiResponse.ok(tripService.selectDriver(principal.userId(), id, driverId));
    }

    @PostMapping({ "/{id}/arrive", "/{id}/arrive-pickup" })
    public ApiResponse<IntercityTripDto> arriveAtPickup(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(tripService.arriveAtPickup(principal.userId(), id));
    }

    @PostMapping("/{id}/verify-pickup-otp")
    public ApiResponse<IntercityTripDto> verifyPickupOtp(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody VerifyOtpRequest req
    ) {
        return ApiResponse.ok(tripService.verifyPickupOtp(principal.userId(), id, req.otp));
    }

    @PostMapping("/{id}/verify-completion-otp")
    public ApiResponse<IntercityTripDto> verifyCompletionOtp(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody VerifyOtpRequest req
    ) {
        return ApiResponse.ok(tripService.verifyCompletionOtp(principal.userId(), id, req.otp));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<IntercityTripDto> cancelTrip(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody CancelTripRequest req
    ) {
        return ApiResponse.ok(tripService.cancelTrip(principal.userId(), id, req.reason));
    }

    @PostMapping({ "/{id}/settle", "/{id}/settle-payment" })
    public ApiResponse<IntercityTripDto> settlePayment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(tripService.settlePayment(principal.userId(), id));
    }

    @PostMapping({ "/{id}/arrive-dest", "/{id}/arrive-destination" })
    public ApiResponse<IntercityTripDto> arriveAtDestination(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(tripService.arriveAtDestination(principal.userId(), id));
    }

    @PostMapping("/{id}/complete-owner")
    public ApiResponse<IntercityTripDto> completeOwnerTrip(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(tripService.completeOwnerTrip(principal.userId(), id));
    }

    @PostMapping("/{id}/settlement/submit")
    public ApiResponse<IntercityTripDto> submitSettlement(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody SubmitSettlementRequest req
    ) {
        return ApiResponse.ok(tripService.submitSettlement(principal.userId(), id, req));
    }

    @PostMapping("/{id}/settlement/confirm")
    public ApiResponse<IntercityTripDto> confirmSettlement(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(tripService.confirmSettlement(principal.userId(), id));
    }

    @PostMapping("/{id}/dispute")
    public ApiResponse<IntercityTripDto> raiseDispute(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody RaiseDisputeRequest req
    ) {
        return ApiResponse.ok(tripService.raiseDispute(principal.userId(), id, req));
    }

    @PostMapping("/{id}/dispute/resolve")
    public ApiResponse<IntercityTripDto> resolveDispute(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody ResolveDisputeRequest req
    ) {
        return ApiResponse.ok(tripService.resolveDispute(principal.userId(), id, req));
    }
}
