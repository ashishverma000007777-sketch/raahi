package in.raahi.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class MechanicDtos {

    public static class MechanicDto {
        public String userId;
        public String name;
        public String phone;
        public String shopName;
        public String specializations;
        public boolean isAvailable;
        public Double lat;
        public Double lng;
        public double distanceKm;
        public double ratingAvg;
    }

    public static class UpdateLocationRequest {
        @NotNull(message = "lat is required")
        @DecimalMin(value = "-90.0", message = "lat must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "lat must be between -90 and 90")
        public Double lat;

        @NotNull(message = "lng is required")
        @DecimalMin(value = "-180.0", message = "lng must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "lng must be between -180 and 180")
        public Double lng;
    }
}
