package com.smartcity.issues.service;

import com.smartcity.issues.domain.entity.*;
import com.smartcity.issues.domain.enums.*;
import com.smartcity.issues.domain.repository.*;
import com.smartcity.issues.messaging.IssueStatusChangedEvent;
import com.smartcity.issues.messaging.IssueSubmittedEvent;
import com.smartcity.issues.web.dto.Dtos.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IssueService {

    private final IssueRepository              issueRepo;
    private final UserRepository               userRepo;
    private final DepartmentRepository         deptRepo;
    private final DistrictRepository           districtRepo;
    private final IssueStatusHistoryRepository historyRepo;
    private final ApplicationEventPublisher    eventPublisher;

    // ── Submit a new issue ─────────────────────────────────────────

    public IssueResponse submitIssue(CreateIssueRequest req, Long userId) {
        User reporter = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Issue issue = Issue.builder()
            .title(req.getTitle())
            .description(req.getDescription())
            .category(req.getCategory())
            .status(IssueStatus.OPEN)
            .priority(Priority.fromCategory(req.getCategory()))  // auto-score
            .address(req.getAddress())
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .reportedBy(reporter)
            .build();

        // Set district if provided
        if (req.getDistrictId() != null) {
            districtRepo.findById(req.getDistrictId()).ifPresent(issue::setDistrict);
        }

        // Add media URLs
        if (req.getMediaUrls() != null) {
            List<IssueMedia> media = req.getMediaUrls().stream()
                .map(url -> IssueMedia.builder().issue(issue).url(url).build())
                .collect(Collectors.toList());
            issue.getMediaFiles().addAll(media);
        }

        Issue saved = issueRepo.save(issue);
        log.info("Issue #{} submitted by user {} — category={}, priority={}",
                 saved.getId(), userId, saved.getCategory(), saved.getPriority());

        // Record initial status history
        historyRepo.save(IssueStatusHistory.builder()
            .issue(saved)
            .newStatus(IssueStatus.OPEN)
            .changedBy(reporter)
            .notes("Issue submitted by citizen")
            .build());

        // Publish event → triggers DepartmentRoutingService + NotificationService
        eventPublisher.publishEvent(new IssueSubmittedEvent(this, saved));

        return toDetailResponse(saved);
    }

    // ── Update issue status ────────────────────────────────────────

    public IssueResponse updateStatus(Long issueId, UpdateStatusRequest req, Long userId) {
        Issue issue = getIssueById(issueId);
        User  changer = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        IssueStatus oldStatus = issue.getStatus();
        IssueStatus newStatus = req.getStatus();

        // Validate transition
        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Cannot transition from " + oldStatus + " to " + newStatus
            );
        }

        issue.setStatus(newStatus);

        if (newStatus == IssueStatus.RESOLVED) {
            issue.setResolvedAt(LocalDateTime.now());
            issue.setResolutionNotes(req.getNotes());
        }

        issueRepo.save(issue);

        // Record history
        historyRepo.save(IssueStatusHistory.builder()
            .issue(issue)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .changedBy(changer)
            .notes(req.getNotes())
            .build());

        log.info("Issue #{} status changed: {} → {} by user {}",
                 issueId, oldStatus, newStatus, userId);

        // Fire event → NotificationService sends emails
        eventPublisher.publishEvent(new IssueStatusChangedEvent(this, issue, oldStatus, newStatus, changer));

        return toDetailResponse(issue);
    }

    // ── Assign issue to department/staff ──────────────────────────

    public IssueResponse assignIssue(Long issueId, AssignIssueRequest req, Long adminId) {
        Issue issue   = getIssueById(issueId);
        User  admin   = userRepo.findById(adminId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Department dept = deptRepo.findById(req.getDepartmentId())
            .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        IssueStatus oldStatus = issue.getStatus();
        issue.setAssignedDepartment(dept);
        issue.setStatus(IssueStatus.ASSIGNED);

        if (req.getStaffId() != null) {
            userRepo.findById(req.getStaffId()).ifPresent(issue::setAssignedTo);
        }

        issueRepo.save(issue);

        historyRepo.save(IssueStatusHistory.builder()
            .issue(issue)
            .oldStatus(oldStatus)
            .newStatus(IssueStatus.ASSIGNED)
            .changedBy(admin)
            .notes(req.getNotes() != null ? req.getNotes()
                   : "Manually assigned to " + dept.getName())
            .build());

        eventPublisher.publishEvent(
            new IssueStatusChangedEvent(this, issue, oldStatus, IssueStatus.ASSIGNED, admin)
        );

        return toDetailResponse(issue);
    }

    // ── Queries ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IssueResponse getIssue(Long issueId) {
        return toDetailResponse(getIssueById(issueId));
    }

    @Transactional(readOnly = true)
    public Page<IssueSummaryResponse> getAllIssues(
            IssueStatus status, IssueCategory category, Pageable pageable) {

        Page<Issue> issues;
        if (status != null && category != null) {
            issues = issueRepo.findByStatusAndCategory(status, category, pageable);
        } else if (status != null) {
            issues = issueRepo.findByStatus(status, pageable);
        } else if (category != null) {
            issues = issueRepo.findByCategory(category, pageable);
        } else {
            issues = issueRepo.findAll(pageable);
        }
        return issues.map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<IssueSummaryResponse> getMyIssues(Long userId, Pageable pageable) {
        return issueRepo.findByReportedById(userId, pageable).map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<IssueSummaryResponse> getDepartmentIssues(
            Long deptId, IssueStatus status, Pageable pageable) {
        Page<Issue> issues = status != null
            ? issueRepo.findByAssignedDepartmentIdAndStatus(deptId, status, pageable)
            : issueRepo.findByAssignedDepartmentId(deptId, pageable);
        return issues.map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        List<CategoryStat> byCategory = issueRepo.countGroupByCategory().stream()
            .map(r -> new CategoryStat(r[0].toString(), (Long) r[1]))
            .collect(Collectors.toList());

        List<StatusStat> byStatus = issueRepo.countGroupByStatus().stream()
            .map(r -> new StatusStat(r[0].toString(), (Long) r[1]))
            .collect(Collectors.toList());

        List<DistrictStat> byDistrict = issueRepo.countGroupByDistrict().stream()
            .map(r -> new DistrictStat(r[0].toString(), (Long) r[1]))
            .collect(Collectors.toList());

        return DashboardStats.builder()
            .totalIssues(issueRepo.count())
            .openIssues(issueRepo.countByStatus(IssueStatus.OPEN))
            .inProgressIssues(issueRepo.countByStatus(IssueStatus.IN_PROGRESS))
            .resolvedIssues(issueRepo.countByStatus(IssueStatus.RESOLVED))
            .criticalIssues(issueRepo.countByPriority(Priority.CRITICAL))
            .byCategory(byCategory)
            .byStatus(byStatus)
            .byDistrict(byDistrict)
            .build();
    }

    // ── Helpers ────────────────────────────────────────────────────

    private Issue getIssueById(Long id) {
        return issueRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Issue #" + id + " not found"));
    }

    private IssueResponse toDetailResponse(Issue i) {
        List<StatusHistoryResponse> history = historyRepo
            .findByIssueIdOrderByChangedAtAsc(i.getId()).stream()
            .map(h -> StatusHistoryResponse.builder()
                .oldStatus(h.getOldStatus())
                .newStatus(h.getNewStatus())
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System")
                .notes(h.getNotes())
                .changedAt(h.getChangedAt())
                .build())
            .collect(Collectors.toList());

        List<String> mediaUrls = i.getMediaFiles().stream()
            .map(IssueMedia::getUrl)
            .collect(Collectors.toList());

        return IssueResponse.builder()
            .id(i.getId())
            .title(i.getTitle())
            .description(i.getDescription())
            .category(i.getCategory())
            .categoryDisplayName(i.getCategory().getDisplayName())
            .status(i.getStatus())
            .statusDescription(i.getStatus().getDescription())
            .priority(i.getPriority())
            .address(i.getAddress())
            .latitude(i.getLatitude())
            .longitude(i.getLongitude())
            .districtName(i.getDistrict() != null ? i.getDistrict().getName() : null)
            .reportedByName(i.getReportedBy().getFullName())
            .assignedDepartmentName(i.getAssignedDepartment() != null
                ? i.getAssignedDepartment().getName() : null)
            .assignedToName(i.getAssignedTo() != null
                ? i.getAssignedTo().getFullName() : null)
            .createdAt(i.getCreatedAt())
            .updatedAt(i.getUpdatedAt())
            .resolvedAt(i.getResolvedAt())
            .resolutionNotes(i.getResolutionNotes())
            .mediaUrls(mediaUrls)
            .history(history)
            .build();
    }

    private IssueSummaryResponse toSummaryResponse(Issue i) {
        return IssueSummaryResponse.builder()
            .id(i.getId())
            .title(i.getTitle())
            .category(i.getCategory())
            .status(i.getStatus())
            .priority(i.getPriority())
            .districtName(i.getDistrict() != null ? i.getDistrict().getName() : null)
            .createdAt(i.getCreatedAt())
            .build();
    }
}
