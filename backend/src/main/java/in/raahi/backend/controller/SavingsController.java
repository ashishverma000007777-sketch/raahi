package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.SavingsDtos.SavingsSummaryDto;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.service.SavingsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/savings")
public class SavingsController {

    private final SavingsService savingsService;

    public SavingsController(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    @GetMapping
    public ApiResponse<SavingsSummaryDto> getSavings(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(savingsService.getSavingsSummary(principal.userId()));
    }
}
