package in.raahi.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class IntercityTripDtos {

    public static class CreateIntercityTripRequest {
        @NotBlank(message = "pickupCity is required")
        @Size(max = 100, message = "pickupCity exceeds 100 characters")
        public String pickupCity;

        @NotBlank(message = "destCity is required")
        @Size(max = 100, message = "destCity exceeds 100 characters")
        public String destCity;

        @NotBlank(message = "pickupAddress is required")
        @Size(max = 255, message = "pickupAddress exceeds 255 characters")
        public String pickupAddress;

        @NotBlank(message = "destAddress is required")
        @Size(max = 255, message = "destAddress exceeds 255 characters")
        public String destAddress;

        @NotNull(message = "pickupLat is required")
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        public Double pickupLat;

        @NotNull(message = "pickupLng is required")
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        public Double pickupLng;

        @NotNull(message = "destLat is required")
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        public Double destLat;

        @NotNull(message = "destLng is required")
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        public Double destLng;

        @NotNull(message = "departureTime is required")
        public Instant departureTime;

        public String tripType = "ONE_WAY"; // ONE_WAY or ROUND_TRIP
        public Instant returnTime;

        @NotBlank(message = "vehicleCategory is required")
        @Size(max = 50)
        public String vehicleCategory;

        public BigDecimal estimatedDistanceKm;
        public Integer estimatedDurationMin;

        @NotNull(message = "fare is required")
        @DecimalMin(value = "1.0", message = "fare must be greater than 0")
        @DecimalMax(value = "1000000.0", message = "fare exceeds maximum limit")
        public BigDecimal fare;

        public String paymentMode = "DIRECT_CASH";

        @Size(max = 255)
        public String cancellationTerms;

        public String specialInstructions;

        public Boolean saveAsDraft = false;
    }

    public static class ApplyTripRequest {
        @Size(max = 255)
        public String driverNotes;
    }

    public static class VerifyOtpRequest {
        @NotBlank(message = "otp is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "otp must be a 6-digit numeric string")
        public String otp;
    }

    public static class CancelTripRequest {
        @NotBlank(message = "reason is required")
        @Size(max = 255)
        public String reason;
    }

    public static class IntercityTripDto {
        public String id;
        public String agentId;
        public String agentName;
        public String agentPhone;
        public String agencyName;

        public String driverId;
        public String driverName;
        public String driverPhone;
        public Double driverRatingAvg;
        public String driverVehicleType;
        public String driverVehicleReg;

        public String pickupCity;
        public String destCity;
        public String pickupAddress;
        public String destAddress;
        public Double pickupLat;
        public Double pickupLng;
        public Double destLat;
        public Double destLng;

        public Instant departureTime;
        public String tripType;
        public Instant returnTime;
        public String vehicleCategory;
        public BigDecimal estimatedDistanceKm;
        public Integer estimatedDurationMin;

        public BigDecimal fare;
        public String paymentMode;
        public String paymentStatus;
        public String cancellationTerms;
        public String specialInstructions;

        // Security: pickupOtp only visible to AGENT prior to start; never to applicants
        public String pickupOtp;
        // Security: completionOtp only visible to AGENT prior to finish; never to applicants
        public String completionOtp;

        public String status;
        public String viewerRole; // "AGENT", "ASSIGNED_DRIVER", "APPLICANT", "VIEWER"
        public String applicationStatus; // Status of viewer's own application if APPLICANT
        public Integer totalApplications;

        public Instant assignedAt;
        public Instant arrivedAt;
        public Instant startedAt;
        public Instant completedAt;
        public Instant cancelledAt;
        public String cancelReason;
        public Instant createdAt;
    }

    public static class IntercityTripApplicationDto {
        public String id;
        public String tripId;
        public String driverId;
        public String driverName;
        public String driverPhone;
        public Double driverRatingAvg;
        public Integer driverTotalHelps;
        public String driverVehicleType;
        public String driverVehicleReg;
        public boolean driverVerified;

        public String status;
        public String driverNotes;
        public Instant createdAt;
    }

    public static class MarketplaceStatusDto {
        public boolean online;
        public String pickupCity;

        public MarketplaceStatusDto() {}
        public MarketplaceStatusDto(boolean online, String pickupCity) {
            this.online = online;
            this.pickupCity = pickupCity;
        }
    }

    public static class UpdateMarketplaceStatusRequest {
        public boolean online;
        public String pickupCity;
    }
}
