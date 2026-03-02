package com.smartcity.issues.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;           // e.g. INFRA, WATER, POWER

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "assignedDepartment", fetch = FetchType.LAZY)
    private List<Issue> issues;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
