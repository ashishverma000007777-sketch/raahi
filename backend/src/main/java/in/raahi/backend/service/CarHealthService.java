package in.raahi.backend.service;

import in.raahi.backend.dto.VehicleDtos.CarHealthDto;
import in.raahi.backend.dto.VehicleDtos.MaintenanceItemDto;
import in.raahi.backend.dto.VehicleDtos.WeeklyReportDto;
import in.raahi.backend.dto.VehicleDtos.YearEndReportDto;
import in.raahi.backend.entity.FuelLog;
import in.raahi.backend.entity.Trip;
import in.raahi.backend.entity.User;
import in.raahi.backend.entity.Vehicle;
import in.raahi.backend.repository.FuelLogRepository;
import in.raahi.backend.repository.TripRepository;
import in.raahi.backend.repository.VehicleOdometerLogRepository;
import in.raahi.backend.repository.VehicleServiceRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deterministic Car Health & Report Card scoring.
 * Evaluates:
 * 1) Care (service records, PUC, insurance, tyre, battery)
 * 2) Efficiency (actual fuel logs, litres, cost, odometer, mileage trend)
 * 3) Safety (driving telemetry from completed trips, or explicitly "Not enough driving data" if none)
 * Plus Weekly and Year-End Reports.
 */
@Service
public class CarHealthService {

    private static final int SERVICE_INTERVAL_KM = 5000;
    private static final int SERVICE_DUE_SOON_KM = 8000;
    private static final int TYRE_INTERVAL_KM = 40000;
    private static final int BATTERY_INTERVAL_MONTHS = 30;
    private static final int INSURANCE_DUE_SOON_DAYS = 30;
    private static final int PUC_DUE_SOON_DAYS = 15;

    private final FuelLogRepository fuelLogRepository;
    private final TripRepository tripRepository;
    private final VehicleServiceRecordRepository serviceRecordRepository;
    private final VehicleOdometerLogRepository odometerLogRepository;

    public CarHealthService(
            @Autowired(required = false) FuelLogRepository fuelLogRepository,
            @Autowired(required = false) TripRepository tripRepository,
            @Autowired(required = false) VehicleServiceRecordRepository serviceRecordRepository,
            @Autowired(required = false) VehicleOdometerLogRepository odometerLogRepository) {
        this.fuelLogRepository = fuelLogRepository;
        this.tripRepository = tripRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.odometerLogRepository = odometerLogRepository;
    }

    public CarHealthService() {
        this(null, null, null, null);
    }

    public static String gradeForScore(Integer score) {
        if (score == null) return null;
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B+";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        return "D";
    }

    public CarHealthDto compute(Vehicle v) {
        return compute(v, v != null ? v.getUser() : null);
    }

