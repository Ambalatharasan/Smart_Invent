package com.stockwise.api.repository;

import com.stockwise.api.entity.LoginEvent;
import com.stockwise.api.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {
    List<LoginEvent> findTop50ByOrderByCreatedAtDesc();

    List<LoginEvent> findTop25ByUserAccountOrderByCreatedAtDesc(UserAccount userAccount);
}
