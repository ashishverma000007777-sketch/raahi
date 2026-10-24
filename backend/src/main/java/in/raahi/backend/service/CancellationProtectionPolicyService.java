package in.raahi.backend.service;

import in.raahi.backend.entity.Job;
import in.raahi.backend.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service calculating travel-loss compensation and enforcing eligibility rules
 * under Raahi's Roadside Assistance Cancellation Protection Policy.
 *
 * Confirmed Business Policy:
 * 1. ALL helper compensation claims require admin review. Automatic approval is strictly disabled.
 * 2. Every eligible claim is created as ELIGIBLE_PENDING_REVIEW.
 * 3. Ineligible claims are marked NOT_ELIGIBLE with a persisted reason.
 * 4. Stored calculation inputs and policy version ensure full auditability.
 */
@Service
public class CancellationProtectionPolicyService {

    public static final String POLICY_VERSION = "2026.10-admin-v1";

    private final boolean enabled;
    private final double baseRatePerKm;
    private final double baseCalloutFee;
    private final double returnDistanceFactor;
    private final double minDispatchDistanceKm;
    private final double maxCompensationCap;
    private final int maxDailyClaimsPerHelper;

    public CancellationProtectionPolicyService(
            @Value("${raahi.cancellation-protection.enabled:true}") boolean enabled,
            @Value("${raahi.cancellation-protection.base-rate-per-km:12.0}") double baseRatePerKm,
            @Value("${raahi.cancellation-protection.base-callout-fee:50.0}") double baseCalloutFee,
            @Value("${raahi.cancellation-protection.return-distance-factor:0.5}") double returnDistanceFactor,
            @Value("${raahi.cancellation-protection.min-dispatch-distance-km:0.5}") double minDispatchDistanceKm,
            @Value("${raahi.cancellation-protection.max-compensation-cap:500.0}") double maxCompensationCap,
            @Value("${raahi.cancellation-protection.max-daily-claims-per-helper:3}") int maxDailyClaimsPerHelper) {
        this.enabled = enabled;
        this.baseRatePerKm = Math.max(0.0, baseRatePerKm);
        this.baseCalloutFee = Math.max(0.0, baseCalloutFee);
        this.returnDistanceFactor = Math.max(0.0, returnDistanceFactor);
        this.minDispatchDistanceKm = Math.max(0.0, minDispatchDistanceKm);
        this.maxCompensationCap = Math.max(0.0, maxCompensationCap);
        this.maxDailyClaimsPerHelper = Math.max(1, maxDailyClaimsPerHelper);
    }

    public static class TravelLossCalculation {
        public String policyVersion = POLICY_VERSION;
        public double baseCalloutFee;
        public double baseRatePerKm;
        public double returnDistanceFactor;

        public double oneWayDistanceKm;
        public double actualTravelKm;
        public double returnDistanceKm;
        public double estimatedLossAmount;
        public String evidenceType; // GPS_VERIFIED, UNVERIFIED
        public boolean eligible;     // true -> ELIGIBLE_PENDING_REVIEW; false -> NOT_ELIGIBLE
        public String eligibilityReason;

        public Double startLat;
        public Double startLng;
        public Double cancelLat;
        public Double cancelLng;
        public Double destLat;
        public Double destLng;
    }

    /**
     * Pure calculation helper for unit testing and deterministic calculations.
     *
     * Formula:
     * baseCalloutFee (INR) + (actualTravelKm (km) + returnDistanceKm (km)) * baseRatePerKm (INR/km)
     * Capped at maxCompensationCap (INR) and rounded to 2 decimal places.
     */
    public static double computeEstimatedAmount(double actualTravelKm, double returnDistanceKm,
                                                double baseCalloutFee, double baseRatePerKm,
                                                double maxCompensationCap) {
        if (actualTravelKm <= 0.0) return 0.0;
        double totalTravelKm = actualTravelKm + returnDistanceKm;
        double raw = baseCalloutFee + (totalTravelKm * baseRatePerKm);
        double capped = Math.min(raw, maxCompensationCap);
        return CommissionMath.round2(Math.max(0.0, capped));
    }