    public CarHealthDto compute(Vehicle v, User user) {
        LocalDate today = LocalDate.now();
        int deduction = 0;
        int factorsConsidered = 0;
        final int factorsTotal = 3;

        // 1) Service factor
        if (v != null && v.getLastServiceDate() != null && v.getLastServiceOdometerKm() != null && v.getOdometerKm() != null) {
            factorsConsidered++;
            int kmSince = Math.max(0, v.getOdometerKm() - v.getLastServiceOdometerKm());
            if (kmSince > SERVICE_DUE_SOON_KM) deduction += 30;
            else if (kmSince > SERVICE_INTERVAL_KM) deduction += 15;
        }

        // 2) Insurance factor
        if (v != null && v.getInsuranceExpiry() != null) {
            factorsConsidered++;
            long daysToExpiry = ChronoUnit.DAYS.between(today, v.getInsuranceExpiry());
            if (daysToExpiry < 0) deduction += 20;
            else if (daysToExpiry <= INSURANCE_DUE_SOON_DAYS) deduction += 10;
        }

        // 3) PUC factor
        if (v != null && v.getPucExpiry() != null) {
            factorsConsidered++;
            long daysToExpiry = ChronoUnit.DAYS.between(today, v.getPucExpiry());
            if (daysToExpiry < 0) deduction += 15;
            else if (daysToExpiry <= PUC_DUE_SOON_DAYS) deduction += 7;
        }

        CarHealthDto dto = new CarHealthDto();
        dto.factorsConsidered = factorsConsidered;
        dto.factorsTotal = factorsTotal;
        dto.maintenanceItems = v != null ? maintenanceItems(v, today) : new ArrayList<>();

        // Care sub-score
        int careFactorsCount = factorsConsidered;
        if (v != null) {
            if (v.getTyreReplacedOdometerKm() != null) careFactorsCount++;
            if (v.getBatteryReplacedDate() != null) careFactorsCount++;
        }
        dto.careFactorsConsidered = careFactorsCount;
        dto.careFactorsTotal = 5;

        if (factorsConsidered > 0) {
            int cScore = Math.max(0, Math.min(100, 100 - deduction));
            dto.careScore = cScore;
            dto.careGrade = gradeForScore(cScore);
            dto.careSummary = (cScore >= 80) ? "Service, Insurance & PUC up to date" : "Service or document renewal attention recommended";
        } else {
            dto.careScore = null;
            dto.careGrade = null;
            dto.careSummary = "Add service, insurance, or PUC details to track vehicle care";
        }

        // Efficiency sub-score (from fuel logs)
        List<FuelLog> userFuelLogs = new ArrayList<>();
        if (fuelLogRepository != null && user != null) {
            try {
                userFuelLogs = fuelLogRepository.findSince(user.getId(), Instant.EPOCH);
            } catch (Exception ignored) {}
        }

        if (userFuelLogs.isEmpty()) {
            dto.efficiencyScore = null;
            dto.efficiencyGrade = null;
            dto.efficiencyMileageKmPerLitre = null;
            dto.efficiencyTrendText = "No fuel logs recorded yet";
            dto.efficiencySummary = "Add fuel refills in Fuel Log to track mileage and cost";
        } else {
            List<FuelLog> sortedLogs = userFuelLogs.stream()
                    .sorted(Comparator.comparing(FuelLog::getFilledAt))
                    .collect(Collectors.toList());

            double totalKm = 0;
            double totalLitresForMileage = 0;
            for (int i = 1; i < sortedLogs.size(); i++) {
                FuelLog prev = sortedLogs.get(i - 1);
                FuelLog curr = sortedLogs.get(i);
                if (curr.getOdometerKm() != null && prev.getOdometerKm() != null
                        && curr.getOdometerKm() > prev.getOdometerKm()
                        && curr.getLitres() != null && curr.getLitres().doubleValue() > 0) {
                    totalKm += (curr.getOdometerKm() - prev.getOdometerKm());
                    totalLitresForMileage += curr.getLitres().doubleValue();
                }
            }

            if (totalLitresForMileage > 0) {
                double avgMileage = Math.round((totalKm / totalLitresForMileage) * 10.0) / 10.0;
                dto.efficiencyMileageKmPerLitre = avgMileage;
                int effScore;
                if (avgMileage >= 18.0) effScore = 95;
                else if (avgMileage >= 15.0) effScore = 88;
                else if (avgMileage >= 12.0) effScore = 78;
                else if (avgMileage >= 9.0) effScore = 68;
                else effScore = 55;
                dto.efficiencyScore = effScore;
                dto.efficiencyGrade = gradeForScore(effScore);
                dto.efficiencyTrendText = String.format("%.1f km/L across %d refills", avgMileage, sortedLogs.size());
                dto.efficiencySummary = "Efficiency calculated from real odometer & fuel entries";
            } else {
                dto.efficiencyScore = 75;
                dto.efficiencyGrade = "B";
                dto.efficiencyMileageKmPerLitre = null;
                dto.efficiencyTrendText = sortedLogs.size() + " refill(s) logged · Log next refill for exact km/L";
                dto.efficiencySummary = "Log your next refill with odometer reading to calculate real mileage";
            }
        }

        // Safety sub-score (from trip driving telemetry)
        List<Trip> userTrips = new ArrayList<>();
        if (tripRepository != null && user != null) {
            try {
                userTrips = tripRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            } catch (Exception ignored) {}
        }

        List<Trip> completedTripsWithScore = userTrips.stream()
                .filter(t -> t.getStatus() == Trip.Status.COMPLETED && t.getDrivingScore() != null)
                .collect(Collectors.toList());

        if (completedTripsWithScore.isEmpty()) {
            dto.safetyScore = null;
            dto.safetyGrade = null;
            dto.safetyStatusText = "Not enough driving data";
            dto.safetySummary = "Take trips with Trip Mode to calculate your driving safety score";
        } else {
            double avgScore = completedTripsWithScore.stream().mapToInt(Trip::getDrivingScore).average().orElse(80.0);
            int sScore = (int) Math.round(avgScore);
            dto.safetyScore = sScore;
            dto.safetyGrade = gradeForScore(sScore);
            dto.safetyStatusText = "Based on " + completedTripsWithScore.size() + " completed trip" + (completedTripsWithScore.size() > 1 ? "s" : "");
            dto.safetySummary = "Driving safety measured from completed highway journeys";
        }

        // Overall Score & Grade
        if (factorsConsidered == 0 && dto.efficiencyScore == null && dto.safetyScore == null) {
            dto.score = null;
            dto.grade = null;
            dto.message = "Complete your vehicle information to calculate your Car Health";
        } else {
            if (dto.efficiencyScore == null && dto.safetyScore == null) {
                dto.score = dto.careScore;
            } else {
                double totalWeight = 0;
                double weightedSum = 0;
                if (dto.careScore != null) {
                    weightedSum += dto.careScore * 0.45;
                    totalWeight += 0.45;
                }
                if (dto.efficiencyScore != null) {
                    weightedSum += dto.efficiencyScore * 0.30;
                    totalWeight += 0.30;
                }
                if (dto.safetyScore != null) {
                    weightedSum += dto.safetyScore * 0.25;
                    totalWeight += 0.25;
                }
                dto.score = (int) Math.round(weightedSum / (totalWeight > 0 ? totalWeight : 1.0));
            }
            dto.grade = gradeForScore(dto.score);
            dto.message = null;
        }

        // Weekly Trend
        int trend = 0;
        Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        boolean recentFuel = userFuelLogs.stream().anyMatch(f -> f.getFilledAt() != null && f.getFilledAt().isAfter(oneWeekAgo));
        if (recentFuel) trend += 3;
        if (v != null && v.getPucExpiry() != null && v.getPucExpiry().isBefore(today) && v.getPucExpiry().isAfter(today.minusDays(7))) {
            trend -= 5;
        }
        if (v != null && v.getInsuranceExpiry() != null && v.getInsuranceExpiry().isBefore(today) && v.getInsuranceExpiry().isAfter(today.minusDays(7))) {
            trend -= 5;
        }
        dto.weeklyTrend = trend;
        dto.weeklyTrendText = (trend > 0 ? "↑ " + trend + " pts this week" : (trend < 0 ? "↓ " + (-trend) + " pts this week" : "Stable this week"));

        // Weekly Report ("Your car this week")
        WeeklyReportDto wr = new WeeklyReportDto();
        wr.title = "Your car this week";
        wr.highlights = new ArrayList<>();

        List<FuelLog> weekFuel = userFuelLogs.stream().filter(f -> f.getFilledAt() != null && f.getFilledAt().isAfter(oneWeekAgo)).collect(Collectors.toList());
        double weekFuelSpent = weekFuel.stream().mapToDouble(f -> f.getTotalCost() != null ? f.getTotalCost().doubleValue() : 0.0).sum();
        double weekLitres = weekFuel.stream().mapToDouble(f -> f.getLitres() != null ? f.getLitres().doubleValue() : 0.0).sum();
        wr.fuelSpent = weekFuelSpent > 0 ? weekFuelSpent : null;
        wr.litresFilled = weekLitres > 0 ? weekLitres : null;

        List<Trip> weekTrips = userTrips.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(oneWeekAgo) && t.getStatus() == Trip.Status.COMPLETED)
                .collect(Collectors.toList());
        wr.tripsCompleted = weekTrips.size();
        double weekTripDistance = weekTrips.stream().mapToDouble(t -> t.getDistanceKm() != null ? t.getDistanceKm().doubleValue() : 0.0).sum();
        wr.distanceDrivenKm = (int) Math.round(weekTripDistance);

