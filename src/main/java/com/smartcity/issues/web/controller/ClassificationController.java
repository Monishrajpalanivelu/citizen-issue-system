package com.smartcity.issues.web.controller;

import com.smartcity.issues.domain.enums.IssueCategory;
import com.smartcity.issues.service.ClassificationService;
import com.smartcity.issues.web.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classify")
@RequiredArgsConstructor
@Tag(name = "Classification", description = "Classify issues by location and category")
@SecurityRequirement(name = "bearerAuth")
public class ClassificationController {

    private final ClassificationService classificationService;

    // ════════════════════════════════════════════════════════
    // LOCATION-BASED CLASSIFICATION
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/classify/by-location
     *
     * Returns every district with its issue breakdown:
     *   - open / in-progress / resolved counts
     *   - top 5 categories reported in that district
     *   - priority distribution
     *   - hotspot level (NONE → CRITICAL)
     *
     * Use this to answer: "Which areas of the city need attention?"
     */
    @GetMapping("/by-location")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary     = "Classify all issues by location (district)",
        description = "Returns per-district breakdown with hotspot levels, top categories, and status counts. Sorted worst-first."
    )
    public ApiResponse<List<LocationClassificationResponse>> classifyByLocation() {
        return ApiResponse.ok(classificationService.classifyByLocation());
    }

    /**
     * GET /api/v1/classify/by-location/{districtId}
     *
     * Deep-dive into a single district.
     */
    @GetMapping("/by-location/{districtId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary     = "Classify issues for a specific district",
        description = "Full breakdown for a single district by ID."
    )
    public ApiResponse<LocationClassificationResponse> classifyDistrict(
            @PathVariable @Parameter(description = "District ID") Long districtId) {
        return ApiResponse.ok(classificationService.classifyDistrict(districtId));
    }

    // ════════════════════════════════════════════════════════
    // CATEGORY-BASED CLASSIFICATION
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/classify/by-category
     *
     * Returns every issue category with:
     *   - total / open / resolved counts
     *   - critical & high priority counts
     *   - avg resolution time (hours)
     *   - resolution rate %
     *   - which districts report this category most
     *   - responsible department
     *
     * Use this to answer: "Which types of problems are hardest to fix?"
     */
    @GetMapping("/by-category")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary     = "Classify all issues by category",
        description = "Returns per-category breakdown with resolution rates, avg fix time, and top districts. Sorted by volume."
    )
    public ApiResponse<List<CategoryClassificationResponse>> classifyByCategory() {
        return ApiResponse.ok(classificationService.classifyByCategory());
    }

    /**
     * GET /api/v1/classify/by-category/{category}
     *
     * Deep-dive into a single category (e.g. POTHOLE, POWER_OUTAGE).
     */
    @GetMapping("/by-category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary     = "Classify a specific issue category",
        description = "Full breakdown for one category — resolution stats, district distribution, and department owner."
    )
    public ApiResponse<CategoryClassificationResponse> classifyCategory(
            @PathVariable @Parameter(description = "Category code e.g. POTHOLE") IssueCategory category) {
        return ApiResponse.ok(classificationService.classifyCategory(category));
    }

    // ════════════════════════════════════════════════════════
    // HEATMAP — District × Category Grid
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/classify/heatmap
     *
     * Returns a 2D data grid: rows = districts, columns = categories.
     * Each cell has total count, open count, critical count, and
     * an intensity label (NONE → CRITICAL) for frontend colour-coding.
     *
     * Also returns a summary: worst district, most-reported category,
     * total active & critical counts city-wide.
     *
     * Use this to render a visual city risk dashboard.
     */
    @GetMapping("/heatmap")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary     = "Issue heatmap — district × category grid",
        description = "2D grid of issue counts with intensity labels. Ideal for rendering a colour-coded city dashboard."
    )
    public ApiResponse<ClassificationHeatmapResponse> heatmap() {
        return ApiResponse.ok(classificationService.getHeatmap());
    }

    // ════════════════════════════════════════════════════════
    // GEO / NEARBY
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/classify/nearby?lat=&lng=&radius=
     *
     * Find all open issues within N metres of a coordinate.
     * Results sorted by distance ascending.
     * Useful for: field workers checking their area, citizens reporting near-duplicates.
     */
    @GetMapping("/nearby")
    @Operation(
        summary     = "Find issues near a GPS coordinate",
        description = "Returns active issues within the specified radius (metres). Sorted nearest-first."
    )
    public ApiResponse<List<NearbyIssueResponse>> nearby(
            @RequestParam @Parameter(description = "Latitude")               double lat,
            @RequestParam @Parameter(description = "Longitude")              double lng,
            @RequestParam(defaultValue = "500")
            @Parameter(description = "Search radius in metres (default 500)") double radius) {
        return ApiResponse.ok(
            "Issues within " + (int) radius + "m",
            classificationService.findNearby(lat, lng, radius)
        );
    }

    /**
     * GET /api/v1/classify/geo-clusters
     *
     * Returns per-district geo clusters with:
     *   - geographic centre (lat/lng)
     *   - issue count + critical count
     *   - dominant category
     *   - risk level
     *   - top 10 issues in the cluster
     *
     * Use this to power map cluster markers.
     */
    @GetMapping("/geo-clusters")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary     = "Geo clusters for map display",
        description = "Returns one cluster per district with centre coordinates, risk level, and dominant issue type."
    )
    public ApiResponse<List<GeoClusterResponse>> geoClusters() {
        return ApiResponse.ok(classificationService.getGeoClusters());
    }
}
