package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.entity.IssueStatus;
import com.glbajaj.campuscare.exception.ApiException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.glbajaj.campuscare.entity.IssueStatus.*;

/**
 * The issue state machine - the ONLY place that knows which status changes are legal.
 *
 *   REPORTED -> ASSIGNED | REJECTED           (admin)
 *   ASSIGNED -> IN_PROGRESS                   (assigned staff / admin)
 *   ASSIGNED -> ASSIGNED                      (admin reassigns to another staff member)
 *   IN_PROGRESS -> RESOLVED                   (assigned staff / admin, needs a resolution note)
 *   IN_PROGRESS -> ASSIGNED                   (admin reassigns, e.g. because the staff member left)
 *   RESOLVED -> CLOSED                        (ONLY the student, by confirming)
 *   RESOLVED -> REOPENED                      (ONLY the student, with a reason)
 *   REOPENED -> ASSIGNED | REJECTED           (admin)
 *   CLOSED, REJECTED                          (final)
 */
public final class IssueWorkflow {
    private static final Map<IssueStatus, Set<IssueStatus>> ALLOWED = new EnumMap<>(IssueStatus.class);

    static {
        ALLOWED.put(REPORTED, EnumSet.of(ASSIGNED, REJECTED));
        ALLOWED.put(ASSIGNED, EnumSet.of(IN_PROGRESS, ASSIGNED));
        ALLOWED.put(IN_PROGRESS, EnumSet.of(RESOLVED, ASSIGNED));
        ALLOWED.put(RESOLVED, EnumSet.of(CLOSED, REOPENED));
        ALLOWED.put(REOPENED, EnumSet.of(ASSIGNED, REJECTED));
        ALLOWED.put(CLOSED, EnumSet.noneOf(IssueStatus.class));
        ALLOWED.put(REJECTED, EnumSet.noneOf(IssueStatus.class));
    }

    private IssueWorkflow() {}

    public static boolean canMove(IssueStatus from, IssueStatus to) { return ALLOWED.get(from).contains(to); }

    public static void require(IssueStatus from, IssueStatus to) {
        if (!canMove(from, to)) {
            throw ApiException.badRequest("Invalid status change: " + from.name().replace('_', ' ') + " \u2192 " + to.name().replace('_', ' ') + ".");
        }
    }
}