        if (wr.tripsCompleted > 0) {
            wr.highlights.add("Completed " + wr.tripsCompleted + " trip(s) totaling " + wr.distanceDrivenKm + " km.");
        }
        if (weekFuelSpent > 0) {
            wr.highlights.add(String.format("Filled %.1f L fuel (₹%.0f spent) across %d refill(s).", weekLitres, weekFuelSpent, weekFuel.size()));
        }
        if (dto.careScore != null) {
            wr.highlights.add("Care score standing at " + dto.careScore + "/100 (" + dto.careGrade + ").");
        }
        if (wr.highlights.isEmpty()) {
            wr.highlights.add("No highway trips or fuel entries logged in the past 7 days.");
        }
        dto.weeklyReport = wr;

        // Year-End Report
        YearEndReportDto yr = new YearEndReportDto();
        yr.year = today.getYear();
        yr.highlights = new ArrayList<>();
        Instant yearStart = LocalDate.of(today.getYear(), 1, 1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        List<Trip> yearTrips = userTrips.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(yearStart) && t.getStatus() == Trip.Status.COMPLETED)
                .collect(Collectors.toList());
        yr.totalTrips = yearTrips.size();
        yr.totalDistanceKm = yearTrips.stream().mapToDouble(t -> t.getDistanceKm() != null ? t.getDistanceKm().doubleValue() : 0.0).sum();

        List<FuelLog> yearFuel = userFuelLogs.stream().filter(f -> f.getFilledAt() != null && f.getFilledAt().isAfter(yearStart)).collect(Collectors.toList());
        yr.totalFuelSpent = yearFuel.stream().mapToDouble(f -> f.getTotalCost() != null ? f.getTotalCost().doubleValue() : 0.0).sum();
        yr.totalFuelLitres = yearFuel.stream().mapToDouble(f -> f.getLitres() != null ? f.getLitres().doubleValue() : 0.0).sum();
        yr.maintenanceCount = dto.maintenanceItems.size();

