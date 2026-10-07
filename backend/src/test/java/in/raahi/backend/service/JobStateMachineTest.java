package in.raahi.backend.service;

import in.raahi.backend.entity.Job.Status;
import in.raahi.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobStateMachineTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(JobStateMachine.canTransition(Status.PENDING, Status.MATCHED));
        assertTrue(JobStateMachine.canTransition(Status.MATCHED, Status.ARRIVED));
        assertTrue(JobStateMachine.canTransition(Status.ARRIVED, Status.IN_PROGRESS));
        assertTrue(JobStateMachine.canTransition(Status.IN_PROGRESS, Status.WORK_DONE));
        assertTrue(JobStateMachine.canTransition(Status.WORK_DONE, Status.COMPLETED));
    }

    @Test
    void noNormalCancellationAfterArrival() {
        for (Status s : new Status[]{Status.ARRIVED, Status.IN_PROGRESS, Status.WORK_DONE, Status.COMPLETED}) {
            assertFalse(JobStateMachine.canTransition(s, Status.CANCELLED), s + " must not be cancellable");
            assertFalse(JobStateMachine.cancellable(s));
        }
        assertTrue(JobStateMachine.cancellable(Status.PENDING));
        assertTrue(JobStateMachine.cancellable(Status.MATCHED));
    }

    @Test
    void stepsCannotBeSkippedOrReversed() {
        assertFalse(JobStateMachine.canTransition(Status.PENDING, Status.IN_PROGRESS));
        assertFalse(JobStateMachine.canTransition(Status.MATCHED, Status.COMPLETED));
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
