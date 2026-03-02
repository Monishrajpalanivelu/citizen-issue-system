package com.smartcity.issues.web.controller;

import com.smartcity.issues.domain.entity.User;
import com.smartcity.issues.domain.enums.IssueCategory;
import com.smartcity.issues.domain.enums.IssueStatus;
import com.smartcity.issues.service.IssueService;
import com.smartcity.issues.web.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/issues")
@RequiredArgsConstructor
@Tag(name = "Issues", description = "Citizen issue submission and tracking")
@SecurityRequirement(name = "bearerAuth")
public class IssueController {

    private final IssueService issueService;

    // ── POST /issues — Submit new issue ───────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a new city issue (citizen)")
    public ApiResponse<IssueResponse> submit(
            @Valid @RequestBody CreateIssueRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok("Issue submitted successfully",
            issueService.submitIssue(req, user.getId()));
    }

    // ── GET /issues — List all issues (admin/staff view) ─────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List all issues with filtering (admin/staff only)")
    public ApiResponse<Page<IssueSummaryResponse>> listAll(
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssueCategory category,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(issueService.getAllIssues(status, category, pageable));
    }

    // ── GET /issues/my — Citizen's own issues ─────────────────────
    @GetMapping("/my")
    @Operation(summary = "Get all issues submitted by the logged-in citizen")
    public ApiResponse<Page<IssueSummaryResponse>> myIssues(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(issueService.getMyIssues(user.getId(), pageable));
    }

    // ── GET /issues/{id} — Issue details ──────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Get full details and history of an issue")
    public ApiResponse<IssueResponse> getIssue(@PathVariable Long id) {
        return ApiResponse.ok(issueService.getIssue(id));
    }

    // ── PATCH /issues/{id}/status — Update status ─────────────────
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update issue status (staff/admin only)")
    public ApiResponse<IssueResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok("Status updated",
            issueService.updateStatus(id, req, user.getId()));
    }

    // ── POST /issues/{id}/assign — Assign to department ──────────
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign issue to a department (admin only)")
    public ApiResponse<IssueResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignIssueRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok("Issue assigned",
            issueService.assignIssue(id, req, user.getId()));
    }
}
