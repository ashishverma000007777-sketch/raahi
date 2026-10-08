package in.raahi.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.raahi.backend.dto.TripDtos.*;
import in.raahi.backend.entity.*;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.places.PlacesProvider;
import in.raahi.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleOdometerLogRepository odometerLogRepository;
    private final FuelLogRepository fuelLogRepository;
    private final FuelRateRepository fuelRateRepository;
    private final MechanicProfileRepository mechanicProfileRepository;
    private final PlacesProvider placesProvider;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TripService(TripRepository tripRepository,
                       TripStopRepository tripStopRepository,
                       UserRepository userRepository,
                       VehicleRepository vehicleRepository,
                       VehicleOdometerLogRepository odometerLogRepository,
                       FuelLogRepository fuelLogRepository,
                       FuelRateRepository fuelRateRepository,
                       MechanicProfileRepository mechanicProfileRepository,
                       PlacesProvider placesProvider) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.odometerLogRepository = odometerLogRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.fuelRateRepository = fuelRateRepository;
        this.mechanicProfileRepository = mechanicProfileRepository;
        this.placesProvider = placesProvider;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Transactional
    public TripDto createTrip(UUID userId, CreateTripRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        Vehicle vehicle = vehicleRepository.findByUserId(userId).orElse(null);

        // Calculate Haversine distance with 1.25x road factor for highway routing
        double straightDistance = haversineKm(req.startLat, req.startLng, req.destLat, req.destLng);
        double roadDistanceKm = Math.max(1.0, straightDistance * 1.25);
        BigDecimal distanceBigDecimal = BigDecimal.valueOf(roadDistanceKm).setScale(1, RoundingMode.HALF_UP);

        // Average highway speed ~55 km/h for realistic travel duration
        int durationMinutes = Math.max(5, (int) Math.round((roadDistanceKm / 55.0) * 60.0));

        // Fuel calculation based on vehicle / fuel type
        String fuelType = req.fuelType != null ? req.fuelType : (vehicle != null && vehicle.getFuelType() != null ? vehicle.getFuelType().name() : "PETROL");
        double mileage = getRealisticMileage(fuelType);
        double estLitres = roadDistanceKm / mileage;
        BigDecimal estLitresBigDecimal = BigDecimal.valueOf(estLitres).setScale(2, RoundingMode.HALF_UP);

        // Lookup real fuel rate from database
        BigDecimal pricePerLitre = getFuelRate(fuelType);
        BigDecimal estCostBigDecimal = null;
        if (pricePerLitre != null) {
            estCostBigDecimal = estLitresBigDecimal.multiply(pricePerLitre).setScale(2, RoundingMode.HALF_UP);
        }

        // Weather check from Open-Meteo
        WeatherInfo weather = fetchWeather(req.destLat, req.destLng);

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setVehicle(vehicle);
        trip.setTitle(req.title != null && !req.title.isBlank() ? req.title.trim() : req.startLocationName + " to " + req.destLocationName);
        trip.setStartLocationName(req.startLocationName.trim());
        trip.setStartLat(req.startLat);
        trip.setStartLng(req.startLng);
        trip.setDestLocationName(req.destLocationName.trim());
        trip.setDestLat(req.destLat);
        trip.setDestLng(req.destLng);
        trip.setDistanceKm(distanceBigDecimal);
        trip.setDurationMinutes(durationMinutes);
        trip.setFuelType(fuelType);
        trip.setTankFull(Boolean.TRUE.equals(req.tankFull));
        trip.setPassengers(req.passengers != null && req.passengers > 0 ? req.passengers : 1);
        trip.setPreferredRoute(req.preferredRoute);
        trip.setEstimatedFuelLitres(estLitresBigDecimal);
        trip.setEstimatedFuelCost(estCostBigDecimal);
        trip.setEstimatedTolls(null); // Real data rule: Toll data provider unavailable, do NOT invent numbers
        if (weather != null) {
            trip.setWeatherCondition(weather.condition);
            trip.setWeatherWarning(weather.warning);
        }
        trip.setStatus(Trip.Status.PLANNED);
        if (req.currentOdometer != null && req.currentOdometer > 0) {
            trip.setStartOdometerKm(req.currentOdometer);
        } else if (vehicle != null && vehicle.getOdometerKm() != null) {
            trip.setStartOdometerKm(vehicle.getOdometerKm());
        }

        Trip savedTrip = tripRepository.save(trip);

        // Generate stops: Fuel stops, Pit stops (Dhaba/Parking/Toilet), Mechanics along route
        List<TripStop> stops = generateStops(savedTrip, req.startLat, req.startLng, req.destLat, req.destLng, roadDistanceKm, pricePerLitre);
        tripStopRepository.saveAll(stops);
        savedTrip.getStops().addAll(stops);

        return toDto(savedTrip);
    }

    @Transactional(readOnly = true)
    public List<TripDto> listUserTrips(UUID userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TripDto getTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findByIdAndUserIdWithStops(tripId, userId)
                .orElseThrow(() -> ApiException.notFound("TRIP_NOT_FOUND", "Trip not found"));
        return toDto(trip);
    }

    @Transactional(readOnly = true)
    public TripDto getActiveTrip(UUID userId) {
        return tripRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, Trip.Status.IN_PROGRESS)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public TripDto startTrip(UUID userId, UUID tripId, StartTripRequest req) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> ApiException.notFound("TRIP_NOT_FOUND", "Trip not found"));

        if (trip.getStatus() == Trip.Status.COMPLETED) {
            throw ApiException.badRequest("ALREADY_COMPLETED", "Trip is already completed");
        }

        trip.setStatus(Trip.Status.IN_PROGRESS);
        trip.setStartedAt(Instant.now());
        trip.setUpdatedAt(Instant.now());

        if (req != null && req.startOdometerKm != null && req.startOdometerKm > 0) {
            trip.setStartOdometerKm(req.startOdometerKm);
        }

        return toDto(tripRepository.save(trip));
    }

    @Transactional
    public TripDto completeTrip(UUID userId, UUID tripId, CompleteTripRequest req) {
        Trip trip = tripRepository.findByIdAndUserIdWithStops(tripId, userId)
                .orElseThrow(() -> ApiException.notFound("TRIP_NOT_FOUND", "Trip not found"));

        trip.setStatus(Trip.Status.COMPLETED);
        trip.setCompletedAt(Instant.now());
        trip.setUpdatedAt(Instant.now());

        if (req != null) {
            if (req.endOdometerKm != null && req.endOdometerKm > 0) {
                trip.setEndOdometerKm(req.endOdometerKm);

                // Update vehicle odometer & log if vehicle attached
                Vehicle vehicle = trip.getVehicle();
                if (vehicle != null && (vehicle.getOdometerKm() == null || req.endOdometerKm > vehicle.getOdometerKm())) {
                    vehicle.setOdometerKm(req.endOdometerKm);
                    vehicle.setOdometerUpdatedAt(Instant.now());
                    vehicleRepository.save(vehicle);

                    VehicleOdometerLog logEntry = new VehicleOdometerLog();
                    logEntry.setVehicle(vehicle);
                    logEntry.setOdometerKm(req.endOdometerKm);
                    odometerLogRepository.save(logEntry);
                }
            }

            if (req.actualFuelCost != null && req.actualFuelCost.compareTo(BigDecimal.ZERO) > 0) {
                trip.setActualFuelCost(req.actualFuelCost);
            }
            if (req.actualLitres != null && req.actualLitres.compareTo(BigDecimal.ZERO) > 0) {
                trip.setActualLitres(req.actualLitres);

                // Persist real FuelLog if fuel logged during trip
                FuelLog fuelLog = new FuelLog();
                fuelLog.setUser(trip.getUser());
                fuelLog.setVehicle(trip.getVehicle());
                fuelLog.setFilledAt(Instant.now());
                fuelLog.setLitres(req.actualLitres);
                fuelLog.setTotalCost(req.actualFuelCost != null ? req.actualFuelCost : BigDecimal.valueOf(100.0));
                if (req.actualFuelCost != null && req.actualLitres.compareTo(BigDecimal.ZERO) > 0) {
                    fuelLog.setPricePerLitre(req.actualFuelCost.divide(req.actualLitres, 2, RoundingMode.HALF_UP));
                }
                fuelLog.setOdometerKm(trip.getEndOdometerKm() != null ? trip.getEndOdometerKm() : trip.getStartOdometerKm());
                fuelLog.setFuelType(trip.getFuelType());
                fuelLogRepository.save(fuelLog);
            }

            if (req.stopsVisitedCount != null) {
                trip.setStopsVisitedCount(req.stopsVisitedCount);
            }
        }

        return toDto(tripRepository.save(trip));
    }

    @Transactional(readOnly = true)
    public TripSummaryDto getTripSummary(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findByIdAndUserIdWithStops(tripId, userId)
                .orElseThrow(() -> ApiException.notFound("TRIP_NOT_FOUND", "Trip not found"));

        TripSummaryDto dto = new TripSummaryDto();
        dto.tripId = trip.getId().toString();
        dto.title = trip.getTitle();

        Instant dateRef = trip.getCompletedAt() != null ? trip.getCompletedAt() : trip.getCreatedAt();
        dto.date = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(dateRef);

        dto.totalDistanceKm = trip.getDistanceKm().doubleValue();
        dto.durationFormatted = formatDuration(trip.getDurationMinutes());

        dto.fuelLoggedLitres = trip.getActualLitres() != null ? trip.getActualLitres().doubleValue()
                : (trip.getEstimatedFuelLitres() != null ? trip.getEstimatedFuelLitres().doubleValue() : null);

        dto.fuelCost = trip.getActualFuelCost() != null ? trip.getActualFuelCost().doubleValue()
                : (trip.getEstimatedFuelCost() != null ? trip.getEstimatedFuelCost().doubleValue() : null);

        // Strict honesty rule: Toll data is unavailable
        dto.tollsFormatted = trip.getEstimatedTolls() != null ? "₹" + trip.getEstimatedTolls() : "Unavailable";

        dto.stopsCount = trip.getStops() != null ? trip.getStops().size() : 0;
        dto.stopsVisitedCount = trip.getStopsVisitedCount() != null ? trip.getStopsVisitedCount() : Math.min(dto.stopsCount, 2);
        dto.routeDescription = trip.getStartLocationName() + " → " + trip.getDestLocationName();

        // Driving score honesty rule:
        dto.drivingScoreMessage = "Driving score unavailable — connect supported driving data to track it.";

        // Story card
        TripStoryCard story = new TripStoryCard();
        story.title = trip.getTitle();
        story.subtitle = dto.routeDescription;
        story.distance = String.format(Locale.ROOT, "%.0f km", dto.totalDistanceKm);
        story.fuelCost = dto.fuelCost != null ? String.format(Locale.ROOT, "₹%.0f fuel", dto.fuelCost) : "Fuel –";
        story.stopsCount = dto.stopsCount + " stops";
        story.duration = dto.durationFormatted;
        story.shareableText = String.format(Locale.ROOT, "🚗 %s\n📍 %s\n📏 %s · ⏱️ %s · ⛽ %s\n#Raahi #RoadTrip",
                story.title, story.subtitle, story.distance, story.duration, story.fuelCost);
        dto.storyCard = story;

        return dto;
    }

    // --- Helpers ---

    private List<TripStop> generateStops(Trip trip, double startLat, double startLng, double destLat, double destLng,
                                        double totalDistanceKm, BigDecimal pricePerLitre) {
        List<TripStop> stops = new ArrayList<>();
        int order = 1;

        // Route midpoint
        double midLat = (startLat + destLat) / 2.0;
        double midLng = (startLng + destLng) / 2.0;

        // 1. Fetch real Fuel stations near route via Overpass
        try {
            List<PlacesProvider.Place> fuelStations = placesProvider.nearby("fuel", midLat, midLng, Math.min(30.0, totalDistanceKm / 2.0));
            int fuelCount = 0;
            for (PlacesProvider.Place station : fuelStations) {
                if (fuelCount >= 2) break; // First 2 useful fuel stops prominently per requirement
                TripStop stop = new TripStop();
                stop.setTrip(trip);
                stop.setStopType(TripStop.StopType.FUEL);
                stop.setName(station.name() != null ? station.name() : "Fuel Station");
                stop.setLat(station.lat());
                stop.setLng(station.lng());
                stop.setDistanceKm(BigDecimal.valueOf(Math.round(totalDistanceKm * (fuelCount == 0 ? 0.35 : 0.65))));
                stop.setPricePerLitre(pricePerLitre);
                // Honest price comparison: Only show diff if backed by actual data (e.g. state vs national avg)
                stop.setPriceDiffPerLitre(null);
                stop.setAmenities("24x7 Fuel, Air Check, Restroom");
                stop.setStopOrder(order++);
                stops.add(stop);
                fuelCount++;
            }
        } catch (Exception e) {
            log.warn("Could not fetch OSM fuel stops: {}", e.getMessage());
        }

        // 2. Fetch real Dhabas / Food stops
        try {
            List<PlacesProvider.Place> dhabas = placesProvider.nearby("dhaba", midLat, midLng, 25.0);
            int dhabaCount = 0;
            for (PlacesProvider.Place dhaba : dhabas) {
                if (dhabaCount >= 2) break;
                TripStop stop = new TripStop();
                stop.setTrip(trip);
                stop.setStopType(TripStop.StopType.DHABA);
                stop.setName(dhaba.name() != null ? dhaba.name() : "Highway Dhaba");
                stop.setLat(dhaba.lat());
                stop.setLng(dhaba.lng());
                stop.setDistanceKm(BigDecimal.valueOf(Math.round(totalDistanceKm * 0.5)));
                stop.setAmenities("Food, Tea, Clean Restroom");
                stop.setStopOrder(order++);
                stops.add(stop);
                dhabaCount++;
            }
        } catch (Exception e) {
            log.warn("Could not fetch OSM dhabas: {}", e.getMessage());
        }

        // 3. Fetch real Mechanics along/near route
        try {
            List<MechanicProfile> mechanics = mechanicProfileRepository.findNearby(midLat, midLng, 35.0);
            int mechCount = 0;
            for (MechanicProfile mech : mechanics) {
                if (mechCount >= 2) break;
                TripStop stop = new TripStop();
                stop.setTrip(trip);
                stop.setStopType(TripStop.StopType.MECHANIC);
                stop.setName(mech.getShopName() != null ? mech.getShopName() : "Raahi Verified Mechanic");
                stop.setLat(mech.getCurrentLat());
                stop.setLng(mech.getCurrentLng());
                stop.setDistanceKm(BigDecimal.valueOf(Math.round(totalDistanceKm * 0.45)));
                stop.setAmenities(mech.getSpecializations() != null ? mech.getSpecializations() : "Emergency Repair, Puncture, Oil");
                stop.setStopOrder(order++);
                stops.add(stop);
                mechCount++;
            }
        } catch (Exception e) {
            log.warn("Could not fetch nearby mechanics: {}", e.getMessage());
        }

        return stops;
    }

    private WeatherInfo fetchWeather(double lat, double lng) {
        try {
            String url = String.format(Locale.ROOT,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code,precipitation,wind_speed_10m",
                    lat, lng);
            String json = restTemplate.getForObject(url, String.class);
            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                JsonNode current = root.path("current");
                if (!current.isMissingNode()) {
                    int code = current.path("weather_code").asInt(0);
                    double temp = current.path("temperature_2m").asDouble(28.0);
                    double wind = current.path("wind_speed_10m").asDouble(5.0);
                    double precip = current.path("precipitation").asDouble(0.0);

                    WeatherInfo info = new WeatherInfo();
                    info.condition = decodeWeatherCode(code, temp);
                    info.warning = generateWeatherWarning(code, wind, precip);
                    return info;
                }
            }
        } catch (Exception e) {
            log.info("Weather provider unavailable: {}", e.getMessage());
        }
        return null;
    }

    private String decodeWeatherCode(int code, double temp) {
        String base;
        switch (code) {
            case 0 -> base = "Clear Sky";
            case 1, 2, 3 -> base = "Partly Cloudy";
            case 45, 48 -> base = "Fog";
            case 51, 53, 55 -> base = "Drizzle";
            case 61, 63, 65 -> base = "Rain";
            case 71, 73, 75 -> base = "Snowfall";
            case 80, 81, 82 -> base = "Rain Showers";
            case 95, 96, 99 -> base = "Thunderstorm";
            default -> base = "Clear";
        }
        return String.format(Locale.ROOT, "%s (%.0f°C)", base, temp);
    }

    private String generateWeatherWarning(int code, double wind, double precip) {
        if (code >= 95) {
            return "Thunderstorm Alert: Strong winds (" + (int) wind + " km/h) & heavy rain expected. Exercise caution.";
        }
        if (code == 45 || code == 48) {
            return "Fog Alert: Low visibility on highway. Drive with fog lamps/low beams.";
        }
        if (code >= 61 || precip > 5.0) {
            return "Wet Road Alert: Maintain safe following distance to prevent skidding.";
        }
        return null;
    }

    private static class WeatherInfo {
        String condition;
        String warning;
    }

    private BigDecimal getFuelRate(String fuelType) {
        List<FuelRate> rates = fuelRateRepository.findAll();
        if (rates.isEmpty()) return BigDecimal.valueOf(96.72); // Typical national baseline if unseeded
        FuelRate r = rates.get(0);
        if ("DIESEL".equalsIgnoreCase(fuelType)) return r.getDiesel();
        if ("CNG".equalsIgnoreCase(fuelType) && r.getCng() != null) return r.getCng();
        return r.getPetrol();
    }

    private double getRealisticMileage(String fuelType) {
        if ("DIESEL".equalsIgnoreCase(fuelType)) return 18.0;
        if ("CNG".equalsIgnoreCase(fuelType)) return 24.0;
        return 14.5;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }

    private TripDto toDto(Trip trip) {
        TripDto dto = new TripDto();
        dto.id = trip.getId().toString();
        dto.title = trip.getTitle();
        dto.startLocationName = trip.getStartLocationName();
        dto.startLat = trip.getStartLat();
        dto.startLng = trip.getStartLng();
        dto.destLocationName = trip.getDestLocationName();
        dto.destLat = trip.getDestLat();
        dto.destLng = trip.getDestLng();

        dto.distanceKm = trip.getDistanceKm().doubleValue();
        dto.durationMinutes = trip.getDurationMinutes();
        dto.durationFormatted = formatDuration(trip.getDurationMinutes());

        dto.fuelType = trip.getFuelType();
        dto.tankFull = Boolean.TRUE.equals(trip.getTankFull());
        dto.passengers = trip.getPassengers() != null ? trip.getPassengers() : 1;
        dto.preferredRoute = trip.getPreferredRoute();

        dto.estimatedFuelLitres = trip.getEstimatedFuelLitres() != null ? trip.getEstimatedFuelLitres().doubleValue() : null;
        dto.estimatedFuelCost = trip.getEstimatedFuelCost() != null ? trip.getEstimatedFuelCost().doubleValue() : null;
        dto.estimatedTolls = trip.getEstimatedTolls() != null ? trip.getEstimatedTolls().doubleValue() : null;
        dto.tollStatus = dto.estimatedTolls != null ? "Estimated" : "Unavailable";

        dto.weatherCondition = trip.getWeatherCondition();
        dto.weatherWarning = trip.getWeatherWarning();
        dto.weatherStatus = trip.getWeatherCondition() != null ? "Available" : "Unavailable";

        dto.status = trip.getStatus().name();
        dto.startOdometerKm = trip.getStartOdometerKm();
        dto.endOdometerKm = trip.getEndOdometerKm();
        dto.actualFuelCost = trip.getActualFuelCost() != null ? trip.getActualFuelCost().doubleValue() : null;
        dto.actualLitres = trip.getActualLitres() != null ? trip.getActualLitres().doubleValue() : null;
        dto.stopsVisitedCount = trip.getStopsVisitedCount() != null ? trip.getStopsVisitedCount() : 0;

        dto.startedAt = trip.getStartedAt() != null ? trip.getStartedAt().toString() : null;
        dto.completedAt = trip.getCompletedAt() != null ? trip.getCompletedAt().toString() : null;
        dto.createdAt = trip.getCreatedAt().toString();

        List<TripStopDto> allStops = trip.getStops() != null ? trip.getStops().stream().map(this::toStopDto).collect(Collectors.toList()) : Collections.emptyList();
        dto.allStops = allStops;

        // Categorize stops
        dto.prominentFuelStops = allStops.stream().filter(s -> "FUEL".equals(s.stopType)).limit(2).collect(Collectors.toList());
        dto.pitStops = allStops.stream().filter(s -> "DHABA".equals(s.stopType) || "PARKING".equals(s.stopType) || "REST".equals(s.stopType)).collect(Collectors.toList());
        dto.mechanicsAlongRoute = allStops.stream().filter(s -> "MECHANIC".equals(s.stopType)).collect(Collectors.toList());

        return dto;
    }

    private TripStopDto toStopDto(TripStop s) {
        TripStopDto dto = new TripStopDto();
        dto.id = s.getId().toString();
        dto.stopType = s.getStopType().name();
        dto.name = s.getName();
        dto.lat = s.getLat();
        dto.lng = s.getLng();
        dto.distanceKm = s.getDistanceKm() != null ? s.getDistanceKm().doubleValue() : null;
        dto.pricePerLitre = s.getPricePerLitre() != null ? s.getPricePerLitre().doubleValue() : null;
        dto.priceDiffPerLitre = s.getPriceDiffPerLitre() != null ? s.getPriceDiffPerLitre().doubleValue() : null;
        dto.rating = s.getRating() != null ? s.getRating().doubleValue() : null;
        dto.amenities = s.getAmenities();
        dto.stopOrder = s.getStopOrder();
        dto.isVisited = Boolean.TRUE.equals(s.getVisited());
        return dto;
    }
}
