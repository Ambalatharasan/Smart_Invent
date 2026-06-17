package com.stockwise.api.service;

import com.stockwise.api.dto.UserDtos.UpdateUserProfileRequest;
import com.stockwise.api.dto.UserDtos.UserAccountResponse;
import com.stockwise.api.dto.UserDtos.UserProfileResponse;
import com.stockwise.api.entity.UserAccount;
import com.stockwise.api.entity.UserProfileData;
import com.stockwise.api.repository.UserAccountRepository;
import com.stockwise.api.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;

    public UserService(UserAccountRepository userAccountRepository, UserProfileRepository userProfileRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserAccountResponse currentUser(String email) {
        UserAccount user = findByEmail(email);
        return toResponse(user, profileFor(user));
    }

    @Transactional
    public UserProfileResponse currentProfile(String email) {
        UserAccount user = findByEmail(email);
        UserProfileData profile = profileFor(user);
        return toProfileResponse(userProfileRepository.save(profile));
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(String email, UpdateUserProfileRequest request) {
        UserAccount user = findByEmail(email);
        UserProfileData profile = userProfileRepository.findByUserAccount(user)
                .orElseGet(() -> new UserProfileData(user, user.getName(), null, null));

        profile.update(
                displayNameOrDefault(request.displayName(), profile.getDisplayName(), user.getName()),
                normalized(request.phone()),
                normalized(request.department()),
                normalized(request.jobTitle()),
                normalized(request.location()),
                normalized(request.bio())
        );

        return toProfileResponse(userProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<UserAccountResponse> listUsers() {
        return userAccountRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(user -> toResponse(user, userProfileRepository.findByUserAccount(user).orElse(null)))
                .toList();
    }

    private UserAccount findByEmail(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));
    }

    private UserProfileData profileFor(UserAccount user) {
        return userProfileRepository.findByUserAccount(user)
                .orElseGet(() -> userProfileRepository.save(new UserProfileData(user, user.getName(), null, null)));
    }

    private UserAccountResponse toResponse(UserAccount user, UserProfileData profile) {
        return new UserAccountResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt(),
                profile == null ? null : toProfileResponse(profile)
        );
    }

    private UserProfileResponse toProfileResponse(UserProfileData profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getDisplayName(),
                profile.getPhone(),
                profile.getDepartment(),
                profile.getJobTitle(),
                profile.getLocation(),
                profile.getBio(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String displayNameOrDefault(String requested, String existing, String fallback) {
        String normalized = normalized(requested);
        if (normalized != null) {
            return normalized;
        }
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return fallback;
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
