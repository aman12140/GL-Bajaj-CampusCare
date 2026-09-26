package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.entity.IssueStatus;
import com.glbajaj.campuscare.exception.ApiException;
import org.junit.jupiter.api.Test;

import static com.glbajaj.campuscare.entity.IssueStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit test of the state machine (no Spring, no database). */
class IssueWorkflowTest {

    @Test
    void legalTransitionsAreAllowed() {
        assertThat(IssueWorkflow.canMove(REPORTED, ASSIGNED)).isTrue();
        assertThat(IssueWorkflow.canMove(ASSIGNED, IN_PROGRESS)).isTrue();
        assertThat(IssueWorkflow.canMove(IN_PROGRESS, RESOLVED)).isTrue();
        assertThat(IssueWorkflow.canMove(RESOLVED, CLOSED)).isTrue();
        assertThat(IssueWorkflow.canMove(RESOLVED, REOPENED)).isTrue();
        assertThat(IssueWorkflow.canMove(REOPENED, ASSIGNED)).isTrue();
        assertThat(IssueWorkflow.canMove(ASSIGNED, ASSIGNED)).isTrue();       // reassignment
    }

    @Test
    void illegalTransitionsAreRejected() {
        assertThat(IssueWorkflow.canMove(REPORTED, RESOLVED)).isFalse();
        assertThat(IssueWorkflow.canMove(REPORTED, CLOSED)).isFalse();
        assertThat(IssueWorkflow.canMove(IN_PROGRESS, CLOSED)).isFalse();
        assertThat(IssueWorkflow.canMove(ASSIGNED, RESOLVED)).isFalse();
        assertThatThrownBy(() -> IssueWorkflow.require(REPORTED, RESOLVED)).isInstanceOf(ApiException.class);
    }

    @Test
    void closedAndRejectedAreFinal() {
        for (IssueStatus to : IssueStatus.values()) {
            assertThat(IssueWorkflow.canMove(CLOSED, to)).isFalse();
            assertThat(IssueWorkflow.canMove(REJECTED, to)).isFalse();
        }
    }
}
