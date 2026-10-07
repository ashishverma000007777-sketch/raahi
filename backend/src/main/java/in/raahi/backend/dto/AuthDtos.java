package in.raahi.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class VerifyRequest {
        @NotBlank(message = "idToken is required")
        @Size(max = 4096, message = "idToken exceeds maximum length")
        public String idToken;
        @Size(max = 50)
        public String requestedRole; // optional, defaults to DRIVER server-side; client cannot pick ADMIN/etc
    }

    public static class VerifyResponse {
        public String token;
        public boolean isNewUser;
        public UserDto user;
    }

    public static class UserDto {
        public String id;
        public String name;
        public String phone;
        public String role;
        public String vehicleType;
        public String vehicleReg;
        public boolean isVerified;
        public double ratingAvg;
        public int totalHelps;
    }

    public static class UpdateProfileRequest {
        @Size(max = 100)
        public String name;
        @Size(max = 50)
        public String vehicleType;
        @Size(max = 20)
        public String vehicleReg;
        @Size(max = 10)
        public String language;
    }

    public static class LocationRequest {
        @NotNull(message = "lat is required")
        @DecimalMin(value = "-90.0", message = "lat must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "lat must be between -90 and 90")
        public Double lat;

        @NotNull(message = "lng is required")
        @DecimalMin(value = "-180.0", message = "lng must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "lng must be between -180 and 180")
        public Double lng;
    }

    public static class FcmTokenRequest {
        @NotBlank(message = "token is required")
        @Size(max = 500, message = "FCM token exceeds 500 characters")
        public String token;
    }
}
