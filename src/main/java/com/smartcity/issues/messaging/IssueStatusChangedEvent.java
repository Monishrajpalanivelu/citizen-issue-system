package com.smartcity.issues.messaging;

import com.smartcity.issues.domain.entity.Issue;
import com.smartcity.issues.domain.entity.User;
import com.smartcity.issues.domain.enums.IssueStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class IssueStatusChangedEvent extends ApplicationEvent {

    private final Issue issue;
    private final IssueStatus oldStatus;
    private final IssueStatus newStatus;
    private final User changedBy;

    public IssueStatusChangedEvent(Object source, Issue issue,
                                    IssueStatus oldStatus, IssueStatus newStatus,
                                    User changedBy) {
        super(source);
        this.issue     = issue;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
    }
}
