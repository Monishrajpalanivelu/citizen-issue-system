package com.smartcity.issues.domain.repository;

import com.smartcity.issues.domain.entity.Issue;
import com.smartcity.issues.domain.enums.IssueCategory;
import com.smartcity.issues.domain.enums.IssueStatus;
import com.smartcity.issues.domain.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    // ── By reporter ────────────────────────────────────
    Page<Issue> findByReportedById(Long userId, Pageable pageable);

    // ── By status & category (filterable) ─────────────
    Page<Issue> findByStatus(IssueStatus status, Pageable pageable);
    Page<Issue> findByCategory(IssueCategory category, Pageable pageable);
    Page<Issue> findByStatusAndCategory(IssueStatus status, IssueCategory category, Pageable pageable);

    // ── By department ──────────────────────────────────
    Page<Issue> findByAssignedDepartmentId(Long deptId, Pageable pageable);
    Page<Issue> findByAssignedDepartmentIdAndStatus(Long deptId, IssueStatus status, Pageable pageable);

    // ── By district ────────────────────────────────────
    Page<Issue> findByDistrictId(Long districtId, Pageable pageable);

    // ── By assigned staff ─────────────────────────────
    Page<Issue> findByAssignedToId(Long staffId, Pageable pageable);

    // ── Dashboard analytics ────────────────────────────
    long countByStatus(IssueStatus status);
    long countByCategory(IssueCategory category);
    long countByPriority(Priority priority);
    long countByAssignedDepartmentId(Long deptId);

    @Query("SELECT i.status, COUNT(i) FROM Issue i GROUP BY i.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT i.category, COUNT(i) FROM Issue i GROUP BY i.category ORDER BY COUNT(i) DESC")
    List<Object[]> countGroupByCategory();

    @Query("SELECT i.district.name, COUNT(i) FROM Issue i WHERE i.district IS NOT NULL GROUP BY i.district.name ORDER BY COUNT(i) DESC")
    List<Object[]> countGroupByDistrict();

    // ── Recent issues ─────────────────────────────────
    @Query("SELECT i FROM Issue i WHERE i.createdAt >= :since ORDER BY i.createdAt DESC")
    List<Issue> findRecentIssues(@Param("since") LocalDateTime since);

    // ── Nearby issues (simple bounding box) ───────────
    @Query("""
        SELECT i FROM Issue i
        WHERE i.latitude IS NOT NULL
          AND i.latitude  BETWEEN :minLat AND :maxLat
          AND i.longitude BETWEEN :minLng AND :maxLng
          AND i.status    <> 'CLOSED'
        ORDER BY i.createdAt DESC
        """)
    List<Issue> findNearby(
        @Param("minLat") double minLat, @Param("maxLat") double maxLat,
        @Param("minLng") double minLng, @Param("maxLng") double maxLng
    );

    // ── SLA breach candidates: open issues older than N hours ─
    @Query("SELECT i FROM Issue i WHERE i.status NOT IN ('RESOLVED','CLOSED','REJECTED') AND i.createdAt < :threshold ORDER BY i.priority DESC")
    List<Issue> findSlaBreachCandidates(@Param("threshold") LocalDateTime threshold);

    // ════════════════════════════════════════════════════════
    // CLASSIFICATION QUERIES
    // ════════════════════════════════════════════════════════

    // ── By Location: district breakdown ──────────────────
    @Query("""
        SELECT i.district.id, i.district.name, i.category, i.status, i.priority, COUNT(i)
        FROM Issue i
        WHERE i.district IS NOT NULL
        GROUP BY i.district.id, i.district.name, i.category, i.status, i.priority
        ORDER BY i.district.name, COUNT(i) DESC
        """)
    List<Object[]> classifyByDistrictCategoryStatus();

    // ── Category heatmap: district × category count ───────
    @Query(value = """
        SELECT d.name AS district_name,
               i.category,
               COUNT(*) AS total,
               SUM(CASE WHEN i.status NOT IN ('RESOLVED','CLOSED') THEN 1 ELSE 0 END) AS open_count,
               SUM(CASE WHEN i.priority = 'CRITICAL' THEN 1 ELSE 0 END) AS critical_count
        FROM issues i
        JOIN districts d ON i.district_id = d.id
        GROUP BY d.name, i.category
        ORDER BY d.name, total DESC
        """, nativeQuery = true)
    List<Object[]> heatmapByDistrictAndCategory();

    // ── Issues by district + status summary ───────────────
    @Query(value = """
        SELECT d.name,
               COUNT(*) AS total,
               SUM(CASE WHEN i.status = 'OPEN'        THEN 1 ELSE 0 END) AS open,
               SUM(CASE WHEN i.status = 'ASSIGNED'    THEN 1 ELSE 0 END) AS assigned,
               SUM(CASE WHEN i.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress,
               SUM(CASE WHEN i.status = 'RESOLVED'    THEN 1 ELSE 0 END) AS resolved,
               SUM(CASE WHEN i.status = 'REJECTED'    THEN 1 ELSE 0 END) AS rejected
        FROM issues i
        JOIN districts d ON i.district_id = d.id
        GROUP BY d.name
        ORDER BY total DESC
        """, nativeQuery = true)
    List<Object[]> districtStatusSummary();

    // ── Issues by category + status summary ───────────────
    @Query(value = """
        SELECT category,
               COUNT(*) AS total,
               SUM(CASE WHEN status = 'OPEN'        THEN 1 ELSE 0 END) AS open,
               SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress,
               SUM(CASE WHEN status = 'RESOLVED'    THEN 1 ELSE 0 END) AS resolved,
               SUM(CASE WHEN priority = 'CRITICAL'  THEN 1 ELSE 0 END) AS critical,
               SUM(CASE WHEN priority = 'HIGH'      THEN 1 ELSE 0 END) AS high,
               ROUND(AVG(EXTRACT(EPOCH FROM (
                   COALESCE(resolved_at, NOW()) - created_at
               )) / 3600), 1) AS avg_resolution_hours
        FROM issues
        GROUP BY category
        ORDER BY total DESC
        """, nativeQuery = true)
    List<Object[]> categoryStatusSummary();

    // ── Top hotspot locations (lat/lng cluster) ────────────
    @Query("""
        SELECT i FROM Issue i
        WHERE i.latitude IS NOT NULL
          AND i.status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
        ORDER BY i.priority DESC, i.createdAt DESC
        """)
    List<Issue> findAllActiveGeoIssues();

    // ── Issues under a specific district + category ────────
    List<Issue> findByDistrictIdAndCategory(Long districtId, IssueCategory category, Pageable pageable);

    // ── Unresolved issues count per district ──────────────
    @Query("""
        SELECT i.district.name, COUNT(i)
        FROM Issue i
        WHERE i.district IS NOT NULL
          AND i.status NOT IN ('RESOLVED','CLOSED','REJECTED')
        GROUP BY i.district.name
        ORDER BY COUNT(i) DESC
        """)
    List<Object[]> unresolvedCountByDistrict();

    // ── Resolution rate per category ──────────────────────
    @Query(value = """
        SELECT category,
               COUNT(*) AS total,
               SUM(CASE WHEN status IN ('RESOLVED','CLOSED') THEN 1 ELSE 0 END) AS resolved,
               ROUND(
                   100.0 * SUM(CASE WHEN status IN ('RESOLVED','CLOSED') THEN 1 ELSE 0 END) / COUNT(*),
                   1
               ) AS resolution_rate_pct
        FROM issues
        GROUP BY category
        ORDER BY resolution_rate_pct DESC
        """, nativeQuery = true)
    List<Object[]> resolutionRateByCategory();
}
