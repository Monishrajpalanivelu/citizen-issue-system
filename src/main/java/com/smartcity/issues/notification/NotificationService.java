package com.smartcity.issues.notification;

import com.smartcity.issues.domain.entity.Department;
import com.smartcity.issues.domain.entity.Issue;
import com.smartcity.issues.domain.enums.IssueStatus;
import com.smartcity.issues.messaging.IssueStatusChangedEvent;
import com.smartcity.issues.messaging.IssueSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.notification.from-email}")
    private String fromEmail;

    @Value("${app.notification.admin-email}")
    private String adminEmail;

    // ── Issue Submitted: notify citizen + department ────────────────

    @Async
    @EventListener
    public void onIssueSubmitted(IssueSubmittedEvent event) {
        Issue issue = event.getIssue();
        log.info("Sending submission notification for issue #{}", issue.getId());

        // 1. Confirm to citizen
        sendEmail(
            issue.getReportedBy().getEmail(),
            "✅ Issue Reported: #" + issue.getId() + " – " + issue.getTitle(),
            buildCitizenConfirmationEmail(issue)
        );

        // 2. Alert admin
        sendEmail(
            adminEmail,
            "🔔 New Issue Reported: #" + issue.getId() + " [" + issue.getPriority() + "]",
            buildAdminAlertEmail(issue)
        );
    }

    // ── Status Changed: notify citizen + department ─────────────────

    @Async
    @EventListener
    public void onStatusChanged(IssueStatusChangedEvent event) {
        Issue issue    = event.getIssue();
        IssueStatus to = event.getNewStatus();

        log.info("Sending status update notification for issue #{}: {} → {}",
                 issue.getId(), event.getOldStatus(), to);

        // Notify citizen of any meaningful status change
        if (to != IssueStatus.OPEN) {
            sendEmail(
                issue.getReportedBy().getEmail(),
                "📋 Update on Your Issue #" + issue.getId() + " – " + to.getDescription(),
                buildStatusUpdateEmail(issue, event.getOldStatus(), to)
            );
        }

        // Notify department when assigned
        if (to == IssueStatus.ASSIGNED && issue.getAssignedDepartment() != null) {
            Department dept = issue.getAssignedDepartment();
            sendEmail(
                dept.getEmail(),
                "📥 New Issue Assigned: #" + issue.getId() + " [" + issue.getPriority() + "]",
                buildDepartmentAssignmentEmail(issue, dept)
            );
        }

        // Escalate critical issues
        if (issue.getPriority().getLevel() == 4 && to == IssueStatus.ASSIGNED) {
            sendEmail(
                adminEmail,
                "🚨 CRITICAL Issue Assigned: #" + issue.getId(),
                "CRITICAL priority issue has been assigned.\n\n" + buildAdminAlertEmail(issue)
            );
        }
    }

    // ── Email templates ─────────────────────────────────────────────

    private String buildCitizenConfirmationEmail(Issue issue) {
        return """
            Dear %s,
            
            Thank you for reporting an issue in your community!
            
            📋 Issue Details:
            ─────────────────────────────────
            Issue ID    : #%d
            Title       : %s
            Category    : %s
            Priority    : %s
            Status      : %s
            Location    : %s
            ─────────────────────────────────
            
            Your report has been received and will be reviewed shortly.
            We will notify you as your issue progresses.
            
            Track your issue online or contact us at %s.
            
            Thank you for helping improve our city!
            
            Smart City Operations Team
            """.formatted(
                issue.getReportedBy().getFullName(),
                issue.getId(),
                issue.getTitle(),
                issue.getCategory().getDisplayName(),
                issue.getPriority(),
                issue.getStatus().getDescription(),
                issue.getAddress() != null ? issue.getAddress() : "Not specified",
                fromEmail
        );
    }

    private String buildAdminAlertEmail(Issue issue) {
        return """
            New issue has been reported.
            
            Issue ID  : #%d
            Title     : %s
            Category  : %s
            Priority  : %s (%s)
            Reporter  : %s (%s)
            Location  : %s
            District  : %s
            
            Please review and ensure routing is correct.
            
            — Smart City System
            """.formatted(
                issue.getId(),
                issue.getTitle(),
                issue.getCategory().getDisplayName(),
                issue.getPriority(),
                issue.getPriority().getDescription(),
                issue.getReportedBy().getFullName(),
                issue.getReportedBy().getEmail(),
                issue.getAddress() != null ? issue.getAddress() : "N/A",
                issue.getDistrict() != null ? issue.getDistrict().getName() : "N/A"
        );
    }

    private String buildStatusUpdateEmail(Issue issue, IssueStatus from, IssueStatus to) {
        String closing = to == IssueStatus.RESOLVED
            ? "\nYour issue has been resolved! If the problem persists, please submit a new report."
            : "\nWe appreciate your patience as we work to resolve this issue.";

        return """
            Dear %s,
            
            Your issue #%d has been updated.
            
            Previous Status : %s
            New Status      : %s
            
            Issue: %s
            %s
            
            Thank you for your patience.
            Smart City Operations Team
            """.formatted(
                issue.getReportedBy().getFullName(),
                issue.getId(),
                from.getDescription(),
                to.getDescription(),
                issue.getTitle(),
                closing
        );
    }

    private String buildDepartmentAssignmentEmail(Issue issue, Department dept) {
        return """
            A new issue has been assigned to the %s department.
            
            Issue ID   : #%d
            Title      : %s
            Category   : %s
            Priority   : %s — %s
            Location   : %s
            Description:
            %s
            
            Please log in to the system to accept and begin work on this issue.
            
            — Smart City System
            """.formatted(
                dept.getName(),
                issue.getId(),
                issue.getTitle(),
                issue.getCategory().getDisplayName(),
                issue.getPriority(),
                issue.getPriority().getDescription(),
                issue.getAddress() != null ? issue.getAddress() : "See coordinates in system",
                issue.getDescription()
        );
    }

    // ── Mail sender ─────────────────────────────────────────────────

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {} | Subject: {}", to, subject);
        } catch (Exception e) {
            // Don't let notification failure break the main flow
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
