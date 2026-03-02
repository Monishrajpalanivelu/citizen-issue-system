package com.smartcity.issues.service;

import com.smartcity.issues.domain.entity.Department;
import com.smartcity.issues.domain.entity.Issue;
import com.smartcity.issues.domain.entity.IssueStatusHistory;
import com.smartcity.issues.domain.entity.User;
import com.smartcity.issues.domain.enums.IssueStatus;
import com.smartcity.issues.domain.repository.DepartmentRepository;
import com.smartcity.issues.domain.repository.IssueRepository;
import com.smartcity.issues.domain.repository.IssueStatusHistoryRepository;
import com.smartcity.issues.domain.repository.UserRepository;
import com.smartcity.issues.messaging.IssueSubmittedEvent;
import com.smartcity.issues.messaging.IssueStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentRoutingService {

    private final IssueRepository        issueRepo;
    private final DepartmentRepository   deptRepo;
    private final UserRepository         userRepo;
    private final IssueStatusHistoryRepository historyRepo;
    private final ApplicationEventPublisher    eventPublisher;

    @Value("${app.routing.auto-assign:true}")
    private boolean autoAssign;

    /**
     * Listens for new issue submissions and routes them to the
     * appropriate department based on IssueCategory.departmentCode.
     */
    @Async
    @EventListener
    @Transactional
    public void onIssueSubmitted(IssueSubmittedEvent event) {
        Issue issue = event.getIssue();

        if (!autoAssign) {
            log.info("Auto-assign disabled. Issue {} left unassigned.", issue.getId());
            return;
        }

        String deptCode = issue.getCategory().getDepartmentCode();
        log.info("Routing issue {} (category={}) → department code={}",
                 issue.getId(), issue.getCategory(), deptCode);

        Optional<Department> dept = deptRepo.findByCode(deptCode);

        if (dept.isEmpty()) {
            log.warn("No department found for code {}. Issue {} remains OPEN.", deptCode, issue.getId());
            return;
        }

        // Fetch fresh from DB to avoid stale state in async context
        Issue freshIssue = issueRepo.findById(issue.getId()).orElse(null);
        if (freshIssue == null) return;

        IssueStatus oldStatus = freshIssue.getStatus();

        freshIssue.setAssignedDepartment(dept.get());
        freshIssue.setStatus(IssueStatus.ASSIGNED);
        issueRepo.save(freshIssue);

        // Record status history
        User systemUser = userRepo.findByEmail("admin@smartcity.gov").orElse(null);
        historyRepo.save(IssueStatusHistory.builder()
            .issue(freshIssue)
            .oldStatus(oldStatus)
            .newStatus(IssueStatus.ASSIGNED)
            .changedBy(systemUser)
            .notes("Auto-routed to " + dept.get().getName() + " department")
            .build());

        log.info("Issue {} assigned to department: {}", freshIssue.getId(), dept.get().getName());

        // Fire status change event for notification
        eventPublisher.publishEvent(new IssueStatusChangedEvent(
            this, freshIssue, oldStatus, IssueStatus.ASSIGNED, systemUser
        ));
    }
}
