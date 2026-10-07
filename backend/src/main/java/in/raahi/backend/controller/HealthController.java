package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "raahi-backend"
        ));
    }
}