    /**
     * Evaluates travel loss for an en-route cancellation given start, current, and destination coordinates.
     * All eligible claims are queued for admin review; NO claim is auto-approved.
     */
    public TravelLossCalculation calculate(Job job, Double reportedCancelLat, Double reportedCancelLng, long recentClaimsCount) {
        TravelLossCalculation calc = new TravelLossCalculation();
        calc.policyVersion = POLICY_VERSION;
        calc.baseCalloutFee = baseCalloutFee;
        calc.baseRatePerKm = baseRatePerKm;
        calc.returnDistanceFactor = returnDistanceFactor;

        if (!enabled) {
            calc.evidenceType = "UNVERIFIED";
            calc.eligible = false;
            calc.eligibilityReason = "Cancellation protection policy is disabled in configuration.";
            return calc;
        }

        User helper = job.getHelper();
        Double startLat = job.getEnRouteLat();
        Double startLng = job.getEnRouteLng();
        if (startLat == null && helper != null) {
            startLat = helper.getLastLat();
            startLng = helper.getLastLng();
        }

        Double destLat = job.getReqLat();
        Double destLng = job.getReqLng();

        // Cancellation location: prefer reported coords from request, then helper's last location
        Double cancelLat = reportedCancelLat;
        Double cancelLng = reportedCancelLng;
        if ((cancelLat == null || cancelLng == null) && helper != null) {
            cancelLat = helper.getLastLat();
            cancelLng = helper.getLastLng();
        }

        calc.startLat = startLat;
        calc.startLng = startLng;
        calc.cancelLat = cancelLat;
        calc.cancelLng = cancelLng;
        calc.destLat = destLat;
        calc.destLng = destLng;

        // One-way total dispatch distance (for progress context, not double-counted)
        if (startLat != null && startLng != null && destLat != null && destLng != null) {
            calc.oneWayDistanceKm = CommissionMath.round2(GeoUtil.distanceMeters(startLat, startLng, destLat, destLng) / 1000.0);
        }

        // Evaluate actual travel distance & evidence
        boolean hasStart = startLat != null && startLng != null;
        boolean hasCancel = cancelLat != null && cancelLng != null;

        if (hasStart && hasCancel) {
            double metersFromStart = GeoUtil.distanceMeters(startLat, startLng, cancelLat, cancelLng);
            double kmTravelled = metersFromStart / 1000.0;

            // Check if coordinates indicate genuine movement (> 50 meters)
            if (metersFromStart >= 50.0) {
                calc.evidenceType = "GPS_VERIFIED";
                calc.actualTravelKm = CommissionMath.round2(kmTravelled);
            } else {
                // Coordinates exist but helper didn't move beyond 50m
                calc.evidenceType = "UNVERIFIED";
                calc.actualTravelKm = 0.0;
            }
        } else {
            calc.evidenceType = "UNVERIFIED";
            calc.actualTravelKm = 0.0;
        }

        // Return distance calculation
        calc.returnDistanceKm = CommissionMath.round2(calc.actualTravelKm * returnDistanceFactor);

        // Cost estimation
        calc.estimatedLossAmount = computeEstimatedAmount(calc.actualTravelKm, calc.returnDistanceKm,
                baseCalloutFee, baseRatePerKm, maxCompensationCap);

        // Enforce Business Policy: Admin Review Only (No Auto-Approval)
        if (!"GPS_VERIFIED".equals(calc.evidenceType) || calc.actualTravelKm <= 0.0) {
            calc.eligible = false;
            calc.eligibilityReason = "No reliable GPS travel movement verified. Ineligible for travel loss claim.";
        } else if (calc.actualTravelKm < minDispatchDistanceKm) {
            calc.eligible = false;
            calc.eligibilityReason = "Distance travelled (" + calc.actualTravelKm + " km) is below minimum dispatch threshold ("
                    + minDispatchDistanceKm + " km). Ineligible for reimbursement.";
        } else {
            calc.eligible = true;
            String note = "Verified GPS movement of " + calc.actualTravelKm + " km (est. ₹" + calc.estimatedLossAmount
                    + "). Queued for admin review.";
            if (recentClaimsCount >= maxDailyClaimsPerHelper) {
                note += " [Audit Flag: Helper has filed " + recentClaimsCount + " claims in past 24h]";
            }
            calc.eligibilityReason = note;
        }

        return calc;
    }

    public boolean isEnabled() { return enabled; }
    public double getBaseRatePerKm() { return baseRatePerKm; }
    public double getBaseCalloutFee() { return baseCalloutFee; }
    public double getReturnDistanceFactor() { return returnDistanceFactor; }
    public double getMinDispatchDistanceKm() { return minDispatchDistanceKm; }
    public double getMaxCompensationCap() { return maxCompensationCap; }
    public int getMaxDailyClaimsPerHelper() { return maxDailyClaimsPerHelper; }
}
