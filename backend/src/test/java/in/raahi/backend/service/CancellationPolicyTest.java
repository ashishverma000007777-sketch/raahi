package in.raahi.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CancellationPolicyTest {

    @Test
    void firstViolationIsOneDayBlock() {
        assertEquals(1L, CancellationPolicyService.blockDaysForViolation(1));
    }

    @Test
    void repeatViolationEscalates() {
        assertEquals(7L, CancellationPolicyService.blockDaysForViolation(2));
        assertNull(CancellationPolicyService.blockDaysForViolation(3), "third violation suspends the account");
    }
}
