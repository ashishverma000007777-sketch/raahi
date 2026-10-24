package in.raahi.backend.service;

import in.raahi.backend.entity.Job;
import in.raahi.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CancellationProtectionPolicyServiceTest {

    private CancellationProtectionPolicyService service;

    @BeforeEach
    void setUp() {
        // Base rate: ₹12/km, Callout: ₹50, Return factor: 0.5, Min distance: 0.5 km, Cap: ₹500, Daily flag threshold: 3
        service = new CancellationProtectionPolicyService(true, 12.0, 50.0, 0.5, 0.5, 500.0, 3);
    }

    @Test
    void pureCalculationMath() {
        // actual: 2.0 km, return: 1.0 km, base: 50, rate: 12 => 50 + 3 * 12 = 86
        double amount = CancellationProtectionPolicyService.computeEstimatedAmount(2.0, 1.0, 50.0, 12.0, 500.0);
        assertEquals(86.0, amount);

        // 0 km actual travel should yield 0
        assertEquals(0.0, CancellationProtectionPolicyService.computeEstimatedAmount(0.0, 0.0, 50.0, 12.0, 500.0));

        // Exceeding cap of 500 should be capped at 500
        double capped = CancellationProtectionPolicyService.computeEstimatedAmount(40.0, 20.0, 50.0, 12.0, 500.0);
        assertEquals(500.0, capped);

        // Negative should yield 0
        double negative = CancellationProtectionPolicyService.computeEstimatedAmount(-5.0, 0.0, 50.0, 12.0, 500.0);
        assertEquals(0.0, negative);
    }

    private Job createEnRouteJob(Double startLat, Double startLng, Double reqLat, Double reqLng) {
        Job job = new Job();
        User helper = new User();
        helper.setId(UUID.randomUUID());
        helper.setLastLat(startLat);
        helper.setLastLng(startLng);
        job.setHelper(helper);

        User req = new User();
        req.setId(UUID.randomUUID());
        job.setRequester(req);

        job.setEnRouteAt(Instant.now().minusSeconds(300));
        job.setEnRouteLat(startLat);
        job.setEnRouteLng(startLng);
        job.setReqLat(reqLat);
        job.setReqLng(reqLng);
        job.setStatus(Job.Status.EN_ROUTE);
        return job;
    }

    @Test
    void missingGpsCoordinatesIsUnverifiedAndIneligible() {
        // No coordinates provided at all
        Job job = createEnRouteJob(null, null, 28.6139, 77.2090);
        var calc = service.calculate(job, null, null, 0);

        assertEquals("UNVERIFIED", calc.evidenceType);
        assertEquals(0.0, calc.actualTravelKm);
        assertFalse(calc.eligible);
        assertTrue(calc.eligibilityReason.contains("No reliable GPS"));
    }

    @Test
    void movementUnder50MetersIsTreatedAsUnverifiedTravel() {
        // Coordinates identical (0 meters moved)
        Job job = createEnRouteJob(28.6139, 77.2090, 28.7000, 77.3000);
        var calc = service.calculate(job, 28.6139, 77.2090, 0);

        assertEquals("UNVERIFIED", calc.evidenceType);
        assertEquals(0.0, calc.actualTravelKm);
        assertFalse(calc.eligible);
    }

    @Test
    void movementUnderMinimumDispatchDistanceIsIneligible() {
        // ~200 meters moved (less than 0.5 km min threshold)
        Job job = createEnRouteJob(28.6139, 77.2090, 28.7000, 77.3000);
        // Offset latitude by ~0.002 degrees (~220m)
        var calc = service.calculate(job, 28.6159, 77.2090, 0);

        assertEquals("GPS_VERIFIED", calc.evidenceType);
        assertTrue(calc.actualTravelKm < 0.5);
        assertFalse(calc.eligible);
        assertTrue(calc.eligibilityReason.contains("below minimum dispatch threshold"));
    }

    @Test
    void verifiedTravelIsEligiblePendingAdminReviewNoAutoApproval() {
        // Move ~2 km towards destination (0.018 lat diff is ~2 km)
        Job job = createEnRouteJob(28.6100, 77.2000, 28.6500, 77.2000);
        var calc = service.calculate(job, 28.6280, 77.2000, 0);

        assertEquals("GPS_VERIFIED", calc.evidenceType);
        assertEquals(2.0, calc.actualTravelKm);
        assertEquals(1.0, calc.returnDistanceKm); // 50% return factor
        assertEquals(86.0, calc.estimatedLossAmount); // 50 + (2 + 1) * 12 = 86
        assertTrue(calc.eligible);
        // Verified business policy: ALL claims require admin review
        assertTrue(calc.eligibilityReason.contains("Queued for admin review"));
        assertFalse(calc.eligibilityReason.contains("Auto-approved"));
    }

    @Test
    void largeClaimsAreQueuedForAdminReview() {
        // Travel ~10 km => 50 + (10.01 + 5.01) * 12 = ~230.24
        Job job = createEnRouteJob(28.6000, 77.2000, 28.8000, 77.2000);
        var calc = service.calculate(job, 28.6900, 77.2000, 0);

        assertEquals("GPS_VERIFIED", calc.evidenceType);
        assertTrue(calc.estimatedLossAmount > 150.0);
        assertTrue(calc.eligible);
        assertTrue(calc.eligibilityReason.contains("Queued for admin review"));
    }

    @Test
    void helperExceedingDailyClaimsIncludesAuditFlagInReviewNote() {
        // Move ~2 km, but helper already has 3 claims in 24h
        Job job = createEnRouteJob(28.6100, 77.2000, 28.6500, 77.2000);
        var calc = service.calculate(job, 28.6280, 77.2000, 3);

        assertEquals("GPS_VERIFIED", calc.evidenceType);
        assertTrue(calc.eligible);
        assertTrue(calc.eligibilityReason.contains("Audit Flag: Helper has filed 3 claims"));
    }

    @Test
    void auditInputsAndPolicyVersionArePreserved() {
        Job job = createEnRouteJob(28.6100, 77.2000, 28.6500, 77.2000);
        var calc = service.calculate(job, 28.6280, 77.2000, 0);

        assertEquals("2026.10-admin-v1", calc.policyVersion);
        assertEquals(50.0, calc.baseCalloutFee);
        assertEquals(12.0, calc.baseRatePerKm);
        assertEquals(0.5, calc.returnDistanceFactor);
    }
}
