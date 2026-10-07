package in.raahi.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommissionMathTest {

    @Test
    void tenPercentOfFiveHundredIsFifty() {
        assertEquals(50.0, CommissionMath.commission(500, 0.10), 0.0001);
    }

    @Test
    void roundsToPaise() {
        assertEquals(12.35, CommissionMath.commission(123.45, 0.10), 0.0001);
    }

    @Test
    void zeroOrNegativeInputsGiveZero() {
        assertEquals(0.0, CommissionMath.commission(0, 0.10), 0.0);
        assertEquals(0.0, CommissionMath.commission(500, 0), 0.0);
        assertEquals(0.0, CommissionMath.commission(-5, 0.10), 0.0);
    }

    @Test
    void negativeBalanceBlocksAndSettlementUnblocks() {
        double balance = -CommissionMath.commission(500, 0.10);   // -50
        assertTrue(CommissionMath.owesMoney(balance));
        balance += 50;                                              // settlement
        assertFalse(CommissionMath.owesMoney(balance));
    }

    @Test
    void partialSettlementStillBlocks() {
        assertTrue(CommissionMath.owesMoney(-50 + 20));
    }
}
