package com.stockwise.api.repository;

import com.stockwise.api.entity.ActivityLog;
import com.stockwise.api.entity.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop100ByOrderByCreatedAtDesc();

    List<ActivityLog> findTop100ByTypeOrderByCreatedAtDesc(ActivityType type);
}
