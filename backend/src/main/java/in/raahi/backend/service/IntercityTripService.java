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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IntercityTripService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IntercityTripService.class);

    private final IntercityTripRepository tripRepository;
    private final IntercityTripApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private in.raahi.backend.websocket.WebSocketSessionRegistry webSocketSessionRegistry;

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

    public static BigDecimal getRecommendedRatePerKm(String vehicleCategory) {
        if (vehicleCategory == null) return new BigDecimal("15");
        return switch (vehicleCategory.trim().toUpperCase()) {
            case "SUV" -> new BigDecimal("20");
            case "LUXURY" -> new BigDecimal("30");
            case "TRAVELLER" -> new BigDecimal("25");
            case "HATCHBACK", "SEDAN" -> new BigDecimal("15");
            default -> new BigDecimal("15");
        };
    }

    public static BigDecimal getMinimumRatePerKm(String vehicleCategory, BigDecimal distanceKm) {
        if (vehicleCategory == null) return new BigDecimal("12");
        String cat = vehicleCategory.trim().toUpperCase();
        boolean shortTrip = distanceKm != null && distanceKm.compareTo(new BigDecimal("50")) <= 0;
        return switch (cat) {
            case "SUV" -> shortTrip ? new BigDecimal("20") : new BigDecimal("18");
            case "LUXURY" -> shortTrip ? new BigDecimal("30") : new BigDecimal("25");
            case "TRAVELLER" -> shortTrip ? new BigDecimal("25") : new BigDecimal("22");
            case "HATCHBACK", "SEDAN" -> shortTrip ? new BigDecimal("15") : new BigDecimal("12");
            default -> shortTrip ? new BigDecimal("15") : new BigDecimal("12");
        };
    }

    public static BigDecimal calculateOwnerSuggestedFare(BigDecimal distanceKm, String vehicleCategory) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = getRecommendedRatePerKm(vehicleCategory);
        return distanceKm.multiply(rate).setScale(0, RoundingMode.CEILING);
    }

    public static BigDecimal calculateOwnerMinimumFare(BigDecimal distanceKm, String vehicleCategory) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = getMinimumRatePerKm(vehicleCategory, distanceKm);
        return distanceKm.multiply(rate).setScale(0, RoundingMode.CEILING);
    }

    public static BigDecimal calculateOwnerSuggestedFare(BigDecimal distanceKm) {
        return calculateOwnerSuggestedFare(distanceKm, "SEDAN");
    }

    public static BigDecimal calculateOwnerMinimumFare(BigDecimal distanceKm) {
        return calculateOwnerMinimumFare(distanceKm, "SEDAN");
    }

    public boolean hasScheduleConflict(UUID driverId, Instant newStart, Integer newDurationMin, UUID excludeTripId) {
        if (driverId == null || newStart == null) return false;
        int duration1 = (newDurationMin != null && newDurationMin > 0) ? newDurationMin : 180;
        int bufferMin = 60; // 1-hour conflict buffer
        Instant newEnd = newStart.plus(duration1 + bufferMin, java.time.temporal.ChronoUnit.MINUTES);
        Instant effNewStart = newStart.minus(bufferMin, java.time.temporal.ChronoUnit.MINUTES);

        List<IntercityTrip> scheduledTrips = tripRepository.findScheduledTripsForDriver(driverId);
        for (IntercityTrip t : scheduledTrips) {
            if (excludeTripId != null && t.getId().equals(excludeTripId)) {
                continue;
            }
            Instant existStart = t.getDepartureTime();
            int duration2 = (t.getEstimatedDurationMin() != null && t.getEstimatedDurationMin() > 0) ? t.getEstimatedDurationMin() : 180;
            Instant existEnd = existStart.plus(duration2 + bufferMin, java.time.temporal.ChronoUnit.MINUTES);
            Instant effExistStart = existStart.minus(bufferMin, java.time.temporal.ChronoUnit.MINUTES);

            if (effNewStart.isBefore(existEnd) && effExistStart.isBefore(newEnd)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public IntercityTripDto createTrip(UUID agentId, CreateIntercityTripRequest req) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "User not found"));

        if (agent.getStatus() == User.Status.SUSPENDED) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", "Account is suspended");
        }

        if (req.departureTime == null || req.departureTime.isBefore(Instant.now().minusSeconds(120))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEPARTURE_TIME", "Departure time cannot be in the past");
        }

        if ("ROUND_TRIP".equalsIgnoreCase(req.tripType)) {
            if (req.returnTime == null || req.returnTime.isBefore(req.departureTime)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RETURN_TIME", "Return time must be after departure time for round trips");
            }
        }

        IntercityTrip.RideType rideType;
        if (req.rideType != null && !req.rideType.isBlank()) {
            if ("AGENT".equalsIgnoreCase(req.rideType.trim())) {
                rideType = IntercityTrip.RideType.AGENT;
            } else {
                rideType = IntercityTrip.RideType.OWNER;
            }
        } else {
            rideType = (agent.getRole() == User.Role.TRAVEL_AGENT) ? IntercityTrip.RideType.AGENT : IntercityTrip.RideType.OWNER;
        }

        IntercityTrip trip = new IntercityTrip();
        trip.setAgent(agent);
        trip.setRideType(rideType);
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

        if (rideType == IntercityTrip.RideType.AGENT) {
            if (agent.getRole() != User.Role.TRAVEL_AGENT) {
                throw ApiException.forbidden("NOT_TRAVEL_AGENT", "Only authorized travel agents can publish travel agent rides");
            }
            BigDecimal custFare = req.customerFare != null ? req.customerFare : req.fare;
            if (custFare == null || custFare.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FARE", "Customer fare must be greater than 0");
            }
            BigDecimal comm = req.agentCommission != null ? req.agentCommission : BigDecimal.ZERO;
            if (comm.compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COMMISSION", "Agent commission cannot be negative");
            }
            if (comm.compareTo(custFare) >= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COMMISSION", "Agent commission must be strictly less than customer fare");
            }
            BigDecimal drvPayout = custFare.subtract(comm);

            trip.setFare(custFare);
            trip.setCustomerFare(custFare);
            trip.setDriverPayout(drvPayout);
            trip.setAgentCommission(comm);
            trip.setPaymentRecipient("DRIVER");
            trip.setSettlementResponsibility("DRIVER_TO_AGENT");
        } else {
            // OWNER ride
            if (req.estimatedDistanceKm == null || req.estimatedDistanceKm.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DISTANCE", "Estimated road distance is required for owner rides");
            }
            BigDecimal minFare = calculateOwnerMinimumFare(req.estimatedDistanceKm, req.vehicleCategory);
            if (req.fare == null || req.fare.compareTo(minFare) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "FARE_BELOW_MINIMUM",
                        "Fare ₹" + req.fare + " is below minimum allowed fare of ₹" + minFare + " for " + req.vehicleCategory);
            }
            trip.setFare(req.fare);
            trip.setCustomerFare(req.fare);
            trip.setDriverPayout(req.fare);
            trip.setAgentCommission(BigDecimal.ZERO);
            trip.setPaymentRecipient("DRIVER");
            trip.setSettlementResponsibility("NONE");
        }

        trip.setPaymentMode(req.paymentMode != null ? req.paymentMode : "DIRECT_CASH");
        trip.setPaymentStatus(IntercityTrip.PaymentStatus.PENDING);
        trip.setCancellationTerms(req.cancellationTerms);
        trip.setSpecialInstructions(req.specialInstructions);
        trip.setStatus(Boolean.TRUE.equals(req.saveAsDraft) ? IntercityTrip.Status.DRAFT : IntercityTrip.Status.PUBLISHED);

        IntercityTrip saved = tripRepository.save(trip);
        if (saved.getStatus() == IntercityTrip.Status.PUBLISHED) {
            notifyOnlineDrivers(saved);
        }
        return toDto(saved, agentId);
    }

    @Transactional(readOnly = true)
    public List<IntercityTripDto> searchTrips(UUID viewerId, String pickupCity, String destCity, String vehicleCategory, Instant fromTime) {
        Instant effectiveFrom = fromTime != null ? fromTime : Instant.now().minusSeconds(1800);
        String category = (vehicleCategory != null && !vehicleCategory.isBlank() && !"ALL".equalsIgnoreCase(vehicleCategory.trim()))
                ? vehicleCategory.trim() : null;
        List<IntercityTrip> trips = tripRepository.searchTrips(
                (pickupCity != null && !pickupCity.isBlank()) ? pickupCity.trim() : null,
                (destCity != null && !destCity.isBlank()) ? destCity.trim() : null,
                category,
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
        List<IntercityTrip> ongoingForDriver = tripRepository.findOngoingTripsForDriver(userId);
        if (!ongoingForDriver.isEmpty()) {
            return toDto(ongoingForDriver.get(0), userId);
        }
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

        if (hasScheduleConflict(driverId, trip.getDepartureTime(), trip.getEstimatedDurationMin(), trip.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "SCHEDULE_CONFLICT", "You have a conflicting booking during this trip's time window.");
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

        User driver = userRepository.findByIdForUpdate(selectedDriverId)
                .orElseGet(() -> userRepository.findById(selectedDriverId).orElse(selectedApp.getDriver()));
        if (driver == null || driver.getStatus() == User.Status.SUSPENDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DRIVER_SUSPENDED", "Selected driver is not eligible");
        }

        if (hasScheduleConflict(selectedDriverId, trip.getDepartureTime(), trip.getEstimatedDurationMin(), trip.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "DRIVER_SCHEDULE_CONFLICT", "Selected driver has a conflicting booking during this trip schedule.");
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

        List<IntercityTrip> ongoing = tripRepository.findOngoingTripsForDriver(driverId);
        boolean otherOngoing = ongoing.stream().anyMatch(t -> !t.getId().equals(tripId));
        if (otherOngoing) {
            throw new ApiException(HttpStatus.CONFLICT, "DRIVER_ALREADY_BUSY", "You have another active trip currently in progress. Complete it first.");
        }

        trip.setStatus(IntercityTrip.Status.ARRIVED);
        trip.setArrivedAt(Instant.now());
        IntercityTrip saved = tripRepository.save(trip);

        notificationService.send(trip.getAgent(), "Driver Arrived", "Your driver has arrived at the pickup location.", null);

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

        // Completion OTP is NOT generated at pickup. It is generated only after agent confirms commission payment.
        trip.setCompletionOtp(null);
        trip.setCompletionOtpAttempts(0);

        trip.setStatus(IntercityTrip.Status.IN_PROGRESS);
        trip.setStartedAt(Instant.now());
        IntercityTrip saved = tripRepository.save(trip);

        notificationService.send(trip.getAgent(), "Trip Started", "Trip from " + trip.getPickupCity() + " to " + trip.getDestCity() + " has officially started.", null);

        return toDto(saved, driverId);
    }

    @Transactional
    public IntercityTripDto arriveAtDestination(UUID driverId, UUID tripId) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("NOT_ASSIGNED_DRIVER", "Only the assigned driver can update destination arrival");
        }

        if (trip.getStatus() != IntercityTrip.Status.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "Trip must be IN_PROGRESS to arrive at destination");
        }

        if (trip.getRideType() == IntercityTrip.RideType.OWNER) {
            return completeOwnerTrip(driverId, tripId);
        } else {
            trip.setStatus(IntercityTrip.Status.AWAITING_AGENT_SETTLEMENT);
            IntercityTrip saved = tripRepository.save(trip);
            notificationService.send(trip.getAgent(), "Driver Arrived at Destination", "Trip from " + trip.getPickupCity() + " to " + trip.getDestCity() + " reached destination. Awaiting settlement.", null);
            return toDto(saved, driverId);
        }
    }

    @Transactional
    public IntercityTripDto completeOwnerTrip(UUID driverId, UUID tripId) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("NOT_ASSIGNED_DRIVER", "Only the assigned driver can complete this trip");
        }

        if (trip.getRideType() != IntercityTrip.RideType.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_OWNER_RIDE", "This completion action is only for owner rides");
        }

        if (trip.getStatus() != IntercityTrip.Status.IN_PROGRESS && trip.getStatus() != IntercityTrip.Status.ARRIVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "Trip must be IN_PROGRESS to complete");
        }

        trip.setStatus(IntercityTrip.Status.COMPLETED);
        trip.setCompletedAt(Instant.now());
        trip.setPaymentStatus(IntercityTrip.PaymentStatus.PAID);
        IntercityTrip saved = tripRepository.save(trip);

        User driver = trip.getDriver();
        driver.setTotalHelps(driver.getTotalHelps() + 1);
        userRepository.save(driver);

        notificationService.send(trip.getAgent(), "Trip Completed", "Your trip with driver " + driver.getName() + " has arrived at " + trip.getDestCity() + " and is completed.", null);

        return toDto(saved, driverId);
    }

    @Transactional
    public IntercityTripDto submitSettlement(UUID driverId, UUID tripId, SubmitSettlementRequest req) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("NOT_ASSIGNED_DRIVER", "Only the assigned driver can submit settlement");
        }

        if (trip.getRideType() != IntercityTrip.RideType.AGENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_AGENT_RIDE", "Settlement submission is only required for agent rides");
        }

        if (trip.getStatus() != IntercityTrip.Status.AWAITING_AGENT_SETTLEMENT && trip.getStatus() != IntercityTrip.Status.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "Cannot submit settlement from status: " + trip.getStatus());
        }

        trip.setStatus(IntercityTrip.Status.SETTLEMENT_SUBMITTED);
        trip.setSettlementRef(req.settlementRef.trim());
        trip.setSettlementNotes(req.settlementNotes != null ? req.settlementNotes.trim() : null);
        trip.setSettlementSubmittedAt(Instant.now());

        IntercityTrip saved = tripRepository.save(trip);

        notificationService.send(trip.getAgent(), "Commission Settlement Submitted", "Driver submitted payment reference: " + req.settlementRef + ". Please verify and confirm.", null);

        return toDto(saved, driverId);
    }

    @Transactional
    public IntercityTripDto confirmSettlement(UUID agentId, UUID tripId) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (!trip.getAgent().getId().equals(agentId)) {
            throw ApiException.forbidden("NOT_TRIP_OWNER", "Only the travel agent can confirm commission settlement");
        }

        if (trip.getStatus() != IntercityTrip.Status.SETTLEMENT_SUBMITTED && trip.getStatus() != IntercityTrip.Status.AWAITING_AGENT_SETTLEMENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "Cannot confirm settlement from status: " + trip.getStatus());
        }

        trip.setStatus(IntercityTrip.Status.SETTLEMENT_CONFIRMED);
        trip.setSettlementConfirmedAt(Instant.now());
        if (trip.getCompletionOtp() == null) {
            trip.setCompletionOtp(String.format("%06d", secureRandom.nextInt(1_000_000)));
            trip.setCompletionOtpAttempts(0);
        }

        IntercityTrip saved = tripRepository.save(trip);

        if (trip.getDriver() != null) {
            notificationService.send(trip.getDriver(), "Settlement Confirmed", "Agent confirmed commission settlement. Obtain the completion OTP from the agent to conclude the trip.", null);
        }

        return toDto(saved, agentId);
    }

    @Transactional
    public IntercityTripDto verifyCompletionOtp(UUID driverId, UUID tripId, String submittedOtp) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("NOT_ASSIGNED_DRIVER", "Only the assigned driver can complete this trip");
        }

        if (trip.getRideType() == IntercityTrip.RideType.AGENT) {
            if (trip.getStatus() != IntercityTrip.Status.SETTLEMENT_CONFIRMED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SETTLEMENT_NOT_CONFIRMED", "Agent commission settlement must be confirmed before entering completion OTP");
            }
        } else {
            if (trip.getStatus() != IntercityTrip.Status.IN_PROGRESS && trip.getStatus() != IntercityTrip.Status.ARRIVED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "Trip must be in progress to complete");
            }
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
        trip.setCompletionOtp(null); // Invalidate OTP to prevent replay attacks
        IntercityTrip saved = tripRepository.save(trip);

        // Update driver total helps / completed trips
        User driver = trip.getDriver();
        driver.setTotalHelps(driver.getTotalHelps() + 1);
        userRepository.save(driver);

        notificationService.send(trip.getAgent(), "Trip Completed", "Your intercity trip has arrived at " + trip.getDestCity() + " and is marked completed.", null);

        return toDto(saved, driverId);
    }

    @Transactional
    public IntercityTripDto raiseDispute(UUID userId, UUID tripId, RaiseDisputeRequest req) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        boolean isAgent = trip.getAgent().getId().equals(userId);
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getId().equals(userId);

        if (!isAgent && !isDriver) {
            throw ApiException.forbidden("NOT_AUTHORIZED", "Only the trip creator or assigned driver can raise a dispute");
        }

        if (trip.getStatus() == IntercityTrip.Status.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_DISPUTE_CANCELLED", "Cannot raise a dispute on a cancelled trip");
        }

        User user = userRepository.findById(userId).orElse(null);
        trip.setStatus(IntercityTrip.Status.DISPUTED);
        trip.setDisputedAt(Instant.now());
        trip.setDisputedBy(user);
        trip.setDisputeReason(req.reason.trim());
        trip.setDisputeEvidence(req.evidence != null ? req.evidence.trim() : null);

        IntercityTrip saved = tripRepository.save(trip);

        if (isAgent && trip.getDriver() != null) {
            notificationService.send(trip.getDriver(), "Trip Disputed", "A dispute was raised by the organizer: " + req.reason, null);
        } else if (isDriver) {
            notificationService.send(trip.getAgent(), "Trip Disputed", "A dispute was raised by the driver: " + req.reason, null);
        }

        return toDto(saved, userId);
    }

    @Transactional
    public IntercityTripDto resolveDispute(UUID userId, UUID tripId, ResolveDisputeRequest req) {
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Intercity trip not found"));

        if (trip.getStatus() != IntercityTrip.Status.DISPUTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRIP_NOT_DISPUTED", "Trip is not currently disputed");
        }

        User resolver = userRepository.findById(userId).orElse(null);
        trip.setDisputeResolution(req.resolution.trim());
        trip.setResolvedAt(Instant.now());
        trip.setResolvedBy(resolver);

        if ("RESOLVE_AND_CANCEL".equalsIgnoreCase(req.action)) {
            trip.setStatus(IntercityTrip.Status.CANCELLED);
            trip.setCancelledAt(Instant.now());
            trip.setCancelReason("Resolved: " + req.resolution);
        } else {
            trip.setStatus(IntercityTrip.Status.COMPLETED);
            trip.setCompletedAt(Instant.now());
            trip.setPaymentStatus(IntercityTrip.PaymentStatus.PAID);
        }

        IntercityTrip saved = tripRepository.save(trip);
        return toDto(saved, userId);
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
        dto.rideType = t.getRideType() != null ? t.getRideType().name() : "OWNER";
        dto.customerFare = t.getCustomerFare();
        dto.driverPayout = t.getDriverPayout();
        dto.agentCommission = t.getAgentCommission();
        dto.paymentRecipient = t.getPaymentRecipient();
        dto.settlementResponsibility = t.getSettlementResponsibility();
        dto.settlementRef = t.getSettlementRef();
        dto.settlementNotes = t.getSettlementNotes();
        dto.settlementSubmittedAt = t.getSettlementSubmittedAt();
        dto.settlementConfirmedAt = t.getSettlementConfirmedAt();

        dto.disputedAt = t.getDisputedAt();
        dto.disputedById = t.getDisputedBy() != null ? t.getDisputedBy().getId().toString() : null;
        dto.disputeReason = t.getDisputeReason();
        dto.disputeEvidence = t.getDisputeEvidence();
        dto.disputeResolution = t.getDisputeResolution();
        dto.resolvedAt = t.getResolvedAt();

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
            // AGENT sees completion OTP only after confirming commission settlement
            if (t.getRideType() == IntercityTrip.RideType.AGENT &&
                t.getStatus() == IntercityTrip.Status.SETTLEMENT_CONFIRMED) {
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

    @Transactional(readOnly = true)
    public MarketplaceStatusDto getMarketplaceStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "User not found"));
        return new MarketplaceStatusDto(user.isMarketplaceOnline(), user.getPreferredPickupCity());
    }

    @Transactional
    public MarketplaceStatusDto updateMarketplaceStatus(UUID userId, UpdateMarketplaceStatusRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "User not found"));
        user.setMarketplaceOnline(req != null && req.online);
        user.setPreferredPickupCity(req != null && req.pickupCity != null && !req.pickupCity.isBlank() ? req.pickupCity.trim() : null);
        userRepository.save(user);
        return new MarketplaceStatusDto(user.isMarketplaceOnline(), user.getPreferredPickupCity());
    }

    private void notifyOnlineDrivers(IntercityTrip trip) {
        try {
            List<User> onlineDrivers = userRepository.findOnlineMarketplaceDriversMatchingCity(
                    trip.getPickupCity(),
                    trip.getAgent().getId()
            );
            if (onlineDrivers == null || onlineDrivers.isEmpty()) {
                return;
            }

            String title = "New Intercity Trip: " + trip.getPickupCity() + " → " + trip.getDestCity();
            String body = "₹" + trip.getFare() + " • " + trip.getVehicleCategory() + " • Tap to view & apply";
            Map<String, String> data = new HashMap<>();
            data.put("type", "intercity_new_trip");
            data.put("tripId", trip.getId().toString());
            data.put("pickupCity", trip.getPickupCity());
            data.put("destCity", trip.getDestCity());
            data.put("fare", String.valueOf(trip.getFare()));

            IntercityTripDto dto = toDto(trip, trip.getAgent().getId());

            for (User driver : onlineDrivers) {
                try {
                    if (notificationService != null) {
                        notificationService.send(driver, title, body, data);
                    }
                    if (webSocketSessionRegistry != null) {
                        webSocketSessionRegistry.sendToUser(driver.getId(), "intercity_new_trip", dto);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to notify driver {} about new trip {}: {}", driver.getId(), trip.getId(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch notifications for trip {}: {}", trip.getId(), e.getMessage());
        }
    }
}
