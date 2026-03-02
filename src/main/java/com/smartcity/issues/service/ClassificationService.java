package com.smartcity.issues.service;

import com.smartcity.issues.domain.entity.Issue;
import com.smartcity.issues.domain.enums.IssueCategory;
import com.smartcity.issues.domain.enums.IssueStatus;
import com.smartcity.issues.domain.enums.Priority;
import com.smartcity.issues.domain.repository.DistrictRepository;
import com.smartcity.issues.domain.repository.IssueRepository;
import com.smartcity.issues.web.dto.Dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClassificationService {

    private final IssueRepository   issueRepo;
    private final DistrictRepository districtRepo;

    // ══════════════════════════════════════════════════════
    // LOCATION-BASED CLASSIFICATION
    // ══════════════════════════════════════════════════════

    /**
     * Returns a full classification breakdown for every district:
     * open/resolved counts, top categories, priority distribution,
     * and a computed hotspot level.
     */
    @Cacheable("location-classification")
    public List<LocationClassificationResponse> classifyByLocation() {
        List<Object[]> statusData   = issueRepo.districtStatusSummary();
        List<Object[]> heatmapData  = issueRepo.heatmapByDistrictAndCategory();

        // Build category map per district: districtName → [CategoryBreakdown]
        Map<String, List<CategoryBreakdown>> catMap = new LinkedHashMap<>();
        Map<String, Long> criticalMap = new HashMap<>();

        for (Object[] row : heatmapData) {
            String districtName = (String)  row[0];
            String category     = (String)  row[1];
            long   total        = toLong(row[2]);
            long   criticalCnt  = toLong(row[4]);

            catMap.computeIfAbsent(districtName, k -> new ArrayList<>())
                  .add(new CategoryBreakdown(
                      category,
                      safeCategoryDisplayName(category),
                      total
                  ));

            criticalMap.merge(districtName, criticalCnt, Long::sum);
        }

        List<LocationClassificationResponse> results = new ArrayList<>();

        for (Object[] row : statusData) {
            String districtName = (String) row[0];
            long   total        = toLong(row[1]);
            long   open         = toLong(row[2]);
            long   assigned     = toLong(row[3]);
            long   inProgress   = toLong(row[4]);
            long   resolved     = toLong(row[5]);
            long   rejected     = toLong(row[6]);
            long   unresolved   = open + assigned + inProgress;

            List<CategoryBreakdown> cats = catMap.getOrDefault(districtName, List.of());
            long critical = criticalMap.getOrDefault(districtName, 0L);

            results.add(LocationClassificationResponse.builder()
                .districtName(districtName)
                .totalIssues(total)
                .openIssues(open)
                .inProgressIssues(inProgress)
                .resolvedIssues(resolved)
                .rejectedIssues(rejected)
                .unresolvedIssues(unresolved)
                .categoryBreakdown(cats.stream()
                    .sorted(Comparator.comparingLong(CategoryBreakdown::getCount).reversed())
                    .limit(5)
                    .collect(Collectors.toList()))
                .priorityBreakdown(buildPriorityBreakdown(districtName))
                .hotspotLevel(computeHotspot(unresolved, critical))
                .build());
        }

        // Sort: worst hotspot first
        results.sort(Comparator.comparingLong(LocationClassificationResponse::getUnresolvedIssues).reversed());
        return results;
    }

    /**
     * Full classification for a single district by ID.
     */
    public LocationClassificationResponse classifyDistrict(Long districtId) {
        String name = districtRepo.findById(districtId)
            .map(d -> d.getName())
            .orElseThrow(() -> new NoSuchElementException("District not found: " + districtId));

        return classifyByLocation().stream()
            .filter(r -> r.getDistrictName().equals(name))
            .findFirst()
            .orElse(LocationClassificationResponse.builder()
                .districtName(name).totalIssues(0).hotspotLevel("NONE").build());
    }

    // ══════════════════════════════════════════════════════
    // CATEGORY-BASED CLASSIFICATION
    // ══════════════════════════════════════════════════════

    /**
     * Returns a full classification breakdown for every category:
     * resolution rate, avg time, critical counts, and which districts
     * report this issue most.
     */
    @Cacheable("category-classification")
    public List<CategoryClassificationResponse> classifyByCategory() {
        List<Object[]> catData         = issueRepo.categoryStatusSummary();
        List<Object[]> resolutionData  = issueRepo.resolutionRateByCategory();
        List<Object[]> heatmapData     = issueRepo.heatmapByDistrictAndCategory();

        // Resolution rate map: category → rate %
        Map<String, Double> resolutionRateMap = new HashMap<>();
        for (Object[] row : resolutionData) {
            resolutionRateMap.put((String) row[0], toDouble(row[3]));
        }

        // District breakdown map: category → [DistrictBreakdown]
        Map<String, List<DistrictBreakdown>> districtMap = new LinkedHashMap<>();
        for (Object[] row : heatmapData) {
            String district = (String) row[0];
            String category = (String) row[1];
            long   total    = toLong(row[2]);

            districtMap.computeIfAbsent(category, k -> new ArrayList<>())
                       .add(new DistrictBreakdown(district, total));
        }

        List<CategoryClassificationResponse> results = new ArrayList<>();

        for (Object[] row : catData) {
            String category     = (String) row[0];
            long   total        = toLong(row[1]);
            long   open         = toLong(row[2]);
            long   inProgress   = toLong(row[3]);
            long   resolved     = toLong(row[4]);
            long   critical     = toLong(row[5]);
            long   high         = toLong(row[6]);
            double avgHours     = toDouble(row[7]);
            double resRate      = resolutionRateMap.getOrDefault(category, 0.0);

            IssueCategory cat     = safeCategory(category);
            String deptName = cat != null ? cat.getDepartmentCode() : "GENERAL";
            String display  = cat != null ? cat.getDisplayName()    : category;

            List<DistrictBreakdown> districts = districtMap.getOrDefault(category, List.of())
                .stream()
                .sorted(Comparator.comparingLong(DistrictBreakdown::getCount).reversed())
                .limit(5)
                .collect(Collectors.toList());

            results.add(CategoryClassificationResponse.builder()
                .categoryCode(category)
                .categoryDisplayName(display)
                .responsibleDepartment(deptName)
                .totalIssues(total)
                .openIssues(open)
                .inProgressIssues(inProgress)
                .resolvedIssues(resolved)
                .criticalCount(critical)
                .highCount(high)
                .avgResolutionHours(avgHours)
                .resolutionRatePct(resRate)
                .districtBreakdown(districts)
                .build());
        }

        // Sort: most reported first
        results.sort(Comparator.comparingLong(CategoryClassificationResponse::getTotalIssues).reversed());
        return results;
    }

    /**
     * Classification for a single category.
     */
    public CategoryClassificationResponse classifyCategory(IssueCategory category) {
        return classifyByCategory().stream()
            .filter(r -> r.getCategoryCode().equals(category.name()))
            .findFirst()
            .orElse(CategoryClassificationResponse.builder()
                .categoryCode(category.name())
                .categoryDisplayName(category.getDisplayName())
                .responsibleDepartment(category.getDepartmentCode())
                .totalIssues(0).resolutionRatePct(0).build());
    }

    // ══════════════════════════════════════════════════════
    // HEATMAP — District × Category Grid
    // ══════════════════════════════════════════════════════

    /**
     * 2D heatmap: for every (district, category) pair, returns
     * total count, open count, critical count, and intensity.
     * Ideal for rendering a colour-coded grid in the frontend.
     */
    @Cacheable("classification-heatmap")
    public ClassificationHeatmapResponse getHeatmap() {
        List<Object[]> rows = issueRepo.heatmapByDistrictAndCategory();

        List<HeatmapCell> cells     = new ArrayList<>();
        Set<String>       districts = new LinkedHashSet<>();
        Set<String>       categories = new LinkedHashSet<>();

        long totalActive   = 0;
        long totalCritical = 0;
        Map<String, Long> districtUnresolved = new HashMap<>();
        Map<String, Long> categoryTotals     = new HashMap<>();

        for (Object[] row : rows) {
            String district  = (String) row[0];
            String category  = (String) row[1];
            long   total     = toLong(row[2]);
            long   open      = toLong(row[3]);
            long   critical  = toLong(row[4]);

            districts.add(district);
            categories.add(category);
            totalActive   += open;
            totalCritical += critical;

            districtUnresolved.merge(district, open, Long::sum);
            categoryTotals.merge(category, total, Long::sum);

            cells.add(new HeatmapCell(
                district, category,
                safeCategoryDisplayName(category),
                total, open, critical,
                computeIntensity(total, critical)
            ));
        }

        // Worst district = most unresolved issues
        String worstDistrict = districtUnresolved.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("N/A");

        String topCategory = categoryTotals.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("N/A");

        return ClassificationHeatmapResponse.builder()
            .cells(cells)
            .districts(new ArrayList<>(districts))
            .categories(new ArrayList<>(categories))
            .summary(HeatmapSummary.builder()
                .worstDistrict(worstDistrict)
                .mostReportedCategory(safeCategoryDisplayName(topCategory))
                .totalActiveIssues(totalActive)
                .totalCriticalIssues(totalCritical)
                .build())
            .build();
    }

    // ══════════════════════════════════════════════════════
    // GEO / NEARBY
    // ══════════════════════════════════════════════════════

    /**
     * Find active issues within a radius (metres) of a coordinate.
     * Uses simple bounding box for hackathon speed.
     */
    public List<NearbyIssueResponse> findNearby(double lat, double lng, double radiusMeters) {
        // 1 degree ≈ 111,320 metres at equator
        double delta = radiusMeters / 111_320.0;

        List<Issue> nearby = issueRepo.findNearby(
            lat - delta, lat + delta,
            lng - delta, lng + delta
        );

        return nearby.stream()
            .map(i -> {
                double dist = haversine(lat, lng,
                    i.getLatitude().doubleValue(),
                    i.getLongitude().doubleValue());

                return NearbyIssueResponse.builder()
                    .id(i.getId())
                    .title(i.getTitle())
                    .category(i.getCategory())
                    .categoryDisplayName(i.getCategory().getDisplayName())
                    .status(i.getStatus())
                    .priority(i.getPriority())
                    .latitude(i.getLatitude())
                    .longitude(i.getLongitude())
                    .address(i.getAddress())
                    .distanceMeters(dist)
                    .createdAt(i.getCreatedAt())
                    .build();
            })
            .filter(r -> r.getDistanceMeters() <= radiusMeters)
            .sorted(Comparator.comparingDouble(NearbyIssueResponse::getDistanceMeters))
            .collect(Collectors.toList());
    }

    /**
     * Returns geo clusters per district (for map cluster markers).
     * Each cluster shows dominant category + risk level.
     */
    public List<GeoClusterResponse> getGeoClusters() {
        List<Issue> allActive = issueRepo.findAllActiveGeoIssues();

        // Group by district name
        Map<String, List<Issue>> byDistrict = allActive.stream()
            .filter(i -> i.getDistrict() != null)
            .collect(Collectors.groupingBy(i -> i.getDistrict().getName()));

        return byDistrict.entrySet().stream().map(entry -> {
            String      distName = entry.getKey();
            List<Issue> issues   = entry.getValue();

            // Compute geographic centre
            double avgLat = issues.stream()
                .filter(i -> i.getLatitude() != null)
                .mapToDouble(i -> i.getLatitude().doubleValue())
                .average().orElse(0);
            double avgLng = issues.stream()
                .filter(i -> i.getLongitude() != null)
                .mapToDouble(i -> i.getLongitude().doubleValue())
                .average().orElse(0);

            long criticalCount = issues.stream()
                .filter(i -> i.getPriority() == Priority.CRITICAL).count();

            // Dominant category = most frequent
            String dominant = issues.stream()
                .collect(Collectors.groupingBy(i -> i.getCategory().name(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("OTHER");

            // Build issue list (top 10 per cluster)
            List<NearbyIssueResponse> issueList = issues.stream()
                .limit(10)
                .map(i -> NearbyIssueResponse.builder()
                    .id(i.getId()).title(i.getTitle())
                    .category(i.getCategory())
                    .categoryDisplayName(i.getCategory().getDisplayName())
                    .status(i.getStatus()).priority(i.getPriority())
                    .latitude(i.getLatitude()).longitude(i.getLongitude())
                    .address(i.getAddress()).distanceMeters(0)
                    .createdAt(i.getCreatedAt())
                    .build())
                .collect(Collectors.toList());

            return GeoClusterResponse.builder()
                .districtName(distName)
                .centerLat(avgLat)
                .centerLng(avgLng)
                .issueCount(issues.size())
                .criticalCount(criticalCount)
                .dominantCategory(safeCategoryDisplayName(dominant))
                .riskLevel(computeHotspot(issues.size(), criticalCount))
                .issues(issueList)
                .build();

        }).sorted(Comparator.comparingLong(GeoClusterResponse::getIssueCount).reversed())
          .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════

    private List<PriorityBreakdown> buildPriorityBreakdown(String districtName) {
        // Simplified: pull from pre-loaded geo issues
        List<Issue> distIssues = issueRepo.findAllActiveGeoIssues().stream()
            .filter(i -> i.getDistrict() != null
                      && i.getDistrict().getName().equals(districtName))
            .collect(Collectors.toList());

        Map<String, Long> counts = distIssues.stream()
            .collect(Collectors.groupingBy(i -> i.getPriority().name(), Collectors.counting()));

        return Arrays.stream(Priority.values())
            .map(p -> new PriorityBreakdown(p.name(), counts.getOrDefault(p.name(), 0L)))
            .collect(Collectors.toList());
    }

    /** Hotspot = combination of raw unresolved count + critical boost */
    private String computeHotspot(long unresolved, long critical) {
        if (critical >= 3 || unresolved >= 20) return "CRITICAL";
        if (critical >= 1 || unresolved >= 10) return "HIGH";
        if (unresolved >= 5)                   return "MEDIUM";
        if (unresolved >= 1)                   return "LOW";
        return "NONE";
    }

    /** Cell intensity for heatmap colouring */
    private String computeIntensity(long total, long critical) {
        if (critical >= 2 || total >= 15) return "CRITICAL";
        if (critical >= 1 || total >= 8)  return "HIGH";
        if (total >= 4)                   return "MEDIUM";
        if (total >= 1)                   return "LOW";
        return "NONE";
    }

    private String safeCategoryDisplayName(String categoryCode) {
        try {
            return IssueCategory.valueOf(categoryCode).getDisplayName();
        } catch (Exception e) {
            return categoryCode;
        }
    }

    private IssueCategory safeCategory(String code) {
        try { return IssueCategory.valueOf(code); }
        catch (Exception e) { return null; }
    }

    /** Haversine distance in metres */
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private long   toLong(Object o)   { return o == null ? 0L   : ((Number) o).longValue();   }
    private double toDouble(Object o) { return o == null ? 0.0  : ((Number) o).doubleValue(); }
}
