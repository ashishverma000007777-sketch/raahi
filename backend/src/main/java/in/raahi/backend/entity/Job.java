package in.raahi.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id")
    private User helper;

    @Column(name = "problem_type", nullable = false)
    private String problemType;

    @Column(name = "problem_desc")
    private String problemDesc;

    @Column(name = "req_lat", nullable = false)
    private Double reqLat;

    @Column(name = "req_lng", nullable = false)
    private Double reqLng;

    @Column(name = "highway_name")
    private String highwayName;

    @Column(name = "reward_amount")
    private Double rewardAmount = 0.0;

    @Column(name = "helper_otp")
    private String helperOtp;

    // Closes a brute-force gap found during the Phase 12 security audit: verify-otp had no
    // attempt limit, so an already-assigned helper (the only one who can even call it — see
    // JobController.verifyOtp) could brute-force the 6-digit code via the API instead of
    // actually meeting the requester in person, defeating the point of the handoff check.
    @Column(name = "otp_attempts")
    private int otpAttempts = 0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    public enum Status { PENDING, MATCHED, ARRIVED, IN_PROGRESS, WORK_DONE, COMPLETED, CANCELLED, EXPIRED }

    @Column(name = "arrived_at") private Instant arrivedAt;
    @Column(name = "arrival_lat") private Double arrivalLat;
    @Column(name = "arrival_lng") private Double arrivalLng;
    @Column(name = "arrival_method") private String arrivalMethod;
    @Column(name = "completion_otp") private String completionOtp;
    @Column(name = "completion_otp_attempts") private int completionOtpAttempts = 0;
    @Column(name = "work_done_at") private Instant workDoneAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "final_amount") private Double finalAmount;
    @Column(name = "payment_mode") private String paymentMode;
    @Column(name = "rated") private boolean rated = false;
    @Column(name = "cancelled_by") private UUID cancelledBy;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancel_reason") private String cancelReason;

    public UUID getId() { return id; }
    public User getRequester() { return requester; }
    public void setRequester(User v) { this.requester = v; }
    public User getHelper() { return helper; }
    public void setHelper(User v) { this.helper = v; }
    public String getProblemType() { return problemType; }
    public void setProblemType(String v) { this.problemType = v; }
    public String getProblemDesc() { return problemDesc; }
    public void setProblemDesc(String v) { this.problemDesc = v; }
    public Double getReqLat() { return reqLat; }
    public void setReqLat(Double v) { this.reqLat = v; }
    public Double getReqLng() { return reqLng; }
    public void setReqLng(Double v) { this.reqLng = v; }
    public Double getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(Double v) { this.rewardAmount = v; }
    public String getHelperOtp() { return helperOtp; }
    public void setHelperOtp(String v) { this.helperOtp = v; }
    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int v) { this.otpAttempts = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Instant v) { this.arrivedAt = v; }
    public Double getArrivalLat() { return arrivalLat; }
    public void setArrivalLat(Double v) { this.arrivalLat = v; }
    public Double getArrivalLng() { return arrivalLng; }
    public void setArrivalLng(Double v) { this.arrivalLng = v; }
    public String getArrivalMethod() { return arrivalMethod; }
    public void setArrivalMethod(String v) { this.arrivalMethod = v; }
    public String getCompletionOtp() { return completionOtp; }
    public void setCompletionOtp(String v) { this.completionOtp = v; }
    public int getCompletionOtpAttempts() { return completionOtpAttempts; }
    public void setCompletionOtpAttempts(int v) { this.completionOtpAttempts = v; }
    public Instant getWorkDoneAt() { return workDoneAt; }
    public void setWorkDoneAt(Instant v) { this.workDoneAt = v; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant v) { this.completedAt = v; }
    public Double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(Double v) { this.finalAmount = v; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String v) { this.paymentMode = v; }
    public boolean isRated() { return rated; }
    public void setRated(boolean v) { this.rated = v; }
    public UUID getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(UUID v) { this.cancelledBy = v; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant v) { this.cancelledAt = v; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String v) { this.cancelReason = v; }
}
