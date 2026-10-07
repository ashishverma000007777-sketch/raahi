package in.raahi.backend.dto;
import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public class VehicleDtos {

    /** Everything the user actually entered — nothing computed, nothing fabricated. */
    public static class VehicleDto {
        public String id;
        public String brand;
        public String model;
        public String variant;
        public Integer modelYear;
        public String fuelType;
        public String registrationNumber;
        public Integer odometerKm;
        public String odometerUpdatedAt;

        // Optional — null means "not provided", and every consumer (CarHealthService,
        // Android UI) must treat null as "unknown", never as zero/false/"none".
        public String lastServiceDate;
        public Integer lastServiceOdometerKm;
        public String insuranceExpiry;
        public String pucExpiry;
        public String tyreReplacedDate;
        public Integer tyreReplacedOdometerKm;
        public String batteryReplacedDate;
        public Integer batteryReplacedOdometerKm;

        public String createdAt;
        public String updatedAt;
    }

    /** Used for both initial "Set up your car" and later edits — same upsert endpoint. */
    public static class UpsertVehicleRequest {
        @NotBlank(message = "Brand is required")
        @Size(max = 50, message = "Brand exceeds 50 characters")
        public String brand;

        @NotBlank(message = "Model is required")
        @Size(max = 50, message = "Model exceeds 50 characters")
        public String model;

        @Size(max = 50, message = "Variant exceeds 50 characters")
        public String variant;

        @NotNull(message = "Model year is required")
        @Min(value = 1900, message = "Model year too early")
        @Max(value = 2100, message = "Model year invalid")
        public Integer modelYear;

        @NotBlank(message = "Fuel type is required")
        @Size(max = 20, message = "Fuel type exceeds 20 characters")
        public String fuelType;

        @NotBlank(message = "Registration number is required")
        @Size(min = 4, max = 20, message = "Registration number must be between 4 and 20 characters")
        @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Invalid registration number format")
        public String registrationNumber;

        @NotNull(message = "Current odometer reading is required")
        @Min(value = 0, message = "Odometer cannot be negative")
        @Max(value = 2000000, message = "Odometer exceeds plausible limit")
        public Integer odometerKm;

        @Size(max = 30)
        public String lastServiceDate;

        @Min(value = 0, message = "Odometer cannot be negative")
        @Max(value = 2000000, message = "Odometer exceeds plausible limit")
        public Integer lastServiceOdometerKm;

        @Size(max = 30)
        public String insuranceExpiry;

        @Size(max = 30)
        public String pucExpiry;

        @Size(max = 30)
        public String tyreReplacedDate;

        @Min(value = 0, message = "Odometer cannot be negative")
        @Max(value = 2000000, message = "Odometer exceeds plausible limit")
        public Integer tyreReplacedOdometerKm;

        @Size(max = 30)
        public String batteryReplacedDate;

        @Min(value = 0, message = "Odometer cannot be negative")
        @Max(value = 2000000, message = "Odometer exceeds plausible limit")
        public Integer batteryReplacedOdometerKm;
    }

    public static class UpdateOdometerRequest {
        @NotNull(message = "odometerKm is required")
        @Min(value = 0, message = "Odometer cannot be negative")
        @Max(value = 2000000, message = "Odometer exceeds plausible limit")
        public Integer odometerKm;
    }

    /**
     * score == null means "not enough real data to compute a meaningful score" — the Android
     * client renders that as "Complete your vehicle information to calculate your Car Health",
     * never a fabricated number. See CarHealthService for the exact deterministic formula.
     */
    public static class CarHealthDto {
        public Integer score; // 0-100, or null
        public String message; // populated only when score is null
        public int factorsConsidered;
        public int factorsTotal;
        public List<MaintenanceItemDto> maintenanceItems;

        // "Gaadi ka Report Card" attributes
        public String grade; // e.g. "A+", "A", "B+", "B", "C", "D"
        public Integer weeklyTrend; // +3, -2, 0 or null
        public String weeklyTrendText; // "↑ 3 pts this week"

        // 🛠 Care Sub-score
        public Integer careScore;
        public String careGrade;
        public String careSummary;
        public int careFactorsConsidered;
        public int careFactorsTotal;

        // ⛽ Efficiency Sub-score
        public Integer efficiencyScore;
        public String efficiencyGrade;
        public Double efficiencyMileageKmPerLitre;
        public String efficiencyTrendText;
        public String efficiencySummary;

        // 🛡 Safety Sub-score
        public Integer safetyScore;
        public String safetyGrade;
        public String safetyStatusText;
        public String safetySummary;

        // Weekly Report ("Your car this week")
        public WeeklyReportDto weeklyReport;

        // Year-End Report
        public YearEndReportDto yearEndReport;
    }

    public static class WeeklyReportDto {
        public String title;
        public Integer distanceDrivenKm;
        public Double fuelSpent;
        public Double litresFilled;
        public Integer tripsCompleted;
        public List<String> highlights;
    }

    public static class YearEndReportDto {
        public int year;
        public Integer totalTrips;
        public Double totalDistanceKm;
        public Double totalFuelSpent;
        public Double totalFuelLitres;
        public Integer maintenanceCount;
        public List<String> highlights;
    }

    public static class MaintenanceItemDto {
        public String type; // "Oil Change" | "Tyre Rotation" | "Battery Check" | "Insurance Renewal" | "PUC Renewal"
        public String status; // OK | DUE_SOON | OVERDUE
        public String detail; // human-readable, built from real fields only
        public Integer dueAtKm;
        public String dueAtDate;
    }

    public static class ServiceRecordDto {
        public String id;
        public String serviceDate;
        public Integer odometerKm;
        public String serviceType;
        public String notes;
        public BigDecimal cost;
        public String workshopName;
        public String partsReplaced;
        public String createdAt;
    }

    public static class CreateServiceRecordRequest {
        @NotBlank(message = "serviceDate is required")
        @Size(max = 30)
        public String serviceDate;

        @NotNull(message = "odometerKm is required")
        @Min(value = 0, message = "Odometer cannot be negative")
        @Max(value = 2000000, message = "Odometer exceeds plausible limit")
        public Integer odometerKm;

        @NotBlank(message = "serviceType is required")
        @Size(max = 50, message = "serviceType exceeds 50 characters")
        public String serviceType;

        @Size(max = 1000, message = "notes exceeds 1000 characters")
        public String notes;

        @PositiveOrZero(message = "cost cannot be negative")
        @Max(value = 1000000, message = "cost exceeds limit")
        public BigDecimal cost;

        @Size(max = 100, message = "workshopName exceeds 100 characters")
        public String workshopName;

        @Size(max = 500, message = "partsReplaced exceeds 500 characters")
        public String partsReplaced;
    }
}
