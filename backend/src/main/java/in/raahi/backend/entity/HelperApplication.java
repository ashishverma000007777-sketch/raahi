package in.raahi.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "helper_applications")
public class HelperApplication {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aadhaar_front_id")
    private VerificationDocument aadhaarFront;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aadhaar_back_id")
    private VerificationDocument aadhaarBack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selfie_id")
    private VerificationDocument selfie;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "submitted_at")
    private Instant submittedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    public enum Status { PENDING, APPROVED, REJECTED, SUSPENDED }

    @Column(name = "experience_years") private Integer experienceYears;
    @Column(name = "service_area") private String serviceArea;
    @Column(name = "services") private String services;
    @Column(name = "equipment") private String equipment;
    @Column(name = "vehicle_type") private String vehicleType;
    @Column(name = "vehicle_brand") private String vehicleBrand;
    @Column(name = "vehicle_model") private String vehicleModel;
    @Column(name = "vehicle_variant") private String vehicleVariant;
    @Column(name = "vehicle_reg") private String vehicleReg;
    @Column(name = "service_radius_km") private Integer serviceRadiusKm;
    @Column(name = "payout_upi") private String payoutUpi;
    @Column(name = "payout_holder") private String payoutHolder;
    @Column(name = "payout_bank_acct") private String payoutBankAcct;
    @Column(name = "payout_ifsc") private String payoutIfsc;
    @Column(name = "suspension_reason") private String suspensionReason;

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public VerificationDocument getAadhaarFront() { return aadhaarFront; }
    public void setAadhaarFront(VerificationDocument v) { this.aadhaarFront = v; }
    public VerificationDocument getAadhaarBack() { return aadhaarBack; }
    public void setAadhaarBack(VerificationDocument v) { this.aadhaarBack = v; }
    public VerificationDocument getSelfie() { return selfie; }
    public void setSelfie(VerificationDocument v) { this.selfie = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant v) { this.reviewedAt = v; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User v) { this.reviewedBy = v; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer v) { this.experienceYears = v; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String v) { this.serviceArea = v; }
    public String getServices() { return services; }
    public void setServices(String v) { this.services = v; }
    public String getEquipment() { return equipment; }
    public void setEquipment(String v) { this.equipment = v; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String v) { this.vehicleType = v; }
    public String getVehicleBrand() { return vehicleBrand; }
    public void setVehicleBrand(String v) { this.vehicleBrand = v; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String v) { this.vehicleModel = v; }
    public String getVehicleVariant() { return vehicleVariant; }
    public void setVehicleVariant(String v) { this.vehicleVariant = v; }
    public String getVehicleReg() { return vehicleReg; }
    public void setVehicleReg(String v) { this.vehicleReg = v; }
    public Integer getServiceRadiusKm() { return serviceRadiusKm; }
    public void setServiceRadiusKm(Integer v) { this.serviceRadiusKm = v; }
    public String getPayoutUpi() { return payoutUpi; }
    public void setPayoutUpi(String v) { this.payoutUpi = v; }
    public String getPayoutHolder() { return payoutHolder; }
    public void setPayoutHolder(String v) { this.payoutHolder = v; }
    public String getPayoutBankAcct() { return payoutBankAcct; }
    public void setPayoutBankAcct(String v) { this.payoutBankAcct = v; }
    public String getPayoutIfsc() { return payoutIfsc; }
    public void setPayoutIfsc(String v) { this.payoutIfsc = v; }
    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String v) { this.suspensionReason = v; }
}
