package com.smartcity.issues.domain.repository;

import com.smartcity.issues.domain.entity.User;
import com.smartcity.issues.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleAndDepartmentId(UserRole role, Long departmentId);
    List<User> findByRole(UserRole role);
}
