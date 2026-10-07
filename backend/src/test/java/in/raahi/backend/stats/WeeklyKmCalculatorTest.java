package in.raahi.backend.stats;

import in.raahi.backend.stats.WeeklyKmCalculator.Reading;
import in.raahi.backend.stats.WeeklyKmCalculator.Result;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyKmCalculatorTest {

    @Test
    void testNeedMoreOdometerReadingsWhenUnderTwo() {
        Instant now = Instant.now();
        Result r = WeeklyKmCalculator.compute(5000, 1, null, null, now, 7);

        assertNull(r.km());
        assertEquals("NEED_MORE_ODOMETER_READINGS", r.unavailableReason());
    }

    @Test
    void testComputesFullWindowDistance() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofDays(7));
        Reading floor = new Reading(cutoff.minus(Duration.ofHours(2)), 4500);
        Reading oldest = new Reading(cutoff.minus(Duration.ofDays(30)), 3000);

        Result r = WeeklyKmCalculator.compute(5000, 10, floor, oldest, now, 7);

        assertNotNull(r.km());
        assertEquals(500, r.km()); // 5000 - 4500
        assertTrue(r.fullWindow());
        assertNull(r.unavailableReason());
    }

    @Test
    void testComputesPartialWindowWhenNewerThanWindow() {
        Instant now = Instant.now();
        Instant threeDaysAgo = now.minus(Duration.ofDays(3));
        Reading oldest = new Reading(threeDaysAgo, 4700);

        // No reading at or before cutoff (null floor)
        Result r = WeeklyKmCalculator.compute(5000, 2, null, oldest, now, 7);

        assertNotNull(r.km());
        assertEquals(300, r.km()); // 5000 - 4700
        assertFalse(r.fullWindow());
        assertNull(r.unavailableReason());
    }
}