        yr.highlights.add(String.format("Driven %.0f km across %d completed trip(s) in %d.", yr.totalDistanceKm, yr.totalTrips, yr.year));
        if (yr.totalFuelSpent > 0) {
            yr.highlights.add(String.format("Recorded ₹%.0f fuel spend across %.1f litres.", yr.totalFuelSpent, yr.totalFuelLitres));
        }
        yr.highlights.add(dto.maintenanceItems.stream().anyMatch(m -> "OVERDUE".equals(m.status))
                ? "Immediate maintenance required for overdue items."
                : "Vehicle care and documentation in good standing.");
        dto.yearEndReport = yr;

        return dto;
    }

    private List<MaintenanceItemDto> maintenanceItems(Vehicle v, LocalDate today) {
        List<MaintenanceItemDto> items = new ArrayList<>();

        if (v.getLastServiceDate() != null && v.getLastServiceOdometerKm() != null && v.getOdometerKm() != null) {
            int kmSince = Math.max(0, v.getOdometerKm() - v.getLastServiceOdometerKm());
            int dueAtKm = v.getLastServiceOdometerKm() + SERVICE_INTERVAL_KM;
            MaintenanceItemDto item = new MaintenanceItemDto();
            item.type = "Oil Change";
            item.dueAtKm = dueAtKm;
            if (kmSince > SERVICE_INTERVAL_KM) {
                item.status = "OVERDUE";
                item.detail = "Overdue! Last service at " + v.getLastServiceOdometerKm() + " km, now at " + v.getOdometerKm() + " km (every " + SERVICE_INTERVAL_KM + " km)";
            } else if (kmSince > SERVICE_INTERVAL_KM - 1000) {
                item.status = "DUE_SOON";
                item.detail = (dueAtKm - v.getOdometerKm()) + " km away";
            } else {
                item.status = "OK";
                item.detail = (dueAtKm - v.getOdometerKm()) + " km away";
            }
            items.add(item);
        }

        if (v.getTyreReplacedOdometerKm() != null && v.getOdometerKm() != null) {
            int kmSince = Math.max(0, v.getOdometerKm() - v.getTyreReplacedOdometerKm());
            int dueAtKm = v.getTyreReplacedOdometerKm() + TYRE_INTERVAL_KM;
            MaintenanceItemDto item = new MaintenanceItemDto();
            item.type = "Tyre Rotation";
            item.dueAtKm = dueAtKm;
            item.status = kmSince > TYRE_INTERVAL_KM ? "OVERDUE" : (kmSince > TYRE_INTERVAL_KM - 5000 ? "DUE_SOON" : "OK");
            item.detail = item.status.equals("OVERDUE")
                    ? "Overdue since " + dueAtKm + " km"
                    : (dueAtKm - v.getOdometerKm()) + " km away";
            items.add(item);
        }

        if (v.getBatteryReplacedDate() != null) {
            long monthsSince = ChronoUnit.MONTHS.between(v.getBatteryReplacedDate(), today);
            LocalDate dueDate = v.getBatteryReplacedDate().plusMonths(BATTERY_INTERVAL_MONTHS);
            MaintenanceItemDto item = new MaintenanceItemDto();
            item.type = "Battery Check";
            item.dueAtDate = dueDate.toString();
            item.status = monthsSince >= BATTERY_INTERVAL_MONTHS ? "DUE_SOON" : "OK";
            item.detail = item.status.equals("DUE_SOON")
                    ? "Battery is " + monthsSince + " months old — have it checked"
                    : "Due around " + dueDate;
            items.add(item);
        }

        if (v.getInsuranceExpiry() != null) {
            long daysTo = ChronoUnit.DAYS.between(today, v.getInsuranceExpiry());
            MaintenanceItemDto item = new MaintenanceItemDto();
            item.type = "Insurance Renewal";
            item.dueAtDate = v.getInsuranceExpiry().toString();
            item.status = daysTo < 0 ? "OVERDUE" : (daysTo <= INSURANCE_DUE_SOON_DAYS ? "DUE_SOON" : "OK");
            item.detail = daysTo < 0 ? "Expired " + (-daysTo) + " days ago" : "Expires in " + daysTo + " days";
            items.add(item);
        }

        if (v.getPucExpiry() != null) {
            long daysTo = ChronoUnit.DAYS.between(today, v.getPucExpiry());
            MaintenanceItemDto item = new MaintenanceItemDto();
            item.type = "PUC Renewal";
            item.dueAtDate = v.getPucExpiry().toString();
            item.status = daysTo < 0 ? "OVERDUE" : (daysTo <= PUC_DUE_SOON_DAYS ? "DUE_SOON" : "OK");
            item.detail = daysTo < 0 ? "Expired " + (-daysTo) + " days ago" : "Expires in " + daysTo + " days";
            items.add(item);
        }

        return items;
    }
}
