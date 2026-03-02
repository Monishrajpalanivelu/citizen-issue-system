package com.smartcity.issues.web.dto;

import com.smartcity.issues.domain.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    // ── Auth DTOs ───────────────────────────────────────────────────────────────

    @Data
    public static class RegisterRequest {
        @NotBlank
        String fullName;
        @Email
        @NotBlank
        String email;
        @NotBlank
        @Size(min = 6)
        String password;
        String phone;
    }

    @Data
    public static class LoginRequest {
        @Email
        @NotBlank
        String email;
        @NotBlank
        String password;
    }

    @Data
    @Builder
    public static class AuthResponse {
        String token;
        String email;
        String fullName;
        UserRole role;
        String message;
    }

    // ── Issue DTOs ──────────────────────────────────────────────────────────────

    @Data
    public static class CreateIssueRequest {
        @NotBlank
        @Size(max = 200)
        String title;
        @NotBlank
        String description;
        @NotNull
        IssueCategory category;
        String address;
        BigDecimal latitude;
        BigDecimal longitude;
        Long districtId;
        List<String> mediaUrls;
    }

    @Data
    public static class UpdateStatusRequest {
        @NotNull
        IssueStatus status;
        String notes;
    }

    @Data
    public static class AssignIssueRequest {
        @NotNull
        Long departmentId;
        Long staffId;
        String notes;
    }

    @Data
    @Builder
    public static class IssueResponse {
        Long id;
        String title;
        String description;
        IssueCategory category;
        String categoryDisplayName;
        IssueStatus status;
        String statusDescription;
        Priority priority;
        String address;
        BigDecimal latitude;
        BigDecimal longitude;
        String districtName;
        String reportedByName;
        String assignedDepartmentName;
        String assignedToName;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
        LocalDateTime resolvedAt;
        String resolutionNotes;
        List<String> mediaUrls;
        List<StatusHistoryResponse> history;
    }

    @Data
    @Builder
    public static class StatusHistoryResponse {
        IssueStatus oldStatus;
        IssueStatus newStatus;
        String changedByName;
        String notes;
        LocalDateTime changedAt;
    }

    @Data
    @Builder
    public static class IssueSummaryResponse {
        Long id;
        String title;
        IssueCategory category;
        IssueStatus status;
        Priority priority;
        String districtName;
        LocalDateTime createdAt;
    }

    // ── Dashboard DTOs ──────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class DashboardStats {
        long totalIssues;
        long openIssues;
        long inProgressIssues;
        long resolvedIssues;
        long criticalIssues;
        List<CategoryStat> byCategory;
        List<StatusStat> byStatus;
        List<DistrictStat> byDistrict;
    }

    @Data
    @AllArgsConstructor
    public static class CategoryStat {
        String category;
        long count;
    }

    @Data
    @AllArgsConstructor
    public static class StatusStat {
        String status;
        long count;
    }

    @Data
    @AllArgsConstructor
    public static class DistrictStat {
        String district;
        long count;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLASSIFICATION DTOs — Location & Category Breakdown
    // ══════════════════════════════════════════════════════════════════════════

    @Data
    @Builder
    public static class LocationClassificationResponse {
        String districtName;
        long totalIssues;
        long openIssues;
        long inProgressIssues;
        long resolvedIssues;
        long rejectedIssues;
        long unresolvedIssues;
        List<CategoryBreakdown> categoryBreakdown;
        List<PriorityBreakdown> priorityBreakdown;
        String hotspotLevel; // NONE | LOW | MEDIUM | HIGH | CRITICAL
    }

    @Data
    @Builder
    public static class CategoryClassificationResponse {
        String categoryCode;
        String categoryDisplayName;
        String responsibleDepartment;
        long totalIssues;
        long openIssues;
        long inProgressIssues;
        long resolvedIssues;
        long criticalCount;
        long highCount;
        double avgResolutionHours;
        double resolutionRatePct;
        List<DistrictBreakdown> districtBreakdown;
    }

    @Data
    @Builder
    public static class ClassificationHeatmapResponse {
        List<HeatmapCell> cells;
        List<String> districts;
        List<String> categories;
        HeatmapSummary summary;
    }

    @Data
    @AllArgsConstructor
    public static class HeatmapCell {
        String districtName;
        String category;
        String categoryDisplayName;
        long total;
        long openCount;
        long criticalCount;
        String intensity;
    }

    @Data
    @Builder
    public static class HeatmapSummary {
        String worstDistrict;
        String mostReportedCategory;
        long totalActiveIssues;
        long totalCriticalIssues;
    }

    @Data
    @AllArgsConstructor
    public static class CategoryBreakdown {
        String category;
        String displayName;
        long count;
    }

    @Data
    @AllArgsConstructor
    public static class PriorityBreakdown {
        String priority;
        long count;
    }

    @Data
    @AllArgsConstructor
    public static class DistrictBreakdown {
        String districtName;
        long count;
    }

    @Data
    @Builder
    public static class NearbyIssueResponse {
        Long id;
        String title;
        IssueCategory category;
        String categoryDisplayName;
        IssueStatus status;
        Priority priority;
        BigDecimal latitude;
        BigDecimal longitude;
        String address;
        double distanceMeters;
        LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class GeoClusterResponse {
        String districtName;
        double centerLat;
        double centerLng;
        long issueCount;
        long criticalCount;
        String dominantCategory;
        String riskLevel;
        List<NearbyIssueResponse> issues;
    }

    // ── Utility ─────────────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class ApiResponse<T> {
        boolean success;
        String message;
        T data;

        public static <T> ApiResponse<T> ok(T data) {
            return ApiResponse.<T>builder().success(true).data(data).build();
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }
}
