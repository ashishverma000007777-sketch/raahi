package in.raahi.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class JobDtos {

    public static class CreateJobRequest {
        @NotBlank(message = "problemType is required")
        @Size(max = 50, message = "problemType exceeds 50 characters")
        public String problemType;

        @Size(max = 1000, message = "problemDesc exceeds 1000 characters")
        public String problemDesc;

        @NotNull(message = "lat is required")
        @DecimalMin(value = "-90.0", message = "lat must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "lat must be between -90 and 90")
        public Double lat;

        @NotNull(message = "lng is required")
        @DecimalMin(value = "-180.0", message = "lng must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "lng must be between -180 and 180")
        public Double lng;

        @Size(max = 100, message = "highwayName exceeds 100 characters")
        public String highwayName;

        @DecimalMin(value = "0.0", message = "rewardAmount cannot be negative")
        @DecimalMax(value = "50000.0", message = "rewardAmount cannot exceed 50,000")
        public Double rewardAmount = 0.0;
    }

    public static class VerifyOtpRequest {
        @NotBlank(message = "otp is required")
        @Pattern(regexp = "^\\d{4,8}$", message = "OTP must be 4 to 8 digits")
        public String otp;
    }

    public static class JobDto {
        public String id;
        public String status;
        public String problemType;
        public String problemDesc;
        public Double lat;
        public Double lng;
        public Double rewardAmount;
        public String helperOtp; // only populated for the requester, stripped for anyone else
        public String requesterName;
        public String requesterPhone; // only populated for the assigned helper — see toDto
        public String helperName;
        public String helperPhone; // only populated for the requester
        public Double helperRatingAvg;
        public Integer helperTotalHelps;
        public Boolean helperVerified;
        // "REQUESTER" or "HELPER" — computed server-side so the Android client never has to
        // guess which role it's viewing a job as (e.g. by string-matching names) when
        // rendering /jobs/mine, which mixes both.
        public String viewerRole;

        // ---- lifecycle details ----
        public Boolean cancelAllowed;      // false from ARRIVED onward: use Report Issue / Contact Support
        public String arrivalMethod;       // GPS / OTP / GPS_OTP
        public String arrivedAt;
        public Double finalAmount;
        public String paymentMode;
        public Boolean rated;
        public String completionOtp;       // only for the requester while WORK_DONE
        public Double commissionAmount;    // only for the helper once COMPLETED
        public Double distanceKm;          // only on the open-requests list
        public String createdAt;
        public Double helperLat;
        public Double helperLng;
        public Integer nearbyHelperCount;

        // ---- en-route and cancellation protection details ----
        public String enRouteAt;
        public String cancellationReasonCode;
        public String cancellationStage;
        public Double travelLossEstimate;
        public Double travelLossApproved;
        public String travelLossStatus; // AUTO_APPROVED, ELIGIBLE_PENDING_REVIEW, ADMIN_APPROVED, REJECTED, NOT_ELIGIBLE
        public String payoutStatus; // UNPAID, PENDING_PAYOUT, PAID, FAILED
        public Double travelDistanceKm;
        public String travelEvidenceType; // GPS_VERIFIED, ESTIMATED_DISPATCH, UNVERIFIED
        public String travelLossNotes;
    }

    public static class LocationUpdateRequest {
        @NotNull(message = "lat is required")
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        public Double lat;

        @NotNull(message = "lng is required")
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        public Double lng;
    }

    public static class ArriveRequest {
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        public Double lat;
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        public Double lng;
        @Pattern(regexp = "^\\d{4,8}$", message = "OTP must be 4 to 8 digits")
        public String otp;
    }

    public static class WorkDoneRequest {
        @DecimalMin(value = "0.0", message = "finalAmount cannot be negative")
        @DecimalMax(value = "50000.0", message = "finalAmount cannot exceed 50,000")
        public Double finalAmount;
        @NotBlank(message = "paymentMode is required")
        @Pattern(regexp = "^(CASH|UPI)$", message = "paymentMode must be CASH or UPI")
        public String paymentMode;
    }

    public static class RateRequest {
        @NotNull(message = "stars is required")
        @jakarta.validation.constraints.Min(value = 1, message = "stars must be 1 to 5")
        @jakarta.validation.constraints.Max(value = 5, message = "stars must be 1 to 5")
        public Integer stars;
        @Size(max = 500, message = "comment exceeds 500 characters")
        public String comment;
    }

    public static class CancelRequest {
        @Size(max = 200, message = "reason exceeds 200 characters")
        public String reason;

        @Pattern(regexp = "^(CUSTOMER_CANCELLED|HELPER_CANCELLED|CUSTOMER_UNREACHABLE|INCORRECT_LOCATION|VEHICLE_EMERGENCY|UNSAFE_CONDITIONS|OTHER)?$",
                message = "Invalid reasonCode")
        public String reasonCode;

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        public Double lat;

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        public Double lng;
    }

    public static class CompensationDto {
        public String id;
        public String jobId;
        public String helperId;
        public String helperName;
        public String helperPhone;
        public String requesterId;
        public String requesterName;
        public String requesterPhone;
        public String cancellationStage;
        public String reasonCode;
        public String reasonText;
        public String enRouteAt;
        public String cancelledAt;
        public Double oneWayDistanceKm;
        public Double actualTravelKm;
        public Double returnDistanceKm;
        public String evidenceType;
        public Double estimatedLossAmount;
        public Double approvedAmount;
        public String status;
        public String eligibilityNotes;
        public String policyVersion;
        public Double baseCalloutFee;
        public Double baseRatePerKm;
        public Double returnFactor;
        public String payoutStatus;
        public String payoutReference;
        public String payoutAt;
        public String payoutNotes;
        public String reviewedBy;
        public String reviewedAt;
        public String reviewNotes;
        public String createdAt;
    }

    public static class ApproveCompensationRequest {
        @DecimalMin(value = "0.0", message = "approvedAmount cannot be negative")
        @DecimalMax(value = "50000.0", message = "approvedAmount exceeds maximum allowable")
        public Double approvedAmount;

        @Size(max = 500, message = "note exceeds 500 characters")
        public String note;
    }

    public static class RejectCompensationRequest {
        @NotBlank(message = "note is required")
        @Size(max = 500, message = "note exceeds 500 characters")
        public String note;
    }

    public static class ReportRequest {
        @NotBlank(message = "message is required")
        @Size(max = 1000, message = "message exceeds 1000 characters")
        public String message;
        @Pattern(regexp = "^(ISSUE|DISPUTE|COMPLAINT)$", message = "kind must be ISSUE, DISPUTE or COMPLAINT")
        public String kind = "ISSUE";
    }
}
