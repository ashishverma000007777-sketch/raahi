package in.raahi.backend.service;

import in.raahi.backend.dto.IntercityTripDtos.*;
import in.raahi.backend.entity.IntercityTrip;
import in.raahi.backend.entity.IntercityTripApplication;
import in.raahi.backend.entity.IntercityTripApplication.ApplicationStatus;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.notification.NotificationService;
import in.raahi.backend.repository.IntercityTripApplicationRepository;
import in.raahi.backend.repository.IntercityTripRepository;
import in.raahi.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntercityTripServiceTest {

    private IntercityTripRepository tripRepository;
    private IntercityTripApplicationRepository applicationRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;

    private IntercityTripService service;

    private User agent;
    private User driver;
    private User otherDriver;

    @BeforeEach
    void setUp() {
        tripRepository = mock(IntercityTripRepository.class);
        applicationRepository = mock(IntercityTripApplicationRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);

        service = new IntercityTripService(
                tripRepository,
                applicationRepository,
                userRepository,
                notificationService
        );

        agent = new User();
        agent.setId(UUID.randomUUID());
        agent.setPhone("+919876543210");
        agent.setName("Sharma Travels");
        agent.setAgencyName("Sharma Travel Agency");
        agent.setRole(User.Role.TRAVEL_AGENT);
        agent.setStatus(User.Status.ACTIVE);

        driver = new User();
        driver.setId(UUID.randomUUID());
        driver.setPhone("+919876543211");
        driver.setName("Rajesh Kumar");
        driver.setRole(User.Role.DRIVER);
        driver.setStatus(User.Status.ACTIVE);
        driver.setTotalHelps(10);

        otherDriver = new User();
        otherDriver.setId(UUID.randomUUID());
        otherDriver.setPhone("+919876543212");
        otherDriver.setName("Vikram Singh");
        otherDriver.setRole(User.Role.DRIVER);
        otherDriver.setStatus(User.Status.ACTIVE);

        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(userRepository.findById(driver.getId())).thenReturn(Optional.of(driver));
        when(userRepository.findById(otherDriver.getId())).thenReturn(Optional.of(otherDriver));

        when(tripRepository.save(any(IntercityTrip.class))).thenAnswer(inv -> {
            IntercityTrip t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });

        when(applicationRepository.save(any(IntercityTripApplication.class))).thenAnswer(inv -> {
            IntercityTripApplication a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            return a;
        });
    }

    @Test
    void createTrip_Success() {
        CreateIntercityTripRequest req = new CreateIntercityTripRequest();
        req.pickupCity = "Delhi";
        req.destCity = "Jaipur";
        req.pickupAddress = "Connaught Place, New Delhi";
        req.destAddress = "MI Road, Jaipur";
        req.pickupLat = 28.6315;
        req.pickupLng = 77.2167;
        req.destLat = 26.9124;
        req.destLng = 75.7873;
        req.departureTime = Instant.now().plusSeconds(7200);
        req.tripType = "ONE_WAY";
        req.vehicleCategory = "SEDAN";
        req.estimatedDistanceKm = new BigDecimal("280.0");
        req.estimatedDurationMin = 300;
        req.fare = new BigDecimal("4500.00");
        req.paymentMode = "DIRECT_CASH";

        IntercityTripDto result = service.createTrip(agent.getId(), req);

        assertNotNull(result);
        assertEquals("Delhi", result.pickupCity);
        assertEquals("Jaipur", result.destCity);
        assertEquals("PUBLISHED", result.status);
        assertEquals("4500.00", result.fare.toString());
        verify(tripRepository).save(any(IntercityTrip.class));
    }

    @Test
    void createTrip_PastDeparture_ThrowsBadRequest() {
        CreateIntercityTripRequest req = new CreateIntercityTripRequest();
        req.pickupCity = "Delhi";
        req.destCity = "Jaipur";
        req.departureTime = Instant.now().minusSeconds(1000);

        assertThrows(ApiException.class, () -> service.createTrip(agent.getId(), req));
    }

    @Test
    void applyForTrip_Success() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.PUBLISHED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(applicationRepository.existsByTripIdAndDriverId(trip.getId(), driver.getId())).thenReturn(false);

        ApplyTripRequest req = new ApplyTripRequest();
        req.driverNotes = "Experienced highway driver with Dzire";

        IntercityTripApplicationDto appDto = service.applyForTrip(driver.getId(), trip.getId(), req);

        assertNotNull(appDto);
        assertEquals("APPLIED", appDto.status);
        assertEquals("Experienced highway driver with Dzire", appDto.driverNotes);
        verify(notificationService).send(eq(agent), eq("New Driver Application"), anyString(), isNull());
    }

    @Test
    void applyForTrip_OwnTrip_ThrowsBadRequest() {
        IntercityTrip trip = createSampleTrip();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThrows(ApiException.class, () -> service.applyForTrip(agent.getId(), trip.getId(), new ApplyTripRequest()));
    }

    @Test
    void applyForTrip_Duplicate_ThrowsConflict() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.PUBLISHED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(applicationRepository.existsByTripIdAndDriverId(trip.getId(), driver.getId())).thenReturn(true);

        assertThrows(ApiException.class, () -> service.applyForTrip(driver.getId(), trip.getId(), new ApplyTripRequest()));
    }

    @Test
    void selectDriver_Success_GeneratesPickupOtpAndRejectsOthers() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.PUBLISHED);
        when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));

        IntercityTripApplication app1 = new IntercityTripApplication();
        app1.setId(UUID.randomUUID());
        app1.setTrip(trip);
        app1.setDriver(driver);
        app1.setStatus(ApplicationStatus.APPLIED);

        IntercityTripApplication app2 = new IntercityTripApplication();
        app2.setId(UUID.randomUUID());
        app2.setTrip(trip);
        app2.setDriver(otherDriver);
        app2.setStatus(ApplicationStatus.APPLIED);

        when(applicationRepository.findByTripIdOrderByCreatedAtAsc(trip.getId())).thenReturn(List.of(app1, app2));
        when(applicationRepository.findByTripIdAndDriverId(trip.getId(), driver.getId())).thenReturn(Optional.of(app1));

        IntercityTripDto result = service.selectDriver(agent.getId(), trip.getId(), driver.getId());

        assertEquals("ASSIGNED", result.status);
        assertEquals(driver.getId().toString(), result.driverId);
        assertNotNull(result.pickupOtp, "Agent must see pickup OTP");
        assertEquals(6, result.pickupOtp.length(), "Pickup OTP must be 6 digits");

        assertEquals(ApplicationStatus.ACCEPTED, app1.getStatus());
        assertEquals(ApplicationStatus.REJECTED, app2.getStatus());
        verify(notificationService).send(eq(driver), eq("Trip Confirmed!"), anyString(), isNull());
    }

    @Test
    void arriveAtPickup_Success() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ASSIGNED);
        trip.setDriver(driver);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        IntercityTripDto result = service.arriveAtPickup(driver.getId(), trip.getId());

        assertEquals("ARRIVED", result.status);
        assertNotNull(result.arrivedAt);
        verify(notificationService).send(eq(agent), eq("Driver Arrived"), anyString(), isNull());
    }

    @Test
    void verifyPickupOtp_Success_GeneratesCompletionOtp() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ARRIVED);
        trip.setDriver(driver);
        trip.setPickupOtp("123456");
        trip.setPickupOtpAttempts(0);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        IntercityTripDto result = service.verifyPickupOtp(driver.getId(), trip.getId(), "123456");

        assertEquals("IN_PROGRESS", result.status);
        assertNotNull(result.startedAt);
        assertNotNull(trip.getCompletionOtp());
        assertEquals(6, trip.getCompletionOtp().length());
        verify(notificationService).send(eq(agent), eq("Trip Started"), anyString(), isNull());
    }

    @Test
    void verifyPickupOtp_InvalidOtp_IncrementsAttempts() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ARRIVED);
        trip.setDriver(driver);
        trip.setPickupOtp("123456");
        trip.setPickupOtpAttempts(0);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.verifyPickupOtp(driver.getId(), trip.getId(), "999999"));

        assertEquals("INVALID_OTP", e.code);
        assertEquals(1, trip.getPickupOtpAttempts());
    }

    @Test
    void verifyPickupOtp_MaxAttemptsExceeded_Throws() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ARRIVED);
        trip.setDriver(driver);
        trip.setPickupOtp("123456");
        trip.setPickupOtpAttempts(5);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.verifyPickupOtp(driver.getId(), trip.getId(), "123456"));

        assertEquals("MAX_OTP_ATTEMPTS_EXCEEDED", e.code);
    }

    @Test
    void verifyCompletionOtp_Success_CompletesTripAndIncrementsHelps() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        trip.setDriver(driver);
        trip.setCompletionOtp("654321");
        trip.setCompletionOtpAttempts(0);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        IntercityTripDto result = service.verifyCompletionOtp(driver.getId(), trip.getId(), "654321");

        assertEquals("COMPLETED", result.status);
        assertEquals("PAID", result.paymentStatus);
        assertNotNull(result.completedAt);
        assertEquals(11, driver.getTotalHelps(), "Driver total helps must be incremented upon completion");
        verify(userRepository).save(driver);
        verify(notificationService).send(eq(agent), eq("Trip Completed"), anyString(), isNull());
    }

    @Test
    void security_OtpMaskedForDriverAndPublic() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ASSIGNED);
        trip.setDriver(driver);
        trip.setPickupOtp("789123");
        trip.setCompletionOtp("456789");
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        // In ASSIGNED state: Agent sees pickupOtp, but not completionOtp (not generated/needed yet)
        IntercityTripDto agentView = service.getTripDetails(agent.getId(), trip.getId());
        assertEquals("789123", agentView.pickupOtp);
        assertNull(agentView.completionOtp, "Completion OTP should not be visible before trip is IN_PROGRESS");

        // Assigned driver CANNOT see OTPs in DTO (must get them from passenger/agent verbally)
        IntercityTripDto driverView = service.getTripDetails(driver.getId(), trip.getId());
        assertNull(driverView.pickupOtp, "Driver must NOT see pickup OTP in API payload");
        assertNull(driverView.completionOtp, "Driver must NOT see completion OTP in API payload");

        // When trip moves to IN_PROGRESS, Agent sees completionOtp
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        IntercityTripDto agentInProgressView = service.getTripDetails(agent.getId(), trip.getId());
        assertEquals("456789", agentInProgressView.completionOtp);

        // Driver STILL cannot see completion OTP
        IntercityTripDto driverInProgressView = service.getTripDetails(driver.getId(), trip.getId());
        assertNull(driverInProgressView.completionOtp, "Driver must NOT see completion OTP in API payload even when in progress");

        // Public viewer CANNOT see OTPs
        IntercityTripDto publicView = service.getTripDetails(otherDriver.getId(), trip.getId());
        assertNull(publicView.pickupOtp);
        assertNull(publicView.completionOtp);
    }

    @Test
    void cancelTrip_ByAgent_Success() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ASSIGNED);
        trip.setDriver(driver);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        IntercityTripDto result = service.cancelTrip(agent.getId(), trip.getId(), "Client cancelled booking");

        assertEquals("CANCELLED", result.status);
        assertEquals("Client cancelled booking", result.cancelReason);
        verify(notificationService).send(eq(driver), eq("Trip Cancelled"), anyString(), isNull());
    }

    @Test
    void cancelTrip_ActiveTrip_ThrowsBadRequest() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        trip.setDriver(driver);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.cancelTrip(agent.getId(), trip.getId(), "Need to cancel"));

        assertEquals("CANNOT_CANCEL_ACTIVE_TRIP", e.code);
    }

    @Test
    void searchTrips_FiltersCorrectly() {
        IntercityTrip trip = createSampleTrip();
        when(tripRepository.searchTrips(eq("Delhi"), eq("Agra"), eq("SUV"), any(Instant.class)))
                .thenReturn(List.of(trip));

        List<IntercityTripDto> results = service.searchTrips(driver.getId(), "Delhi", "Agra", "SUV", null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Delhi", results.get(0).pickupCity);
        assertEquals("Agra", results.get(0).destCity);
        assertNull(results.get(0).pickupOtp, "Search results must never leak OTPs");
    }

    @Test
    void selectDriver_AlreadyAssigned_ThrowsConflict() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ASSIGNED);
        when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.selectDriver(agent.getId(), trip.getId(), driver.getId()));

        assertEquals("TRIP_ALREADY_ASSIGNED", e.code);
    }

    @Test
    void selectDriver_NotOwner_ThrowsForbidden() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.PUBLISHED);
        when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.selectDriver(otherDriver.getId(), trip.getId(), driver.getId()));

        assertEquals("NOT_TRIP_OWNER", e.code);
    }

    @Test
    void getTripApplications_NotOwner_ThrowsForbidden() {
        IntercityTrip trip = createSampleTrip();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.getTripApplications(driver.getId(), trip.getId()));

        assertEquals("NOT_TRIP_OWNER", e.code);
    }

    @Test
    void arriveAtPickup_UnauthorizedDriver_ThrowsForbidden() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ASSIGNED);
        trip.setDriver(driver);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.arriveAtPickup(otherDriver.getId(), trip.getId()));

        assertEquals("NOT_ASSIGNED_DRIVER", e.code);
    }

    @Test
    void arriveAtPickup_InvalidTransition_ThrowsBadRequest() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.PUBLISHED);
        trip.setDriver(driver);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.arriveAtPickup(driver.getId(), trip.getId()));

        assertEquals("INVALID_STATUS_TRANSITION", e.code);
    }

    @Test
    void verifyPickupOtp_UnauthorizedDriver_ThrowsForbidden() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ARRIVED);
        trip.setDriver(driver);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.verifyPickupOtp(otherDriver.getId(), trip.getId(), "123456"));

        assertEquals("NOT_ASSIGNED_DRIVER", e.code);
    }

    @Test
    void verifyPickupOtp_InvalidTransition_ThrowsBadRequest() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.PUBLISHED);
        trip.setDriver(driver);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.verifyPickupOtp(driver.getId(), trip.getId(), "123456"));

        assertEquals("INVALID_STATUS_TRANSITION", e.code);
    }

    @Test
    void verifyCompletionOtp_InvalidOtp_IncrementsAttempts() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        trip.setDriver(driver);
        trip.setCompletionOtp("654321");
        trip.setCompletionOtpAttempts(1);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.verifyCompletionOtp(driver.getId(), trip.getId(), "000000"));

        assertEquals("INVALID_OTP", e.code);
        assertEquals(2, trip.getCompletionOtpAttempts());
    }

    @Test
    void verifyCompletionOtp_MaxAttemptsExceeded_Throws() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        trip.setDriver(driver);
        trip.setCompletionOtp("654321");
        trip.setCompletionOtpAttempts(5);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.verifyCompletionOtp(driver.getId(), trip.getId(), "654321"));

        assertEquals("MAX_OTP_ATTEMPTS_EXCEEDED", e.code);
    }

    @Test
    void cancelTrip_UnauthorizedUser_ThrowsForbidden() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.ASSIGNED);
        trip.setDriver(driver);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.cancelTrip(otherDriver.getId(), trip.getId(), "Should fail"));

        assertEquals("NOT_AUTHORIZED", e.code);
    }

    @Test
    void settlePayment_Success() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.COMPLETED);
        trip.setPaymentStatus(IntercityTrip.PaymentStatus.PAID);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        IntercityTripDto result = service.settlePayment(agent.getId(), trip.getId());

        assertEquals("SETTLED", result.paymentStatus);
    }

    @Test
    void settlePayment_NotOwner_ThrowsForbidden() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.settlePayment(driver.getId(), trip.getId()));

        assertEquals("NOT_TRIP_OWNER", e.code);
    }

    @Test
    void settlePayment_TripNotCompleted_ThrowsBadRequest() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        ApiException e = assertThrows(ApiException.class, () ->
                service.settlePayment(agent.getId(), trip.getId()));

        assertEquals("TRIP_NOT_COMPLETED", e.code);
    }

    @Test
    void getMarketplaceStatus_ReturnsCorrectStatus() {
        driver.setMarketplaceOnline(true);
        driver.setPreferredPickupCity("Delhi");
        when(userRepository.findById(driver.getId())).thenReturn(Optional.of(driver));

        MarketplaceStatusDto status = service.getMarketplaceStatus(driver.getId());
        assertNotNull(status);
        assertTrue(status.online);
        assertEquals("Delhi", status.pickupCity);
    }

    @Test
    void updateMarketplaceStatus_PersistsAndReturns() {
        when(userRepository.findById(driver.getId())).thenReturn(Optional.of(driver));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMarketplaceStatusRequest req = new UpdateMarketplaceStatusRequest();
        req.online = true;
        req.pickupCity = "Jaipur";

        MarketplaceStatusDto status = service.updateMarketplaceStatus(driver.getId(), req);
        assertNotNull(status);
        assertTrue(status.online);
        assertEquals("Jaipur", status.pickupCity);
        assertTrue(driver.isMarketplaceOnline());
        assertEquals("Jaipur", driver.getPreferredPickupCity());
    }

    @Test
    void createTrip_PublishedTrip_NotifiesMatchingOnlineDrivers() {
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(tripRepository.save(any(IntercityTrip.class))).thenAnswer(inv -> {
            IntercityTrip t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        User onlineDriver = new User();
        onlineDriver.setId(UUID.randomUUID());
        onlineDriver.setMarketplaceOnline(true);
        onlineDriver.setPreferredPickupCity("Delhi");

        when(userRepository.findOnlineMarketplaceDriversMatchingCity(eq("Delhi"), eq(agent.getId())))
                .thenReturn(List.of(onlineDriver));

        CreateIntercityTripRequest req = new CreateIntercityTripRequest();
        req.pickupCity = "Delhi";
        req.destCity = "Agra";
        req.pickupAddress = "Taj Palace, New Delhi";
        req.destAddress = "Taj East Gate, Agra";
        req.pickupLat = 28.6000;
        req.pickupLng = 77.2000;
        req.destLat = 27.1751;
        req.destLng = 78.0421;
        req.departureTime = Instant.now().plusSeconds(7200);
        req.vehicleCategory = "SUV";
        req.fare = new BigDecimal("4500.00");
        req.saveAsDraft = false;

        IntercityTripDto created = service.createTrip(agent.getId(), req);
        assertNotNull(created);
        assertEquals("PUBLISHED", created.status);

        verify(notificationService, times(1)).send(eq(onlineDriver), contains("Delhi"), contains("4500"), any());
    }

    @Test
    void ownerPricing_CalculationsMatchFormulas() {
        // 20 km: Suggested ₹300, min ₹300
        assertEquals(new BigDecimal("300"), IntercityTripService.calculateOwnerSuggestedFare(new BigDecimal("20")));
        assertEquals(new BigDecimal("300"), IntercityTripService.calculateOwnerMinimumFare(new BigDecimal("20")));

        // 30 km: Suggested ₹450, min ₹450
        assertEquals(new BigDecimal("450"), IntercityTripService.calculateOwnerSuggestedFare(new BigDecimal("30")));
        assertEquals(new BigDecimal("450"), IntercityTripService.calculateOwnerMinimumFare(new BigDecimal("30")));

        // 40 km: Suggested ₹600, min ₹600
        assertEquals(new BigDecimal("600"), IntercityTripService.calculateOwnerSuggestedFare(new BigDecimal("40")));
        assertEquals(new BigDecimal("600"), IntercityTripService.calculateOwnerMinimumFare(new BigDecimal("40")));

        // 50 km: Suggested ₹750, min ₹750
        assertEquals(new BigDecimal("750"), IntercityTripService.calculateOwnerSuggestedFare(new BigDecimal("50")));
        assertEquals(new BigDecimal("750"), IntercityTripService.calculateOwnerMinimumFare(new BigDecimal("50")));

        // 60 km: Suggested ₹900, min ₹720
        assertEquals(new BigDecimal("900"), IntercityTripService.calculateOwnerSuggestedFare(new BigDecimal("60")));
        assertEquals(new BigDecimal("720"), IntercityTripService.calculateOwnerMinimumFare(new BigDecimal("60")));

        // 100 km: Suggested ₹1500, min ₹1200
        assertEquals(new BigDecimal("1500"), IntercityTripService.calculateOwnerSuggestedFare(new BigDecimal("100")));
        assertEquals(new BigDecimal("1200"), IntercityTripService.calculateOwnerMinimumFare(new BigDecimal("100")));
    }

    @Test
    void createTrip_OwnerRideBelowMinimumFare_ThrowsBadRequest() {
        CreateIntercityTripRequest req = new CreateIntercityTripRequest();
        req.pickupCity = "Delhi";
        req.destCity = "Sonipat";
        req.pickupAddress = "CP";
        req.destAddress = "Murthal";
        req.pickupLat = 28.6;
        req.pickupLng = 77.2;
        req.destLat = 28.9;
        req.destLng = 77.1;
        req.departureTime = Instant.now().plusSeconds(3600);
        req.vehicleCategory = "SEDAN";
        req.rideType = "OWNER";
        req.estimatedDistanceKm = new BigDecimal("50"); // min fare is 750
        req.fare = new BigDecimal("700"); // below min

        ApiException ex = assertThrows(ApiException.class, () -> service.createTrip(agent.getId(), req));
        assertEquals("FARE_BELOW_MINIMUM", ex.code);
    }

    @Test
    void createTrip_OwnerRideByNonAgentUser_Allowed() {
        User helperUser = new User();
        helperUser.setId(UUID.randomUUID());
        helperUser.setName("Helper User");
        helperUser.setRole(User.Role.HELPER);
        helperUser.setStatus(User.Status.ACTIVE);
        when(userRepository.findById(helperUser.getId())).thenReturn(Optional.of(helperUser));

        CreateIntercityTripRequest req = new CreateIntercityTripRequest();
        req.pickupCity = "Delhi";
        req.destCity = "Jaipur";
        req.pickupAddress = "CP";
        req.destAddress = "MI Road";
        req.pickupLat = 28.6;
        req.pickupLng = 77.2;
        req.destLat = 26.9;
        req.destLng = 75.8;
        req.departureTime = Instant.now().plusSeconds(3600);
        req.vehicleCategory = "SEDAN";
        req.rideType = "OWNER";
        req.estimatedDistanceKm = new BigDecimal("100");
        req.fare = new BigDecimal("1200"); // >= 1200 min

        IntercityTripDto dto = service.createTrip(helperUser.getId(), req);
        assertNotNull(dto);
        assertEquals("OWNER", dto.rideType);
        assertEquals("1200", dto.fare.toString());
        assertEquals("1200", dto.customerFare.toString());
        assertEquals("1200", dto.driverPayout.toString());
        assertEquals("0", dto.agentCommission.stripTrailingZeros().toPlainString());
    }

    @Test
    void createTrip_AgentRideByNonAgentUser_ThrowsForbidden() {
        User regularUser = new User();
        regularUser.setId(UUID.randomUUID());
        regularUser.setRole(User.Role.DRIVER);
        regularUser.setStatus(User.Status.ACTIVE);
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        CreateIntercityTripRequest req = new CreateIntercityTripRequest();
        req.pickupCity = "Delhi";
        req.destCity = "Jaipur";
        req.pickupAddress = "CP";
        req.destAddress = "MI Road";
        req.pickupLat = 28.6;
        req.pickupLng = 77.2;
        req.destLat = 26.9;
        req.destLng = 75.8;
        req.departureTime = Instant.now().plusSeconds(3600);
        req.vehicleCategory = "SEDAN";
        req.rideType = "AGENT";
        req.fare = new BigDecimal("4000");

        ApiException ex = assertThrows(ApiException.class, () -> service.createTrip(regularUser.getId(), req));
        assertEquals("NOT_TRAVEL_AGENT", ex.code);
    }

    @Test
    void applyForTrip_DriverWithActiveTrip_ThrowsConflict() {
        IntercityTrip trip = createSampleTrip();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(applicationRepository.existsByTripIdAndDriverId(trip.getId(), driver.getId())).thenReturn(false);
        when(tripRepository.countActiveTripsForDriver(driver.getId())).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class, () -> service.applyForTrip(driver.getId(), trip.getId(), null));
        assertEquals("DRIVER_HAS_ACTIVE_TRIP", ex.code);
    }

    @Test
    void selectDriver_DriverWithActiveTrip_ThrowsConflict() {
        IntercityTrip trip = createSampleTrip();
        trip.setStatus(IntercityTrip.Status.PUBLISHED);
        when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));

        IntercityTripApplication app = new IntercityTripApplication();
        app.setTrip(trip);
        app.setDriver(driver);
        when(applicationRepository.findByTripIdAndDriverId(trip.getId(), driver.getId())).thenReturn(Optional.of(app));
        when(tripRepository.countActiveTripsForDriver(driver.getId())).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class, () -> service.selectDriver(agent.getId(), trip.getId(), driver.getId()));
        assertEquals("DRIVER_ALREADY_BUSY", ex.code);
    }

    @Test
    void completeOwnerTrip_Success_NoCompletionOtpRequired() {
        IntercityTrip trip = createSampleTrip();
        trip.setRideType(IntercityTrip.RideType.OWNER);
        trip.setDriver(driver);
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        IntercityTripDto result = service.arriveAtDestination(driver.getId(), trip.getId());
        assertEquals("COMPLETED", result.status);
        assertEquals("PAID", result.paymentStatus);
        assertNotNull(result.completedAt);
    }

    @Test
    void agentSettlement_FullLifecycle_Success() {
        IntercityTrip trip = createSampleTrip();
        trip.setRideType(IntercityTrip.RideType.AGENT);
        trip.setDriver(driver);
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        trip.setCompletionOtp("654321");
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        // 1. Arrive at destination -> AWAITING_AGENT_SETTLEMENT
        IntercityTripDto arrivedDto = service.arriveAtDestination(driver.getId(), trip.getId());
        assertEquals("AWAITING_AGENT_SETTLEMENT", arrivedDto.status);

        // 2. Driver submits settlement
        SubmitSettlementRequest subReq = new SubmitSettlementRequest();
        subReq.settlementRef = "UPI123456789";
        subReq.settlementNotes = "Paid ₹1000 commission";
        IntercityTripDto subDto = service.submitSettlement(driver.getId(), trip.getId(), subReq);
        assertEquals("SETTLEMENT_SUBMITTED", subDto.status);
        assertEquals("UPI123456789", subDto.settlementRef);

        // 3. Agent confirms settlement
        IntercityTripDto confDto = service.confirmSettlement(agent.getId(), trip.getId());
        assertEquals("SETTLEMENT_CONFIRMED", confDto.status);

        // 4. Driver enters completion OTP
        IntercityTripDto compDto = service.verifyCompletionOtp(driver.getId(), trip.getId(), "654321");
        assertEquals("COMPLETED", compDto.status);
        assertNotNull(compDto.completedAt);
    }

    @Test
    void dispute_RaiseAndResolve_Success() {
        IntercityTrip trip = createSampleTrip();
        trip.setRideType(IntercityTrip.RideType.OWNER);
        trip.setDriver(driver);
        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        // 1. Driver raises dispute
        RaiseDisputeRequest dReq = new RaiseDisputeRequest();
        dReq.reason = "Vehicle breakdown on highway";
        dReq.evidence = "Flat tyre photo";
        IntercityTripDto dispDto = service.raiseDispute(driver.getId(), trip.getId(), dReq);
        assertEquals("DISPUTED", dispDto.status);
        assertEquals("Vehicle breakdown on highway", dispDto.disputeReason);

        // 2. Resolve dispute
        ResolveDisputeRequest rReq = new ResolveDisputeRequest();
        rReq.resolution = "Driver completed journey with alternate car";
        rReq.action = "RESOLVE_AND_COMPLETE";
        IntercityTripDto resDto = service.resolveDispute(agent.getId(), trip.getId(), rReq);
        assertEquals("COMPLETED", resDto.status);
        assertEquals("Driver completed journey with alternate car", resDto.disputeResolution);
    }

    private IntercityTrip createSampleTrip() {
        IntercityTrip trip = new IntercityTrip();
        trip.setId(UUID.randomUUID());
        trip.setAgent(agent);
        trip.setPickupCity("Delhi");
        trip.setDestCity("Agra");
        trip.setPickupAddress("Taj Palace, New Delhi");
        trip.setDestAddress("Taj East Gate, Agra");
        trip.setPickupLat(28.6000);
        trip.setPickupLng(77.2000);
        trip.setDestLat(27.1751);
        trip.setDestLng(78.0421);
        trip.setDepartureTime(Instant.now().plusSeconds(3600));
        trip.setTripType(IntercityTrip.TripType.ONE_WAY);
        trip.setVehicleCategory("SUV");
        trip.setFare(new BigDecimal("3500.00"));
        trip.setPaymentMode("DIRECT_CASH");
        trip.setPaymentStatus(IntercityTrip.PaymentStatus.PENDING);
        trip.setStatus(IntercityTrip.Status.PUBLISHED);
        return trip;
    }
}
