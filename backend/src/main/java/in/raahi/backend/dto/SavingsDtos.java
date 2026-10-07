package in.raahi.backend.dto;
import java.math.BigDecimal;

import java.util.List;

public class SavingsDtos {

    public static class SavingsSummaryDto {
        // Month-over-Month Fuel Spending
        public Double currentMonthFuelSpend;
        public Double previousMonthFuelSpend;
        public Double fuelSpendSavings;
        public String fuelSpendSavingsMessage;
        public boolean hasMoMComparison;

        // Mileage & Efficiency
        public Double currentMonthLitres;
        public Double currentMonthDistanceKm;
        public Double currentMonthMileageKmPerLitre;
        public Double previousMonthMileageKmPerLitre;
        public Double mileageTrendKmPerLitre;
        public String mileageTrendText;

        // Maintenance Spending
        public BigDecimal totalMaintenanceSpend;
        public BigDecimal currentYearMaintenanceSpend;
        public int maintenanceRecordsCount;

        // Result-based Mileage & Care Streaks
        public List<StreakBadgeDto> streaks;

        // Fuel Price Radar
        public FuelPriceRadarDto fuelPriceRadar;

        // Community Benchmarking
        public CommunityBenchmarkDto communityBenchmark;
    }

    public static class StreakBadgeDto {
        public String id;
        public String title;
        public String subtitle;
        public String icon; // "FUEL", "MILEAGE", "MAINTENANCE"
        public String badgeText;
        public boolean isAchieved;
        public int currentCount;
        public int targetCount;
        public String progressText;
    }

    public static class FuelPriceRadarDto {
        public String homeState;
        public Double homeStatePetrol;
        public Double homeStateDiesel;
        public String cheapestNearbyState;
        public Double cheapestStatePetrol;
        public Double priceDifferencePerLitre;
        public String radarAdvice;
        public List<StateFuelRateDto> stateRates;
    }

    public static class StateFuelRateDto {
        public String state;
        public Double petrol;
        public Double diesel;
        public Double cng;
    }

    public static class CommunityBenchmarkDto {
        public boolean available;
        public String vehicleModel;
        public String fuelType;
        public Double userAvgMileage;
        public Double communityAvgMileage;
        public Double differenceKmPerLitre;
        public String comparisonSummary;
        public String note;
    }
}
