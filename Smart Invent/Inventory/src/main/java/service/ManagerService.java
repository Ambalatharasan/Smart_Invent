package com.stockwise.api.service;

import com.stockwise.api.dto.ManagerDtos.ManagerRequest;
import com.stockwise.api.dto.ManagerDtos.ManagerResponse;
import com.stockwise.api.entity.ActivityType;
import com.stockwise.api.entity.StoreManager;
import com.stockwise.api.repository.StoreManagerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManagerService {
    private final StoreManagerRepository storeManagerRepository;
    private final ActivityLogService activityLogService;

    public ManagerService(StoreManagerRepository storeManagerRepository, ActivityLogService activityLogService) {
        this.storeManagerRepository = storeManagerRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public List<ManagerResponse> list(String department) {
        List<StoreManager> managers = department == null || department.isBlank()
                ? storeManagerRepository.findAll(Sort.by("fullName").ascending())
                : storeManagerRepository.findByDepartmentIgnoreCaseOrderByFullNameAsc(department.trim());
        return managers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ManagerResponse get(Long id) {
        return toResponse(findManager(id));
    }

    @Transactional
    public ManagerResponse create(ManagerRequest request, String actorEmail) {
        if (storeManagerRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new IllegalArgumentException("Manager email already exists");
        }
        StoreManager manager = new StoreManager(
                request.fullName().trim(),
                request.title().trim(),
                request.department().trim(),
                request.email().trim(),
                request.phone().trim(),
                request.shiftName().trim(),
                request.status().trim(),
                request.responsibilities()
        );
        StoreManager saved = storeManagerRepository.save(manager);
        activityLogService.log(ActivityType.MANAGER_UPDATED, "Added manager " + saved.getFullName(), null, null, null, actorEmail);
        return toResponse(saved);
    }

    @Transactional
    public ManagerResponse update(Long id, ManagerRequest request, String actorEmail) {
        StoreManager manager = findManager(id);
        String requestedEmail = request.email().trim();
        if (!manager.getEmail().equalsIgnoreCase(requestedEmail) && storeManagerRepository.existsByEmailIgnoreCase(requestedEmail)) {
            throw new IllegalArgumentException("Manager email already exists");
        }
        manager.setFullName(request.fullName().trim());
        manager.setTitle(request.title().trim());
        manager.setDepartment(request.department().trim());
        manager.setEmail(requestedEmail);
        manager.setPhone(request.phone().trim());
        manager.setShiftName(request.shiftName().trim());
        manager.setStatus(request.status().trim());
        manager.setResponsibilities(request.responsibilities());
        activityLogService.log(ActivityType.MANAGER_UPDATED, "Updated manager " + manager.getFullName(), null, null, null, actorEmail);
        return toResponse(manager);
    }

    @Transactional
    public void delete(Long id, String actorEmail) {
        StoreManager manager = findManager(id);
        storeManagerRepository.delete(manager);
        activityLogService.log(ActivityType.MANAGER_UPDATED, "Deleted manager " + manager.getFullName(), null, null, null, actorEmail);
    }

    private StoreManager findManager(Long id) {
        return storeManagerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
    }

    private ManagerResponse toResponse(StoreManager manager) {
        return new ManagerResponse(
                manager.getId(),
                manager.getFullName(),
                manager.getTitle(),
                manager.getDepartment(),
                manager.getEmail(),
                manager.getPhone(),
                manager.getShiftName(),
                manager.getStatus(),
                manager.getResponsibilities(),
                manager.getCreatedAt(),
                manager.getUpdatedAt()
        );
    }
}
