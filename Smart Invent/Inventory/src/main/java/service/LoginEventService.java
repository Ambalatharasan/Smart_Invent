package com.stockwise.api.service;

import com.stockwise.api.dto.UserDtos.LoginEventResponse;
import com.stockwise.api.entity.LoginEvent;
import com.stockwise.api.entity.UserAccount;
import com.stockwise.api.repository.LoginEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoginEventService {
    private final LoginEventRepository loginEventRepository;

    public LoginEventService(LoginEventRepository loginEventRepository) {
        this.loginEventRepository = loginEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UserAccount userAccount, String email) {
        loginEventRepository.save(new LoginEvent(userAccount, email, true, null));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UserAccount userAccount, String email, String failureReason) {
        loginEventRepository.save(new LoginEvent(userAccount, email, false, failureReason));
    }

    @Transactional(readOnly = true)
    public List<LoginEventResponse> listRecent() {
        return loginEventRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private LoginEventResponse toResponse(LoginEvent event) {
        return new LoginEventResponse(
                event.getId(),
                event.getEmail(),
                event.isSuccessful(),
                event.getFailureReason(),
                event.getCreatedAt()
        );
    }
}
