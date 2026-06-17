package com.stockwise.api.controller;

import com.stockwise.api.dto.UserDtos.LoginEventResponse;
import com.stockwise.api.dto.UserDtos.UpdateUserProfileRequest;
import com.stockwise.api.dto.UserDtos.UserAccountResponse;
import com.stockwise.api.dto.UserDtos.UserProfileResponse;
import com.stockwise.api.service.LoginEventService;
import com.stockwise.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final LoginEventService loginEventService;

    public UserController(UserService userService, LoginEventService loginEventService) {
        this.userService = userService;
        this.loginEventService = loginEventService;
    }

    @GetMapping("/me")
    public UserAccountResponse me(Authentication authentication) {
        return userService.currentUser(authentication.getName());
    }

    @GetMapping("/me/profile")
    public UserProfileResponse myProfile(Authentication authentication) {
        return userService.currentProfile(authentication.getName());
    }

    @PutMapping("/me/profile")
    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return userService.updateCurrentProfile(authentication.getName(), request);
    }

    @GetMapping
    public List<UserAccountResponse> users() {
        return userService.listUsers();
    }

    @GetMapping("/login-events")
    public List<LoginEventResponse> loginEvents() {
        return loginEventService.listRecent();
    }
}
