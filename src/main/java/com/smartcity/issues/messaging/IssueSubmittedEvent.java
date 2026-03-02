package com.smartcity.issues.messaging;

import com.smartcity.issues.domain.entity.Issue;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class IssueSubmittedEvent extends ApplicationEvent {

    private final Issue issue;

    public IssueSubmittedEvent(Object source, Issue issue) {
        super(source);
        this.issue = issue;
    }
}
