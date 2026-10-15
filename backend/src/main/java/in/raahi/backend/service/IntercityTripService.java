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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IntercityTripService {

    private final IntercityTripRepository tripRepository;
    private final IntercityTripApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public IntercityTripService(
            IntercityTripRepository tripRepository,
            IntercityTripApplicationRepository applicationRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.tripRepository = tripRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public IntercityTripDto createTrip(UUID agentId, CreateIntercityTripRequest req) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "User not found"));

        if (agent.getStatus() == User.Status.SUSPENDED) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", "Account is suspended");
        }

        // Ensure user has travel agent permissions
        if (agent.getRole() != User.Role.TRAVEL_AGENT && agent.getRole() != User.Role.ADMIN) {
            agent.setRole(User.Role.TRAVEL_AGENT);
            userRepository.save(agent);
        }

        if (req.departureTime.isBefore(Instant.now().minusSeconds(300))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEPARTURE_TIME", "Departure time cannot be in the past");
        }

        if ("ROUND_TRIP".equalsIgnoreCase(req.tripType)) {
            if (req.returnTime == null || req.returnTime.isBefore(req.departureTime)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RETURN_TIME", "Return time must be after departure time for round trips");
            }
        }

        IntercityTrip trip = new IntercityTrip();
        trip.setAgent(agent);
        trip.setPickupCity(req.pickupCity.trim());
        trip.setDestCity(req.destCity.trim());
        trip.setPickupAddress(req.pickupAddress.trim());
        trip.setDestAddress(req.destAddress.trim());
        trip.setPickupLat(req.pickupLat);
        trip.setPickupLng(req.pickupLng);
        trip.setDestLat(req.destLat);
        trip.setDestLng(req.destLng);
        trip.setDepartureTime(req.departureTime);
        trip.setTripType("ROUND_TRIP".equalsIgnoreCase(req.tripType) ? IntercityTrip.TripType.ROUND_TRIP : IntercityTrip.TripType.ONE_WAY);
        trip.setReturnTime(req.returnTime);
        trip.setVehicleCategory(req.vehicleCategory.trim().toUpperCase());
        trip.setEstimatedDistanceKm(req.estimatedDistanceKm);
        trip.setEstimatedDurationMin(req.estimatedDurationMin);
        trip.setFare(req.fare);
        trip.setPaymentMode(req.paymentMode != null ? req.paymentMode : "DIRECT_CASH");
        trip.setPaymentStatus(IntercityTrip.PaymentStatus.PENDING);
        trip.setCancellationTerms(req.cancellationTerms);
        trip.setSpecialInstructions(req.specialInstructions);
        trip.setStatus(Boolean.TRUE.equals(req.saveAsDraft) ? IntercityTrip.Status.DRAFT : IntercityTrip.Status.PUBLISHED);

        IntercityTrip saved = tripRepository.save(trip);
        return toDto(saved, agentId);
    }

    @Transactional(readOnly = true)
    public List<IntercityTripDto> searchTrips(UUID viewerId, String pickupCity, String destCity, String vehicleCategory, Instant fromTime) {
        Instant effectiveFrom = fromTime != null ? fromTime : Instant.now().minusSeconds(1800);
        List<IntercityTrip> trips = tripRepository.searchTrips(
                (pickupCity != null && !pickupCity.isBlank()) ? pickupCity.trim() : null,
                (destCity != null && !destCity.isBlank()) ? destCity.trim() : null,
                (vehicleCategory != null && !vehicleCategory.isBlank()) ? vehicleCategory.trim() : null,
                effectiveFrom
        );
        return trips.stream().map(t -> toDto(t, viewerId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IntercityTripDto getTripDetails(UUID viewerId, UUID tripId) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));
        return toDto(trip, viewerId);
    }

    @Transactional(readOnly = true)
    public List<IntercityTripDto> getAgentTrips(UUID agentId) {
        return tripRepository.findByAgentIdOrderByCreatedAtDesc(agentId).stream()
                .map(t -> toDto(t, agentId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IntercityTripDto> getDriverTrips(UUID driverId) {
        // Driver's assigned trips + trips driver applied to
        List<IntercityTrip> assigned = tripRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
        List<IntercityTripApplication> apps = applicationRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
        Set<UUID> seenIds = new HashSet<>();
        List<IntercityTripDto> result = new ArrayList<>();

        for (IntercityTrip t : assigned) {
            seenIds.add(t.getId());
            result.add(toDto(t, driverId));
        }
        for (IntercityTripApplication app : apps) {
            if (!seenIds.contains(app.getTrip().getId())) {
                seenIds.add(app.getTrip().getId());
                result.add(toDto(app.getTrip(), driverId));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public IntercityTripDto getActiveTrip(UUID userId) {
        List<IntercityTrip> activeForDriver = tripRepository.findActiveTripsForDriver(userId);
        if (!activeForDriver.isEmpty()) {
            return toDto(activeForDriver.get(0), userId);
        }
        List<IntercityTrip> activeForAgent = tripRepository.findActiveTripsForAgent(userId);
        if (!activeForAgent.isEmpty()) {
            return toDto(activeForAgent.get(0), userId);
        }
        return null;
    }

    @Transactional
    public IntercityTripApplicationDto applyForTrip(UUID driverId, UUID tripId, ApplyTripRequest req) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Driver not found"));

        if (driver.getStatus() == User.Status.SUSPENDED) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", "Account is suspended");
        }

        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getAgent().getId().equals(driverId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_APPLY_TO_OWN_TRIP", "You cannot apply to a trip you created");
        }

        if (trip.getStatus() != IntercityTrip.Status.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "TRIP_NOT_AVAILABLE", "Trip is no longer accepting applications");
        }

        if (applicationRepository.existsByTripIdAndDriverId(tripId, driverId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_APPLIED", "You have already applied for this trip");
        }

        IntercityTripApplication app = new IntercityTripApplication();
        app.setTrip(trip);
        app.setDriver(driver);
        app.setStatus(ApplicationStatus.APPLIED);
        app.setDriverNotes(req != null ? req.driverNotes : null);

        IntercityTripApplication saved = applicationRepository.save(app);

        // Notify Agent
        String driverName = driver.getName() != null ? driver.getName() : "A driver";
        notificationService.send(trip.getAgent(), "New Driver Application", driverName + " applied for your " + trip.getPickupCity() + " to " + trip.getDestCity() + " trip.", null);

        return toApplicationDto(saved);
    }

    @Transactional(readOnly = true)
    public List<IntercityTripApplicationDto> getTripApplications(UUID agentId, UUID tripId) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (!trip.getAgent().getId().equals(agentId)) {
            throw ApiException.forbidden("NOT_TRIP_OWNER", "Only the trip owner can view applications");
        }

        return applicationRepository.findByTripIdOrderByCreatedAtAsc(tripId).stream()
                .map(this::toApplicationDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public IntercityTripDto selectDriver(UUID agentId, UUID tripId, UUID selectedDriverId) {
        IntercityTrip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (!trip.getAgent().getId().equals(agentId)) {
            throw ApiException.forbidden("NOT_TRIP_OWNER", "Only the trip owner can select a driver");
        }

        if (trip.getStatus() != IntercityTrip.Status.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "TRIP_ALREADY_ASSIGNED", "This trip has already been assigned or closed");
        }

        IntercityTripApplication selectedApp = applicationRepository.findByTripIdAndDriverId(tripId, selectedDriverId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Driver application not found for this trip"));

        User driver = selectedApp.getDriver();
        if (driver.getStatus() == User.Status.SUSPENDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DRIVER_SUSPENDED", "Selected driver is not eligible");
        }

        // Generate 6-digit pickup OTP
        String pickupOtp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // Atomic assignment
        trip.setDriver(driver);
        trip.setStatus(IntercityTrip.Status.ASSIGNED);
        trip.setAssignedAt(Instant.now());
        trip.setPickupOtp(pickupOtp);
        trip.setPickupOtpAttempts(0);
        tripRepository.save(trip);

        // Accept selected application and reject others
        List<IntercityTripApplication> allApps = applicationRepository.findByTripIdOrderByCreatedAtAsc(tripId);
        for (IntercityTripApplication app : allApps) {
            if (app.getDriver().getId().equals(selectedDriverId)) {
                app.setStatus(ApplicationStatus.ACCEPTED);
            } else {
                app.setStatus(ApplicationStatus.REJECTED);
            }
            applicationRepository.save(app);
        }

        // Notify selected driver
        notificationService.send(driver, "Trip Confirmed!", "You have been selected for the trip from " + trip.getPickupCity() + " to " + trip.getDestCity() + " (₹" + trip.getFare() + ").", null);

        return toDto(trip, agentId);
    }

    @Transactional
    public IntercityTripDto arriveAtPickup(UUID driverId, UUID tripId) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("NOT_ASSIGNED_DRIVER", "Only the assigned driver can update this trip");
        }

        if (trip.getStatus() != IntercityTrip.Status.ASSIGNED && trip.getStatus() != IntercityTrip.Status.EN_ROUTE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "Trip cannot transition to ARRIVED from " + trip.getStatus());
        }

        trip.setStatus(IntercityTrip.Status.ARRIVED);
        trip.setArrivedAt(Instant.now());
        IntercityTrip saved = tripRepository.save(trip);

        notificationService.send(trip.getAgent(), "Driver Arrived", "Your driver has arrived at the pickup location."
        , null);

        return toDto(saved, driverId);
    }

    @Transactional
    public IntercityTripDto verifyPickupOtp(UUID driverId, UUID tripId, String submittedOtp) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("NOT_ASSIGNED_DRIVER", "Only the assigned driver can verify pickup OTP");
        }

        if (trip.getStatus() != IntercityTrip.Status.ARRIVED && trip.getStatus() != IntercityTrip.Status.ASSIGNED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "Cannot start trip from status: " + trip.getStatus());
        }

        if (trip.getPickupOtpAttempts() >= 5) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MAX_OTP_ATTEMPTS_EXCEEDED", "Too many invalid OTP attempts. Contact agent.");
        }

        if (trip.getPickupOtp() == null || !trip.getPickupOtp().equals(submittedOtp.trim())) {
            trip.setPickupOtpAttempts(trip.getPickupOtpAttempts() + 1);
            tripRepository.save(trip);
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid pickup OTP. Please verify with agent.");
        }

        // Generate completion OTP for destination verification
        String completionOtp = String.format("%06d", secureRandom.nextInt(1_000_000));

        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        trip.setStartedAt(Instant.now());
        trip.setCompletionOtp(completionOtp);
        trip.setCompletionOtpAttempts(0);
        IntercityTrip saved = tripRepository.save(trip);

        notificationService.send(trip.getAgent(), "Trip Started", "Trip from " + trip.getPickupCity() + " to " + trip.getDestCity() + " has officially started.", null);

        return toDto(saved, driverId);
    }

    @Transactional
    public IntercityTripDto verifyCompletionOtp(UUID driverId, UUID tripId, String submittedOtp) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("NOT_ASSIGNED_DRIVER", "Only the assigned driver can complete this trip");
        }

        if (trip.getStatus() != IntercityTrip.Status.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "Trip is not IN_PROGRESS");
        }

        if (trip.getCompletionOtpAttempts() >= 5) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MAX_OTP_ATTEMPTS_EXCEEDED", "Too many invalid OTP attempts. Contact agent.");
        }

        if (trip.getCompletionOtp() == null || !trip.getCompletionOtp().equals(submittedOtp.trim())) {
            trip.setCompletionOtpAttempts(trip.getCompletionOtpAttempts() + 1);
            tripRepository.save(trip);
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid completion OTP. Please verify with agent.");
        }

        trip.setStatus(IntercityTrip.Status.COMPLETED);
        trip.setCompletedAt(Instant.now());
        trip.setPaymentStatus(IntercityTrip.PaymentStatus.PAID);
        IntercityTrip saved = tripRepository.save(trip);

        // Update driver total helps / completed trips
        User driver = trip.getDriver();
        driver.setTotalHelps(driver.getTotalHelps() + 1);
        userRepository.save(driver);

        notificationService.send(trip.getAgent(), "Trip Completed", "Your intercity trip has arrived at " + trip.getDestCity() + " and is marked completed.", null);

        return toDto(saved, driverId);
    }

    @Transactional
    public IntercityTripDto cancelTrip(UUID userId, UUID tripId, String reason) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        boolean isAgent = trip.getAgent().getId().equals(userId);
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getId().equals(userId);

        if (!isAgent && !isDriver) {
            throw ApiException.forbidden("NOT_AUTHORIZED", "Only the agent or assigned driver can cancel this trip");
        }

        if (trip.getStatus() == IntercityTrip.Status.IN_PROGRESS || trip.getStatus() == IntercityTrip.Status.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_CANCEL_ACTIVE_TRIP", "Trip cannot be cancelled once in progress or completed");
        }

        User cancellingUser = userRepository.findById(userId).orElse(null);
        trip.setStatus(IntercityTrip.Status.CANCELLED);
        trip.setCancelledAt(Instant.now());
        trip.setCancelledBy(cancellingUser);
        trip.setCancelReason(reason);

        IntercityTrip saved = tripRepository.save(trip);

        // Notify the counterparty
        if (isAgent && trip.getDriver() != null) {
            notificationService.send(trip.getDriver(), "Trip Cancelled", "The trip to " + trip.getDestCity() + " was cancelled by the travel agent.", null);
        } else if (isDriver) {
            notificationService.send(trip.getAgent(), "Driver Cancelled", "Driver cancelled their assignment for trip to " + trip.getDestCity() + ".", null);
        }

        return toDto(saved, userId);
    }

    @Transactional
    public IntercityTripDto settlePayment(UUID agentId, UUID tripId) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (!trip.getAgent().getId().equals(agentId)) {
            throw ApiException.forbidden("NOT_TRIP_OWNER", "Only the trip owner can settle payment");
        }

        if (trip.getStatus() != IntercityTrip.Status.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRIP_NOT_COMPLETED", "Only completed trips can be settled");
        }

        trip.setPaymentStatus(IntercityTrip.PaymentStatus.SETTLED);
        IntercityTrip saved = tripRepository.save(trip);
        return toDto(saved, agentId);
    }

    // Helper mapping methods
    private IntercityTripDto toDto(IntercityTrip t, UUID viewerId) {
        IntercityTripDto dto = new IntercityTripDto();
        dto.id = t.getId().toString();
        dto.agentId = t.getAgent().getId().toString();
        dto.agentName = t.getAgent().getName();
        dto.agentPhone = t.getAgent().getPhone();
        dto.agencyName = t.getAgent().getAgencyName();

        if (t.getDriver() != null) {
            dto.driverId = t.getDriver().getId().toString();
            dto.driverName = t.getDriver().getName();
            dto.driverPhone = t.getDriver().getPhone();
            dto.driverRatingAvg = t.getDriver().getRatingAvg();
            dto.driverVehicleType = t.getDriver().getVehicleType();
            dto.driverVehicleReg = t.getDriver().getVehicleReg();
        }

        dto.pickupCity = t.getPickupCity();
        dto.destCity = t.getDestCity();
        dto.pickupAddress = t.getPickupAddress();
        dto.destAddress = t.getDestAddress();
        dto.pickupLat = t.getPickupLat();
        dto.pickupLng = t.getPickupLng();
        dto.destLat = t.getDestLat();
        dto.destLng = t.getDestLng();

        dto.departureTime = t.getDepartureTime();
        dto.tripType = t.getTripType().name();
        dto.returnTime = t.getReturnTime();
        dto.vehicleCategory = t.getVehicleCategory();
        dto.estimatedDistanceKm = t.getEstimatedDistanceKm();
        dto.estimatedDurationMin = t.getEstimatedDurationMin();

        dto.fare = t.getFare();
        dto.paymentMode = t.getPaymentMode();
        dto.paymentStatus = t.getPaymentStatus().name();
        dto.cancellationTerms = t.getCancellationTerms();
        dto.specialInstructions = t.getSpecialInstructions();
        dto.status = t.getStatus().name();

        dto.assignedAt = t.getAssignedAt();
        dto.arrivedAt = t.getArrivedAt();
        dto.startedAt = t.getStartedAt();
        dto.completedAt = t.getCompletedAt();
        dto.cancelledAt = t.getCancelledAt();
        dto.cancelReason = t.getCancelReason();
        dto.createdAt = t.getCreatedAt();

        // Compute viewerRole
        boolean isAgent = t.getAgent().getId().equals(viewerId);
        boolean isAssignedDriver = t.getDriver() != null && t.getDriver().getId().equals(viewerId);

        if (isAgent) {
            dto.viewerRole = "AGENT";
            dto.totalApplications = applicationRepository.findByTripIdOrderByCreatedAtAsc(t.getId()).size();
            // AGENT sees pickup OTP once assigned so they can verify the driver in person
            if (t.getStatus() == IntercityTrip.Status.ASSIGNED || t.getStatus() == IntercityTrip.Status.ARRIVED) {
                dto.pickupOtp = t.getPickupOtp();
            }
            // AGENT sees completion OTP during active trip so they can verify destination arrival
            if (t.getStatus() == IntercityTrip.Status.IN_PROGRESS) {
                dto.completionOtp = t.getCompletionOtp();
            }
        } else if (isAssignedDriver) {
            dto.viewerRole = "ASSIGNED_DRIVER";
            // Security: DRIVER NEVER sees OTP in API response! Driver receives it verbally from agent/passenger.
            dto.pickupOtp = null;
            dto.completionOtp = null;
        } else {
            Optional<IntercityTripApplication> myApp = applicationRepository.findByTripIdAndDriverId(t.getId(), viewerId);
            if (myApp.isPresent()) {
                dto.viewerRole = "APPLICANT";
                dto.applicationStatus = myApp.get().getStatus().name();
            } else {
                dto.viewerRole = "VIEWER";
            }
            dto.pickupOtp = null;
            dto.completionOtp = null;
        }

        return dto;
    }

    private IntercityTripApplicationDto toApplicationDto(IntercityTripApplication app) {
        IntercityTripApplicationDto dto = new IntercityTripApplicationDto();
        dto.id = app.getId().toString();
        dto.tripId = app.getTrip().getId().toString();
        dto.driverId = app.getDriver().getId().toString();
        dto.driverName = app.getDriver().getName();
        dto.driverPhone = app.getDriver().getPhone();
        dto.driverRatingAvg = app.getDriver().getRatingAvg();
        dto.driverTotalHelps = app.getDriver().getTotalHelps();
        dto.driverVehicleType = app.getDriver().getVehicleType();
        dto.driverVehicleReg = app.getDriver().getVehicleReg();
        dto.driverVerified = app.getDriver().isVerified();
        dto.status = app.getStatus().name();
        dto.driverNotes = app.getDriverNotes();
        dto.createdAt = app.getCreatedAt();
        return dto;
    }
}
