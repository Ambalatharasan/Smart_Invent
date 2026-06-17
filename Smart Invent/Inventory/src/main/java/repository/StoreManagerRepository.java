package com.stockwise.api.repository;

import com.stockwise.api.entity.StoreManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreManagerRepository extends JpaRepository<StoreManager, Long> {
    Optional<StoreManager> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<StoreManager> findByDepartmentIgnoreCaseOrderByFullNameAsc(String department);
}
