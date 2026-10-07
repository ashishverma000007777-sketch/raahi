package in.raahi.backend.service;

import in.raahi.backend.dto.SavingsDtos.SavingsSummaryDto;
import in.raahi.backend.entity.FuelLog;
import in.raahi.backend.entity.User;
import in.raahi.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SavingsServiceTest {

    private FuelLogRepository fuelLogRepository;
    private FuelRateRepository fuelRateRepository;
    private VehicleRepository vehicleRepository;
    private VehicleServiceRecordRepository serviceRecordRepository;
    private TripRepository tripRepository;
    private CarHealthService carHealthService;
    private SavingsService savingsService;

    @BeforeEach
    void setUp() {
        fuelLogRepository = Mockito.mock(FuelLogRepository.class);
        fuelRateRepository = Mockito.mock(FuelRateRepository.class);
        vehicleRepository = Mockito.mock(VehicleRepository.class);
        serviceRecordRepository = Mockito.mock(VehicleServiceRecordRepository.class);
        tripRepository = Mockito.mock(TripRepository.class);
        carHealthService = Mockito.mock(CarHealthService.class);

        savingsService = new SavingsService(
                fuelLogRepository,
                fuelRateRepository,
                vehicleRepository,
                serviceRecordRepository,
                tripRepository,
                carHealthService
        );
    }

    @Test
    void testSavingsSummaryWithNoLogsReturnsEmptyState() {
        UUID userId = UUID.randomUUID();
        when(fuelLogRepository.findSince(eq(userId), any(Instant.class))).thenReturn(new ArrayList<>());
        when(vehicleRepository.findByUserId(userId)).thenReturn(Optional.empty());

        SavingsSummaryDto dto = savingsService.getSavingsSummary(userId);

        assertNotNull(dto);
        assertEquals(0.0, dto.currentMonthFuelSpend);
        assertNull(dto.previousMonthFuelSpend);
        assertNull(dto.fuelSpendSavings);
        assertFalse(dto.hasMoMComparison);
        assertNotNull(dto.fuelSpendSavingsMessage);
        assertNull(dto.currentMonthMileageKmPerLitre);
        assertNotNull(dto.streaks);
        assertEquals(3, dto.streaks.size());
    }

    @Test
    void testSavingsSummaryWithTwoMonthsComputesMoMSavings() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        Instant currentMonthDate = today.withDayOfMonth(2).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant prevMonthDate = today.withDayOfMonth(1).minusMonths(1).plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<FuelLog> logs = new ArrayList<>();

        // Previous month log: ₹4000
        FuelLog l1 = new FuelLog();
        l1.setFilledAt(prevMonthDate);
        l1.setTotalCost(new BigDecimal("4000.00"));
        l1.setLitres(new BigDecimal("40.0"));
        l1.setOdometerKm(10000);
        logs.add(l1);

        // Current month log: ₹3000 (Saved ₹1000)
        FuelLog l2 = new FuelLog();
        l2.setFilledAt(currentMonthDate);
        l2.setTotalCost(new BigDecimal("3000.00"));
        l2.setLitres(new BigDecimal("30.0"));
        l2.setOdometerKm(10500);
        logs.add(l2);

        when(fuelLogRepository.findSince(eq(userId), any(Instant.class))).thenReturn(logs);
        when(vehicleRepository.findByUserId(userId)).thenReturn(Optional.empty());

        SavingsSummaryDto dto = savingsService.getSavingsSummary(userId);

        assertNotNull(dto);
        assertEquals(3000.0, dto.currentMonthFuelSpend);
        assertEquals(4000.0, dto.previousMonthFuelSpend);
        assertTrue(dto.hasMoMComparison);
        assertNotNull(dto.fuelSpendSavings);
        assertEquals(1000.0, dto.fuelSpendSavings);
        assertTrue(dto.fuelSpendSavingsMessage.contains("Saved ₹1000"));
    }
}
