package in.raahi.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_cancellation_compensations")
public class JobCancellationCompensation {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id", nullable = false)
    private User helper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "cancellation_stage", nullable = false)
    private String cancellationStage;

    @Column(name = "reason_code", nullable = false)
    private String reasonCode;

    @Column(name = "reason_text")
    private String reasonText;

    @Column(name = "en_route_at")
    private Instant enRouteAt;

    @Column(name = "cancelled_at", nullable = false)
    private Instant cancelledAt;

    @Column(name = "start_lat")
    private Double startLat;

    @Column(name = "start_lng")
    private Double startLng;

    @Column(name = "cancel_lat")
    private Double cancelLat;

    @Column(name = "cancel_lng")
    private Double cancelLng;

    @Column(name = "dest_lat")
    private Double destLat;

    @Column(name = "dest_lng")
    private Double destLng;

    @Column(name = "one_way_distance_km")
    private Double oneWayDistanceKm;

    @Column(name = "actual_travel_km")
    private Double actualTravelKm;

    @Column(name = "return_distance_km")
    private Double returnDistanceKm;

    @Column(name = "evidence_type", nullable = false)
    private String evidenceType; // GPS_VERIFIED, ESTIMATED_DISPATCH, UNVERIFIED

    @Column(name = "estimated_loss_amount", nullable = false)
    private Double estimatedLossAmount;

    @Column(name = "approved_amount")
    private Double approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CompensationStatus status = CompensationStatus.ELIGIBLE_PENDING_REVIEW;

    @Column(name = "eligibility_notes")
    private String eligibilityNotes;

    @Column(name = "policy_version", nullable = false)
    private String policyVersion = "2026.10-admin-v1";

    @Column(name = "base_callout_fee")
    private Double baseCalloutFee;

    @Column(name = "base_rate_per_km")
    private Double baseRatePerKm;

    @Column(name = "return_factor")
    private Double returnFactor;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", nullable = false)
    private PayoutStatus payoutStatus = PayoutStatus.UNPAID;

    @Column(name = "payout_reference")
    private String payoutReference;

    @Column(name = "payout_at")
    private Instant payoutAt;

    @Column(name = "payout_notes")
    private String payoutNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes")
    private String reviewNotes;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public enum CompensationStatus {
        ELIGIBLE_PENDING_REVIEW,
        AUTO_APPROVED,
        ADMIN_APPROVED,
        REJECTED,
        NOT_ELIGIBLE
    }

    public enum PayoutStatus {
        UNPAID,
        PENDING_PAYOUT,
        PAID,
        FAILED
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public User getHelper() { return helper; }
    public void setHelper(User helper) { this.helper = helper; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public String getCancellationStage() { return cancellationStage; }
    public void setCancellationStage(String cancellationStage) { this.cancellationStage = cancellationStage; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    public Instant getEnRouteAt() { return enRouteAt; }
    public void setEnRouteAt(Instant enRouteAt) { this.enRouteAt = enRouteAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public Double getStartLat() { return startLat; }
    public void setStartLat(Double startLat) { this.startLat = startLat; }
    public Double getStartLng() { return startLng; }
    public void setStartLng(Double startLng) { this.startLng = startLng; }
    public Double getCancelLat() { return cancelLat; }
    public void setCancelLat(Double cancelLat) { this.cancelLat = cancelLat; }
    public Double getCancelLng() { return cancelLng; }
    public void setCancelLng(Double cancelLng) { this.cancelLng = cancelLng; }
    public Double getDestLat() { return destLat; }
    public void setDestLat(Double destLat) { this.destLat = destLat; }
    public Double getDestLng() { return destLng; }
    public void setDestLng(Double destLng) { this.destLng = destLng; }
    public Double getOneWayDistanceKm() { return oneWayDistanceKm; }
    public void setOneWayDistanceKm(Double oneWayDistanceKm) { this.oneWayDistanceKm = oneWayDistanceKm; }
    public Double getActualTravelKm() { return actualTravelKm; }
    public void setActualTravelKm(Double actualTravelKm) { this.actualTravelKm = actualTravelKm; }
    public Double getReturnDistanceKm() { return returnDistanceKm; }
    public void setReturnDistanceKm(Double returnDistanceKm) { this.returnDistanceKm = returnDistanceKm; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
    public Double getEstimatedLossAmount() { return estimatedLossAmount; }
    public void setEstimatedLossAmount(Double estimatedLossAmount) { this.estimatedLossAmount = estimatedLossAmount; }
    public Double getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(Double approvedAmount) { this.approvedAmount = approvedAmount; }
    public CompensationStatus getStatus() { return status; }
    public void setStatus(CompensationStatus status) { this.status = status; }
    public String getEligibilityNotes() { return eligibilityNotes; }
    public void setEligibilityNotes(String eligibilityNotes) { this.eligibilityNotes = eligibilityNotes; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }
    public Double getBaseCalloutFee() { return baseCalloutFee; }
    public void setBaseCalloutFee(Double baseCalloutFee) { this.baseCalloutFee = baseCalloutFee; }
    public Double getBaseRatePerKm() { return baseRatePerKm; }
    public void setBaseRatePerKm(Double baseRatePerKm) { this.baseRatePerKm = baseRatePerKm; }
    public Double getReturnFactor() { return returnFactor; }
    public void setReturnFactor(Double returnFactor) { this.returnFactor = returnFactor; }
    public PayoutStatus getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(PayoutStatus payoutStatus) { this.payoutStatus = payoutStatus; }
    public String getPayoutReference() { return payoutReference; }
    public void setPayoutReference(String payoutReference) { this.payoutReference = payoutReference; }
    public Instant getPayoutAt() { return payoutAt; }
    public void setPayoutAt(Instant payoutAt) { this.payoutAt = payoutAt; }
    public String getPayoutNotes() { return payoutNotes; }
    public void setPayoutNotes(String payoutNotes) { this.payoutNotes = payoutNotes; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
