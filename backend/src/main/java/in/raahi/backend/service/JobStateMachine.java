package in.raahi.backend.service;

import in.raahi.backend.entity.Job.Status;
import in.raahi.backend.exception.ApiException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for legal job status transitions.
 * Wire names: PENDING = requested, MATCHED = accepted. COMPLETED + rated flag = rated.
 * There is deliberately NO transition from ARRIVED / IN_PROGRESS / WORK_DONE to CANCELLED:
 * after arrival, customers and helpers use Report Issue / Contact Support instead.
 */
public final class JobStateMachine {

    private static final Map<Status, Set<Status>> ALLOWED = new EnumMap<>(Status.class);

    static {
        ALLOWED.put(Status.PENDING, EnumSet.of(Status.MATCHED, Status.CANCELLED, Status.EXPIRED));
        // MATCHED -> PENDING = the helper backed out before arriving and the request is re-opened.
        ALLOWED.put(Status.MATCHED, EnumSet.of(Status.ARRIVED, Status.CANCELLED, Status.PENDING));
        ALLOWED.put(Status.ARRIVED, EnumSet.of(Status.IN_PROGRESS));
        ALLOWED.put(Status.IN_PROGRESS, EnumSet.of(Status.WORK_DONE));
        ALLOWED.put(Status.WORK_DONE, EnumSet.of(Status.COMPLETED));
        ALLOWED.put(Status.COMPLETED, EnumSet.noneOf(Status.class));
        ALLOWED.put(Status.CANCELLED, EnumSet.noneOf(Status.class));
        ALLOWED.put(Status.EXPIRED, EnumSet.noneOf(Status.class));
    }

    private JobStateMachine() {}

    public static boolean canTransition(Status from, Status to) {
        return from != null && to != null && ALLOWED.get(from).contains(to);
    }

    public static void require(Status from, Status to) {
        if (!canTransition(from, to)) {
            throw ApiException.conflict("INVALID_STATE",
                    "Job cannot move from " + from + " to " + to);
        }
    }

    /** Normal (non-support) cancellation is only possible before the helper has arrived. */
    public static boolean cancellable(Status s) {
        return s == Status.PENDING || s == Status.MATCHED;
    }
}
