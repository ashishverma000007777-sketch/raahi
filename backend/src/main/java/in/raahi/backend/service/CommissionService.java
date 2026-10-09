package in.raahi.backend.service;

import in.raahi.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Commission / settlement LEDGER (not a stored-money wallet). Raahi never holds the customer's
 * service money: the customer pays the helper directly, and the helper owes Raahi a commission.
 *   job 500, rate 10%  ->  ledger row COMMISSION -50   -> balance -50  -> cannot accept jobs
 *   settlement of 50   ->  ledger row SETTLEMENT +50   -> balance   0  -> can accept jobs again
 */
@Service
public class CommissionService {

    private final JdbcTemplate jdbc;
    private final double rate;

    public CommissionService(JdbcTemplate jdbc, @Value("${raahi.commission.rate:0.10}") double rate) {
        this.jdbc = jdbc;
        this.rate = rate;
    }

    public double rate() {
        return rate;
    }

    public double balance(UUID helperId) {
        Double sum = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM commission_ledger WHERE helper_id = ?", Double.class, helperId);
        return CommissionMath.round2(sum == null ? 0.0 : sum);
    }

    public static final double OUTSTANDING_LIMIT = 50.0;

    public boolean canAcceptWithBalance(double balance) {
        return CommissionMath.round2(balance) >= -OUTSTANDING_LIMIT;
    }

    public void requireNoDue(UUID helperId) {
        double bal = balance(helperId);
        if (!canAcceptWithBalance(bal)) {
            throw ApiException.forbidden("COMMISSION_LIMIT_REACHED",
                    "Outstanding Raahi commission is ₹" + Math.abs(bal)
                            + ". Settle enough to bring it within ₹50 before accepting new jobs.");
        }
    }
    /** Returns the commission amount recorded (positive number). Idempotent per job (unique index). */
    public double recordCommission(UUID helperId, UUID jobId, double jobAmount, String paymentMode) {
        double commission = CommissionMath.commission(jobAmount, rate);
        if (commission <= 0) return 0.0;
        jdbc.update("INSERT INTO commission_ledger(helper_id, job_id, entry_type, amount, job_amount, commission_rate, payment_mode) VALUES (?,?, 'COMMISSION', ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                helperId, jobId, -commission, jobAmount, rate, paymentMode);
        return commission;
    }

    public void settle(UUID helperId, double amount, String method, String reference, UUID recordedBy) {
        if (amount <= 0 || amount > 1_000_000) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Settlement amount must be between 0 and 10,00,000");
        }
        jdbc.update("INSERT INTO commission_ledger(helper_id, entry_type, amount, payment_mode, reference, recorded_by) VALUES (?, 'SETTLEMENT', ?, ?, ?, ?)",
                helperId, CommissionMath.round2(amount), method, reference, recordedBy);
    }

    public List<Map<String, Object>> entries(UUID helperId, int limit) {
        return jdbc.queryForList("SELECT id, job_id, entry_type, amount, job_amount, commission_rate, payment_mode, reference, created_at FROM commission_ledger WHERE helper_id = ? ORDER BY created_at DESC LIMIT ?", helperId, limit);
    }

    public List<Map<String, Object>> balances(int limit) {
        return jdbc.queryForList("SELECT l.helper_id, u.name, u.phone, ROUND(CAST(SUM(l.amount) AS numeric), 2) AS balance FROM commission_ledger l JOIN users u ON u.id = l.helper_id GROUP BY l.helper_id, u.name, u.phone ORDER BY SUM(l.amount) ASC LIMIT ?", limit);
    }

    public List<Map<String, Object>> allEntries(UUID helperIdOrNull, int limit) {
        if (helperIdOrNull == null) {
            return jdbc.queryForList("SELECT id, helper_id, job_id, entry_type, amount, job_amount, commission_rate, payment_mode, reference, recorded_by, created_at FROM commission_ledger ORDER BY created_at DESC LIMIT ?", limit);
        }
        return jdbc.queryForList("SELECT id, helper_id, job_id, entry_type, amount, job_amount, commission_rate, payment_mode, reference, recorded_by, created_at FROM commission_ledger WHERE helper_id = ? ORDER BY created_at DESC LIMIT ?", helperIdOrNull, limit);
    }
}
