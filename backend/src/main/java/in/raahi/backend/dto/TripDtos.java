package in.raahi.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public class TripDtos {

    public static class CreateTripRequest {
        @NotBlank(message = "startLocationName is required")
        @Size(max = 200, message = "startLocationName exceeds 200 characters")
        public String startLocationName;

        @NotNull(message = "startLat is required")
        @DecimalMin(value = "-90.0", message = "startLat must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "startLat must be between -90 and 90")
        public Double startLat;

        @NotNull(message = "startLng is required")
        @DecimalMin(value = "-180.0", message = "startLng must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "startLng must be between -180 and 180")
        public Double startLng;

        @NotBlank(message = "destLocationName is required")
        @Size(max = 200, message = "destLocationName exceeds 200 characters")
        public String destLocationName;

        @NotNull(message = "destLat is required")
        @DecimalMin(value = "-90.0", message = "destLat must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "destLat must be between -90 and 90")
        public Double destLat;

        @NotNull(message = "destLng is required")
        @DecimalMin(value = "-180.0", message = "destLng must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "destLng must be between -180 and 180")
        public Double destLng;

        @Size(max = 100, message = "title exceeds 100 characters")
        public String title;

        @Size(max = 20, message = "fuelType exceeds 20 characters")
        public String fuelType;

        public Boolean tankFull;

        @Min(value = 1, message = "passengers must be at least 1")
        @Max(value = 100, message = "passengers cannot exceed 100")
        public Integer passengers;

        @Size(max = 50, message = "preferredRoute exceeds 50 characters")
        public String preferredRoute;

        @PositiveOrZero(message = "odometer cannot be negative")
        @Max(value = 2000000, message = "odometer exceeds maximum plausible value")
        public Integer currentOdometer;
    }

    public static class StartTripRequest {
        @PositiveOrZero(message = "odometer cannot be negative")
        @Max(value = 2000000, message = "odometer exceeds maximum plausible value")
        public Integer startOdometerKm;
    }

    public static class CompleteTripRequest {
        @PositiveOrZero(message = "odometer cannot be negative")
        @Max(value = 2000000, message = "odometer exceeds maximum plausible value")
        public Integer endOdometerKm;

        @PositiveOrZero(message = "actualFuelCost cannot be negative")
        @DecimalMax(value = "1000000.00", message = "actualFuelCost exceeds limit")
        public BigDecimal actualFuelCost;

        @PositiveOrZero(message = "actualLitres cannot be negative")
        @DecimalMax(value = "5000.00", message = "actualLitres exceeds limit")
        public BigDecimal actualLitres;

        @PositiveOrZero(message = "stopsVisitedCount cannot be negative")
        @Max(value = 100, message = "stopsVisitedCount exceeds limit")
        public Integer stopsVisitedCount;
    }

    public static class TripStopDto {
        public String id;
        public String stopType; // 'FUEL', 'DHABA', 'PARKING', 'REST', 'MECHANIC', 'EMERGENCY'
        public String name;
        public double lat;
        public double lng;
        public Double distanceKm;
        public Double pricePerLitre;
        public Double priceDiffPerLitre;
        public Double rating;
        public String amenities;
        public int stopOrder;
        public boolean isVisited;
    }

    public static class TripDto {
        public String id;
        public String title;
        public String startLocationName;
        public double startLat;
        public double startLng;
        public String destLocationName;
        public double destLat;
        public double destLng;

        public double distanceKm;
        public int durationMinutes;
        public String durationFormatted;

        public String fuelType;
        public boolean tankFull;
        public int passengers;
        public String preferredRoute;

        public Double estimatedFuelLitres;
        public Double estimatedFuelCost;
        public Double estimatedTolls;
        public String tollStatus; // e.g. "Unavailable" or "Estimated"

        public String weatherCondition;
        public String weatherWarning;
        public String weatherStatus; // "Available" or "Unavailable"

        public String status; // 'PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'

        public Integer startOdometerKm;
        public Integer endOdometerKm;
        public Double actualFuelCost;
        public Double actualLitres;
        public int stopsVisitedCount;

        public String startedAt;
        public String completedAt;
        public String createdAt;

        public List<TripStopDto> prominentFuelStops;
        public List<TripStopDto> pitStops;
        public List<TripStopDto> mechanicsAlongRoute;
        public List<TripStopDto> allStops;
    }

    public static class TripSummaryDto {
        public String tripId;
        public String title;
        public String date;
        public double totalDistanceKm;
        public String durationFormatted;
        public Double fuelLoggedLitres;
        public Double fuelCost;
        public String tollsFormatted;
        public int stopsCount;
        public int stopsVisitedCount;
        public String routeDescription;
        public String drivingScoreMessage; // "Driving score unavailable — connect supported driving data to track it."

        public TripStoryCard storyCard;
    }

    public static class TripStoryCard {
        public String title;
        public String subtitle;
        public String distance;
        public String fuelCost;
        public String stopsCount;
        public String duration;
        public String shareableText;
    }
}
