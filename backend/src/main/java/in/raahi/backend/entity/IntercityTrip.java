package in.raahi.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "intercity_trips")
public class IntercityTrip {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @Column(name = "pickup_city", nullable = false)
    private String pickupCity;

    @Column(name = "dest_city", nullable = false)
    private String destCity;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "dest_address", nullable = false)
    private String destAddress;

    @Column(name = "pickup_lat", nullable = false)
    private Double pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    private Double pickupLng;

    @Column(name = "dest_lat", nullable = false)
    private Double destLat;

    @Column(name = "dest_lng", nullable = false)
    private Double destLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type", nullable = false)
    private RideType rideType;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false)
    private TripType tripType = TripType.ONE_WAY;

    @Column(name = "return_time")
    private Instant returnTime;

    @Column(name = "vehicle_category", nullable = false)
    private String vehicleCategory;

    @Column(name = "estimated_distance_km")
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Column(name = "fare", nullable = false)
    private BigDecimal fare;

    @Column(name = "customer_fare")
    private BigDecimal customerFare;

    @Column(name = "driver_payout")
    private BigDecimal driverPayout;

    @Column(name = "agent_commission")
    private BigDecimal agentCommission = BigDecimal.ZERO;

    @Column(name = "payment_recipient")
    private String paymentRecipient = "DRIVER";

    @Column(name = "settlement_responsibility")
    private String settlementResponsibility = "NONE";

    @Column(name = "settlement_ref")
    private String settlementRef;

    @Column(name = "settlement_notes")
    private String settlementNotes;

    @Column(name = "settlement_submitted_at")
    private Instant settlementSubmittedAt;

    @Column(name = "settlement_confirmed_at")
    private Instant settlementConfirmedAt;

    @Column(name = "payment_mode", nullable = false)
    private String paymentMode = "DIRECT_CASH";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "cancellation_terms")
    private String cancellationTerms;

    @Column(name = "special_instructions")
    private String specialInstructions;

    @Column(name = "pickup_otp")
    private String pickupOtp;

    @Column(name = "pickup_otp_attempts", nullable = false)
    private int pickupOtpAttempts = 0;

    @Column(name = "completion_otp")
    private String completionOtp;

    @Column(name = "completion_otp_attempts", nullable = false)
    private int completionOtpAttempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PUBLISHED;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "arrived_at")
    private Instant arrivedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "disputed_at")
    private Instant disputedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputed_by")
    private User disputedBy;

    @Column(name = "dispute_reason")
    private String disputeReason;

    @Column(name = "dispute_evidence")
    private String disputeEvidence;

    @Column(name = "dispute_resolution")
    private String disputeResolution;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntercityTripApplication> applications = new ArrayList<>();

    public enum RideType {
        OWNER,
        AGENT
    }

    public enum Status {
        DRAFT,
        PUBLISHED,
        ASSIGNED,
        EN_ROUTE,
        ARRIVED,
        IN_PROGRESS,
        AWAITING_AGENT_SETTLEMENT,
        SETTLEMENT_SUBMITTED,
        SETTLEMENT_CONFIRMED,
        COMPLETED,
        CANCELLED,
        DISPUTED
    }

    public enum TripType {
        ONE_WAY,
        ROUND_TRIP
    }

    public enum PaymentStatus {
        PENDING,
        PAID,
        SETTLED,
        FAILED
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getAgent() { return agent; }
    public void setAgent(User agent) { this.agent = agent; }
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public String getPickupCity() { return pickupCity; }
    public void setPickupCity(String pickupCity) { this.pickupCity = pickupCity; }
    public String getDestCity() { return destCity; }
    public void setDestCity(String destCity) { this.destCity = destCity; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDestAddress() { return destAddress; }
    public void setDestAddress(String destAddress) { this.destAddress = destAddress; }
    public Double getPickupLat() { return pickupLat; }
    public void setPickupLat(Double pickupLat) { this.pickupLat = pickupLat; }
    public Double getPickupLng() { return pickupLng; }
    public void setPickupLng(Double pickupLng) { this.pickupLng = pickupLng; }
    public Double getDestLat() { return destLat; }
    public void setDestLat(Double destLat) { this.destLat = destLat; }
    public Double getDestLng() { return destLng; }
    public void setDestLng(Double destLng) { this.destLng = destLng; }
    public Instant getDepartureTime() { return departureTime; }
    public void setDepartureTime(Instant departureTime) { this.departureTime = departureTime; }
    public TripType getTripType() { return tripType; }
    public void setTripType(TripType tripType) { this.tripType = tripType; }
    public Instant getReturnTime() { return returnTime; }
    public void setReturnTime(Instant returnTime) { this.returnTime = returnTime; }
    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }
    public BigDecimal getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }
    public Integer getEstimatedDurationMin() { return estimatedDurationMin; }
    public void setEstimatedDurationMin(Integer estimatedDurationMin) { this.estimatedDurationMin = estimatedDurationMin; }
    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getCancellationTerms() { return cancellationTerms; }
    public void setCancellationTerms(String cancellationTerms) { this.cancellationTerms = cancellationTerms; }
    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    public String getPickupOtp() { return pickupOtp; }
    public void setPickupOtp(String pickupOtp) { this.pickupOtp = pickupOtp; }
    public int getPickupOtpAttempts() { return pickupOtpAttempts; }
    public void setPickupOtpAttempts(int pickupOtpAttempts) { this.pickupOtpAttempts = pickupOtpAttempts; }
    public String getCompletionOtp() { return completionOtp; }
    public void setCompletionOtp(String completionOtp) { this.completionOtp = completionOtp; }
    public int getCompletionOtpAttempts() { return completionOtpAttempts; }
    public void setCompletionOtpAttempts(int completionOtpAttempts) { this.completionOtpAttempts = completionOtpAttempts; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public Instant getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Instant arrivedAt) { this.arrivedAt = arrivedAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public User getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(User cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<IntercityTripApplication> getApplications() { return applications; }
    public void setApplications(List<IntercityTripApplication> applications) { this.applications = applications; }

    public RideType getRideType() {
        if (rideType != null) {
            return rideType;
        }
        if (agent != null && agent.getRole() == User.Role.TRAVEL_AGENT) {
            return RideType.AGENT;
        }
        return RideType.OWNER;
    }
    public void setRideType(RideType rideType) { this.rideType = rideType; }

    public BigDecimal getCustomerFare() { return customerFare; }
    public void setCustomerFare(BigDecimal customerFare) { this.customerFare = customerFare; }

    public BigDecimal getDriverPayout() { return driverPayout; }
    public void setDriverPayout(BigDecimal driverPayout) { this.driverPayout = driverPayout; }

    public BigDecimal getAgentCommission() { return agentCommission; }
    public void setAgentCommission(BigDecimal agentCommission) { this.agentCommission = agentCommission; }

    public String getPaymentRecipient() { return paymentRecipient; }
    public void setPaymentRecipient(String paymentRecipient) { this.paymentRecipient = paymentRecipient; }

    public String getSettlementResponsibility() { return settlementResponsibility; }
    public void setSettlementResponsibility(String settlementResponsibility) { this.settlementResponsibility = settlementResponsibility; }

    public String getSettlementRef() { return settlementRef; }
    public void setSettlementRef(String settlementRef) { this.settlementRef = settlementRef; }

    public String getSettlementNotes() { return settlementNotes; }
    public void setSettlementNotes(String settlementNotes) { this.settlementNotes = settlementNotes; }

    public Instant getSettlementSubmittedAt() { return settlementSubmittedAt; }
    public void setSettlementSubmittedAt(Instant settlementSubmittedAt) { this.settlementSubmittedAt = settlementSubmittedAt; }

    public Instant getSettlementConfirmedAt() { return settlementConfirmedAt; }
    public void setSettlementConfirmedAt(Instant settlementConfirmedAt) { this.settlementConfirmedAt = settlementConfirmedAt; }

    public Instant getDisputedAt() { return disputedAt; }
    public void setDisputedAt(Instant disputedAt) { this.disputedAt = disputedAt; }

    public User getDisputedBy() { return disputedBy; }
    public void setDisputedBy(User disputedBy) { this.disputedBy = disputedBy; }

    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    public String getDisputeEvidence() { return disputeEvidence; }
    public void setDisputeEvidence(String disputeEvidence) { this.disputeEvidence = disputeEvidence; }

    public String getDisputeResolution() { return disputeResolution; }
    public void setDisputeResolution(String disputeResolution) { this.disputeResolution = disputeResolution; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public User getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(User resolvedBy) { this.resolvedBy = resolvedBy; }
}
