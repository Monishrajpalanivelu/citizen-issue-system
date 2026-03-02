package com.smartcity.issues.web.controller;

import com.smartcity.issues.domain.entity.Department;
import com.smartcity.issues.domain.entity.District;
import com.smartcity.issues.domain.enums.IssueStatus;
import com.smartcity.issues.domain.repository.DepartmentRepository;
import com.smartcity.issues.domain.repository.DistrictRepository;
import com.smartcity.issues.service.IssueService;
import com.smartcity.issues.web.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.smartcity.issues.domain.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Departments", description = "Analytics and department views")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final IssueService         issueService;
    private final DepartmentRepository deptRepo;
    private final DistrictRepository   districtRepo;

    // ── GET /dashboard — City-wide stats ──────────────────────────
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "City-wide issue statistics dashboard (admin)")
    public ApiResponse<DashboardStats> dashboard() {
        return ApiResponse.ok(issueService.getDashboardStats());
    }

    // ── GET /departments — List all departments ───────────────────
    @GetMapping("/departments")
    @Operation(summary = "List all departments")
    public ApiResponse<List<Department>> getDepartments() {
        return ApiResponse.ok(deptRepo.findAll());
    }

    // ── GET /departments/{id}/issues — Issues for my department ──
    @GetMapping("/departments/{id}/issues")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get issues assigned to a department")
    public ApiResponse<Page<IssueSummaryResponse>> getDepartmentIssues(
            @PathVariable Long id,
            @RequestParam(required = false) IssueStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(issueService.getDepartmentIssues(id, status, pageable));
    }

    // ── GET /departments/my/issues — Staff sees their dept issues ─
    @GetMapping("/departments/my/issues")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Staff: see all issues for your department")
    public ApiResponse<Page<IssueSummaryResponse>> myDeptIssues(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) IssueStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : -1L;
        return ApiResponse.ok(issueService.getDepartmentIssues(deptId, status, pageable));
    }

    // ── GET /districts — List all districts ──────────────────────
    @GetMapping("/districts")
    @Operation(summary = "List all city districts")
    public ApiResponse<List<District>> getDistricts() {
        return ApiResponse.ok(districtRepo.findAll());
    }
}
