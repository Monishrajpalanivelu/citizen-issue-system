package com.smartcity.issues.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueStatus {
    OPEN            ("Open - Awaiting Review"),
    ASSIGNED        ("Assigned to Department"),
    IN_PROGRESS     ("Work In Progress"),
    ON_HOLD         ("On Hold - Pending Info"),
    RESOLVED        ("Resolved"),
    CLOSED          ("Closed"),
    REJECTED        ("Rejected - Invalid Report");

    private final String description;

    public boolean isTerminal() {
        return this == RESOLVED || this == CLOSED || this == REJECTED;
    }

    public boolean canTransitionTo(IssueStatus next) {
        return switch (this) {
            case OPEN       -> next == ASSIGNED    || next == REJECTED;
            case ASSIGNED   -> next == IN_PROGRESS || next == ON_HOLD  || next == REJECTED;
            case IN_PROGRESS-> next == RESOLVED    || next == ON_HOLD;
            case ON_HOLD    -> next == IN_PROGRESS || next == REJECTED;
            case RESOLVED   -> next == CLOSED      || next == OPEN;   // reopen if needed
            default         -> false;
        };
    }
}
