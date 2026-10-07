package in.raahi.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CommissionMath {
    private CommissionMath() {}

    /** commission = amount * rate, rounded half-up to paise. 500 @ 10% -> 50.00 */
    public static double commission(double jobAmount, double rate) {
        if (jobAmount <= 0 || rate <= 0) return 0.0;
        return BigDecimal.valueOf(jobAmount).multiply(BigDecimal.valueOf(rate))
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** A helper owes Raahi when the ledger balance is below zero (ignoring sub-paisa noise). */
    public static boolean owesMoney(double balance) {
        return round2(balance) < 0.0;
    }
}
