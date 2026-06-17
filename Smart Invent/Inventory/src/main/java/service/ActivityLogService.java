package com.stockwise.api.service;

import com.stockwise.api.dto.ActivityDtos.ActivityResponse;
import com.stockwise.api.dto.ActivityDtos.CreateActivityRequest;
import com.stockwise.api.entity.ActivityLog;
import com.stockwise.api.entity.ActivityType;
import com.stockwise.api.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public ActivityResponse log(ActivityType type, String message, String itemSku, Integer quantity, LocalDate lastRestockDate, String actorEmail) {
        ActivityLog saved = activityLogRepository.save(new ActivityLog(type, message, itemSku, quantity, lastRestockDate, actorEmail));
        return toResponse(saved);
    }

    @Transactional
    public ActivityResponse create(CreateActivityRequest request, String actorEmail) {
        ActivityLog saved = activityLogRepository.save(new ActivityLog(
                request.type(),
                request.message(),
                request.itemSku(),
                request.quantity(),
                request.lastRestockDate(),
                actorEmail
        ));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(String type) {
        if (type == null || type.isBlank()) {
            return activityLogRepository.findTop100ByOrderByCreatedAtDesc()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        ActivityType activityType = ActivityType.valueOf(type.trim().toUpperCase());
        return activityLogRepository.findTop100ByTypeOrderByCreatedAtDesc(activityType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityResponse toResponse(ActivityLog log) {
        return new ActivityResponse(
                log.getId(),
                log.getType(),
                log.getMessage(),
                log.getItemSku(),
                log.getQuantity(),
                log.getLastRestockDate(),
                log.getActorEmail(),
                log.getCreatedAt()
        );
    }
}
