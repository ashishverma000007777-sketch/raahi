package in.raahi.backend.service;

import in.raahi.backend.entity.Job.Status;
import in.raahi.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobStateMachineTest {

    @Test
    void happyPathWithEnRouteIsAllowed() {
        assertTrue(JobStateMachine.canTransition(Status.PENDING, Status.MATCHED));
        assertTrue(JobStateMachine.canTransition(Status.MATCHED, Status.EN_ROUTE));
        assertTrue(JobStateMachine.canTransition(Status.EN_ROUTE, Status.ARRIVED));
        assertTrue(JobStateMachine.canTransition(Status.ARRIVED, Status.IN_PROGRESS));
        assertTrue(JobStateMachine.canTransition(Status.IN_PROGRESS, Status.WORK_DONE));
        assertTrue(JobStateMachine.canTransition(Status.WORK_DONE, Status.COMPLETED));
    }

    @Test
    void directArrivalFromMatchedIsAllowedForBackwardCompatibility() {
        assertTrue(JobStateMachine.canTransition(Status.MATCHED, Status.ARRIVED));
    }

    @Test
    void cancellationStagesAllowed() {
        assertTrue(JobStateMachine.cancellable(Status.PENDING));
        assertTrue(JobStateMachine.cancellable(Status.MATCHED));
        assertTrue(JobStateMachine.cancellable(Status.EN_ROUTE));

        assertTrue(JobStateMachine.canTransition(Status.PENDING, Status.CANCELLED));
        assertTrue(JobStateMachine.canTransition(Status.MATCHED, Status.CANCELLED));
        assertTrue(JobStateMachine.canTransition(Status.EN_ROUTE, Status.CANCELLED));
    }

    @Test
    void helperBackOutReopensToPending() {
        assertTrue(JobStateMachine.canTransition(Status.MATCHED, Status.PENDING));
        assertTrue(JobStateMachine.canTransition(Status.EN_ROUTE, Status.PENDING));
    }

    @Test
    void noNormalCancellationAfterArrival() {
        for (Status s : new Status[]{Status.ARRIVED, Status.IN_PROGRESS, Status.WORK_DONE, Status.COMPLETED}) {
            assertFalse(JobStateMachine.canTransition(s, Status.CANCELLED), s + " must not be cancellable");
            assertFalse(JobStateMachine.cancellable(s));
        }
    }

    @Test
    void stepsCannotBeSkippedOrReversed() {
        assertFalse(JobStateMachine.canTransition(Status.PENDING, Status.EN_ROUTE));
        assertFalse(JobStateMachine.canTransition(Status.PENDING, Status.IN_PROGRESS));
        assertFalse(JobStateMachine.canTransition(Status.MATCHED, Status.COMPLETED));
        assertFalse(JobStateMachine.canTransition(Status.EN_ROUTE, Status.COMPLETED));
        assertFalse(JobStateMachine.canTransition(Status.IN_PROGRESS, Status.COMPLETED));
        assertFalse(JobStateMachine.canTransition(Status.COMPLETED, Status.IN_PROGRESS));
        assertFalse(JobStateMachine.canTransition(Status.CANCELLED, Status.MATCHED));
        assertFalse(JobStateMachine.canTransition(Status.EXPIRED, Status.MATCHED));
    }

    @Test
    void requireThrowsConflictOnInvalidTransition() {
        ApiException e = assertThrows(ApiException.class, () -> JobStateMachine.require(Status.PENDING, Status.COMPLETED));
        assertEquals("INVALID_STATE", e.code);
    }
}
