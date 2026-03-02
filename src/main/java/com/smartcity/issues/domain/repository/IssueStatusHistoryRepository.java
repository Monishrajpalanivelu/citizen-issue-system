package com.smartcity.issues.domain.repository;

import com.smartcity.issues.domain.entity.IssueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, Long> {
    List<IssueStatusHistory> findByIssueIdOrderByChangedAtAsc(Long issueId);
}
