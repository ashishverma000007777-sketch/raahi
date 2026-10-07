package in.raahi.backend.dto;

public class HelperDtos {

    public static class HelperApplicationDto {
        public String id;
        public String status;
        public String email;
        public String rejectionReason;
        public String submittedAt;
        public String reviewedAt;
        public DocSummary aadhaarFront;
        public DocSummary aadhaarBack;
        public DocSummary selfie;
        // Only populated for ADMIN callers reviewing the queue — a helper checking their own
        // application status doesn't need to see or identify themselves by these fields again.
        public String applicantName;
        public String applicantPhone;
        public String userId;
        public String suspensionReason;
        // Onboarding details (owner + admin only)
        public Integer experienceYears;
        public String serviceArea;
        public String services;
        public String equipment;
        public String vehicleType;
        public String vehicleBrand;
        public String vehicleModel;
        public String vehicleVariant;
        public String vehicleReg;
        public Integer serviceRadiusKm;
        public String payoutUpi;
        public String payoutHolder;
        public String payoutBankAcctMasked; // last 4 digits only
        public String payoutIfsc;
    }

    public static class ReasonRequest {
        @jakarta.validation.constraints.NotBlank(message = "reason is required")
        @jakarta.validation.constraints.Size(max = 500, message = "reason exceeds 500 characters")
        public String reason;
    }

    public static class AvailabilityRequest {
        @jakarta.validation.constraints.NotNull(message = "online is required")
        public Boolean online;
        @jakarta.validation.constraints.DecimalMin("-90.0") @jakarta.validation.constraints.DecimalMax("90.0")
        public Double lat;
        @jakarta.validation.constraints.DecimalMin("-180.0") @jakarta.validation.constraints.DecimalMax("180.0")
        public Double lng;
    }

    public static class SettleRequest {
        @jakarta.validation.constraints.NotBlank(message = "helperId is required")
        public String helperId;
        @jakarta.validation.constraints.NotNull(message = "amount is required")
        @jakarta.validation.constraints.DecimalMin(value = "0.01", message = "amount must be positive")
        public Double amount;
        @jakarta.validation.constraints.NotBlank(message = "method is required")
        @jakarta.validation.constraints.Pattern(regexp = "^(UPI|BANK|CASH)$", message = "method must be UPI, BANK or CASH")
        public String method;
        @jakarta.validation.constraints.Size(max = 100)
        public String reference;
    }

    public static class BlockRequest {
        @jakarta.validation.constraints.NotBlank(message = "reason is required")
        @jakarta.validation.constraints.Size(max = 500)
        public String reason;
        // null = until an admin unblocks (account SUSPENDED); otherwise a timed block in days.
        @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(365)
        public Integer days;
    }

    public static class ResolveReportRequest {
        @jakarta.validation.constraints.NotBlank(message = "resolution is required")
        @jakarta.validation.constraints.Size(max = 500)
        public String resolution;
    }

    public static class ExcuseRequest {
        @jakarta.validation.constraints.NotBlank(message = "note is required")
        @jakarta.validation.constraints.Size(max = 300)
        public String note;
    }

    public static class DocSummary {
        public String id;
        public String aiStatus;
        public String aiNote;
    }

    public static class RejectRequest {
        @jakarta.validation.constraints.NotBlank(message = "reason is required")
        @jakarta.validation.constraints.Size(max = 500, message = "reason exceeds 500 characters")
        public String reason;
    }
}
