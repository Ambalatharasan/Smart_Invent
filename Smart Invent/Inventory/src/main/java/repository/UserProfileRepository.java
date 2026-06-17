package com.stockwise.api.repository;

import com.stockwise.api.entity.UserAccount;
import com.stockwise.api.entity.UserProfileData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfileData, Long> {
    Optional<UserProfileData> findByUserAccount(UserAccount userAccount);

    boolean existsByUserAccount(UserAccount userAccount);
}
