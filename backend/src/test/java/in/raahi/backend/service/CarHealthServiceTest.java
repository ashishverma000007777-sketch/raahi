package in.raahi.backend.service;

import in.raahi.backend.dto.VehicleDtos.CarHealthDto;
import in.raahi.backend.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CarHealthServiceTest {

    private CarHealthService service;

    @BeforeEach
    void setUp() {
        service = new CarHealthService();
    }

    @Test
    void testComputeWithNoDataReturnsNullScore() {
        Vehicle v = new Vehicle();
        CarHealthDto dto = service.compute(v);

        assertEquals(0, dto.factorsConsidered);
        assertEquals(3, dto.factorsTotal);
        assertNull(dto.score);
        assertNotNull(dto.message);
    }

    @Test
    void testComputeWithPerfectVehicleReturns100() {
        Vehicle v = new Vehicle();
        v.setOdometerKm(10000);
        v.setLastServiceOdometerKm(9000); // 1000 km since service <= 5000 (0 deduction)
        v.setLastServiceDate(LocalDate.now().minusMonths(1));
        v.setInsuranceExpiry(LocalDate.now().plusMonths(6)); // > 30 days (0 deduction)
        v.setPucExpiry(LocalDate.now().plusMonths(3)); // > 15 days (0 deduction)

        CarHealthDto dto = service.compute(v);

        assertEquals(3, dto.factorsConsidered);
        assertNotNull(dto.score);
        assertEquals(100, dto.score);
    }

    @Test
    void testComputeWithExpiredInsuranceAndPucAppliesCorrectDeductions() {
        Vehicle v = new Vehicle();
        v.setOdometerKm(16000);
        v.setLastServiceOdometerKm(7000); // 9000 km since service > 8000 (-30)
        v.setLastServiceDate(LocalDate.now().minusMonths(6));
        v.setInsuranceExpiry(LocalDate.now().minusDays(5)); // expired (-20)
        v.setPucExpiry(LocalDate.now().minusDays(2)); // expired (-15)

        CarHealthDto dto = service.compute(v);

        assertEquals(3, dto.factorsConsidered);
        assertNotNull(dto.score);
        // 100 - 30 - 20 - 15 = 35
        assertEquals(35, dto.score);
    }
}
