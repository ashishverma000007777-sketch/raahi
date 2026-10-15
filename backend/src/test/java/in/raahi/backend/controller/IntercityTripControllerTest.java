package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.IntercityTripDtos.*;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import in.raahi.backend.service.IntercityTripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IntercityTripControllerTest {

    private IntercityTripService tripService;
    private RateLimiter rateLimiter;
    private IntercityTripController controller;
    private UUID userId;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        tripService = mock(IntercityTripService.class);
        rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.allow(anyString(), anyInt(), any(Duration.class))).thenReturn(true);

        controller = new IntercityTripController(tripService, rateLimiter);
        userId = UUID.randomUUID();
        principal = new AuthenticatedUser(userId, "DRIVER");
    }

    @Test
    void createTrip_rateLimitAllowed_success() {
        CreateIntercityTripRequest req = new CreateIntercityTripRequest();
        req.pickupCity = "Delhi";
        req.destCity = "Chandigarh";
        req.fare = BigDecimal.valueOf(3500);

        IntercityTripDto mockDto = new IntercityTripDto();
        mockDto.id = UUID.randomUUID().toString();
        when(tripService.createTrip(eq(userId), eq(req))).thenReturn(mockDto);

        ApiResponse<IntercityTripDto> res = controller.createTrip(principal, req);
        assertTrue(res.success);
        assertEquals(mockDto.id, res.data.id);
        verify(tripService).createTrip(userId, req);
    }

    @Test
    void createTrip_rateLimitExceeded_throws429() {
        when(rateLimiter.allow(startsWith("intercity:create:"), anyInt(), any(Duration.class))).thenReturn(false);

        CreateIntercityTripRequest req = new CreateIntercityTripRequest();
        ApiException ex = assertThrows(ApiException.class, () -> controller.createTrip(principal, req));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.status);
        assertEquals("RATE_LIMIT_EXCEEDED", ex.code);
        verify(tripService, never()).createTrip(any(), any());
    }

    @Test
    void searchTrips_delegatesToService() {
        when(tripService.searchTrips(eq(userId), eq("Delhi"), eq("Chandigarh"), isNull(), any()))
                .thenReturn(List.of(new IntercityTripDto()));

        ApiResponse<List<IntercityTripDto>> res = controller.searchTrips(principal, "Delhi", "Chandigarh", null, null);
        assertTrue(res.success);
        assertEquals(1, res.data.size());
    }

    @Test
    void getActiveTrip_delegatesToService() {
        IntercityTripDto active = new IntercityTripDto();
        active.id = UUID.randomUUID().toString();
        when(tripService.getActiveTrip(userId)).thenReturn(active);

        ApiResponse<IntercityTripDto> res = controller.getActiveTrip(principal);
        assertTrue(res.success);
        assertEquals(active.id, res.data.id);
    }

    @Test
    void getAgentTrips_delegatesToService() {
        when(tripService.getAgentTrips(userId)).thenReturn(List.of(new IntercityTripDto()));
        ApiResponse<List<IntercityTripDto>> res = controller.getAgentTrips(principal);
        assertTrue(res.success);
        assertEquals(1, res.data.size());
    }

    @Test
    void getDriverTrips_delegatesToService() {
        when(tripService.getDriverTrips(userId)).thenReturn(List.of(new IntercityTripDto()));
        ApiResponse<List<IntercityTripDto>> res = controller.getDriverTrips(principal);
        assertTrue(res.success);
        assertEquals(1, res.data.size());
    }

    @Test
    void applyForTrip_rateLimitAllowed_success() {
        UUID tripId = UUID.randomUUID();
        ApplyTripRequest req = new ApplyTripRequest();
        req.driverNotes = "Experienced driver";

        IntercityTripApplicationDto mockApp = new IntercityTripApplicationDto();
        mockApp.id = UUID.randomUUID().toString();
        when(tripService.applyForTrip(eq(userId), eq(tripId), eq(req))).thenReturn(mockApp);

        ApiResponse<IntercityTripApplicationDto> res = controller.applyForTrip(principal, tripId, req);
        assertTrue(res.success);
        assertEquals(mockApp.id, res.data.id);
    }

    @Test
    void applyForTrip_rateLimitExceeded_throws429() {
        when(rateLimiter.allow(startsWith("intercity:apply:"), anyInt(), any(Duration.class))).thenReturn(false);

        UUID tripId = UUID.randomUUID();
        ApiException ex = assertThrows(ApiException.class, () -> controller.applyForTrip(principal, tripId, new ApplyTripRequest()));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.status);
        assertEquals("RATE_LIMIT_EXCEEDED", ex.code);
        verify(tripService, never()).applyForTrip(any(), any(), any());
    }

    @Test
    void selectDriver_delegatesToService() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        IntercityTripDto dto = new IntercityTripDto();
        when(tripService.selectDriver(userId, tripId, driverId)).thenReturn(dto);

        ApiResponse<IntercityTripDto> res = controller.selectDriver(principal, tripId, driverId);
        assertTrue(res.success);
        verify(tripService).selectDriver(userId, tripId, driverId);
    }

    @Test
    void arriveAtPickup_delegatesToService() {
        UUID tripId = UUID.randomUUID();
        IntercityTripDto dto = new IntercityTripDto();
        when(tripService.arriveAtPickup(userId, tripId)).thenReturn(dto);

        ApiResponse<IntercityTripDto> res = controller.arriveAtPickup(principal, tripId);
        assertTrue(res.success);
        verify(tripService).arriveAtPickup(userId, tripId);
    }

    @Test
    void verifyPickupOtp_delegatesToService() {
        UUID tripId = UUID.randomUUID();
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.otp = "123456";
        IntercityTripDto dto = new IntercityTripDto();
        when(tripService.verifyPickupOtp(userId, tripId, "123456")).thenReturn(dto);

        ApiResponse<IntercityTripDto> res = controller.verifyPickupOtp(principal, tripId, req);
        assertTrue(res.success);
        verify(tripService).verifyPickupOtp(userId, tripId, "123456");
    }

    @Test
    void verifyCompletionOtp_delegatesToService() {
        UUID tripId = UUID.randomUUID();
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.otp = "654321";
        IntercityTripDto dto = new IntercityTripDto();
        when(tripService.verifyCompletionOtp(userId, tripId, "654321")).thenReturn(dto);

        ApiResponse<IntercityTripDto> res = controller.verifyCompletionOtp(principal, tripId, req);
        assertTrue(res.success);
        verify(tripService).verifyCompletionOtp(userId, tripId, "654321");
    }

    @Test
    void cancelTrip_delegatesToService() {
        UUID tripId = UUID.randomUUID();
        CancelTripRequest req = new CancelTripRequest();
        req.reason = "Customer cancelled";
        IntercityTripDto dto = new IntercityTripDto();
        when(tripService.cancelTrip(userId, tripId, "Customer cancelled")).thenReturn(dto);

        ApiResponse<IntercityTripDto> res = controller.cancelTrip(principal, tripId, req);
        assertTrue(res.success);
        verify(tripService).cancelTrip(userId, tripId, "Customer cancelled");
    }

    @Test
    void settlePayment_delegatesToService() {
        UUID tripId = UUID.randomUUID();
        IntercityTripDto dto = new IntercityTripDto();
        when(tripService.settlePayment(userId, tripId)).thenReturn(dto);

        ApiResponse<IntercityTripDto> res = controller.settlePayment(principal, tripId);
        assertTrue(res.success);
        verify(tripService).settlePayment(userId, tripId);
    }
}
