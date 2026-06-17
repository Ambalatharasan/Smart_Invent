package com.stockwise.api.service;

import com.stockwise.api.dto.AuthDtos.AuthResponse;
import com.stockwise.api.dto.AuthDtos.ForgotPasswordRequest;
import com.stockwise.api.dto.AuthDtos.LoginRequest;
import com.stockwise.api.dto.AuthDtos.MessageResponse;
import com.stockwise.api.dto.AuthDtos.RegistrationResponse;
import com.stockwise.api.dto.AuthDtos.ResendVerificationRequest;
import com.stockwise.api.dto.AuthDtos.RegisterRequest;
import com.stockwise.api.dto.AuthDtos.UserProfile;
import com.stockwise.api.dto.AuthDtos.VerifyEmailRequest;
import com.stockwise.api.entity.Role;
import com.stockwise.api.entity.UserAccount;
import com.stockwise.api.entity.UserProfileData;
import com.stockwise.api.repository.UserAccountRepository;
import com.stockwise.api.repository.UserProfileRepository;
import com.stockwise.api.security.JwtService;
import com.stockwise.api.service.EmailVerificationService.VerificationDelivery;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginEventService loginEventService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordValidationService passwordValidationService;
    private final ForgotPasswordService forgotPasswordService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserAccountRepository userAccountRepository,
            UserProfileRepository userProfileRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            LoginEventService loginEventService,
            EmailVerificationService emailVerificationService,
            PasswordValidationService passwordValidationService,
            ForgotPasswordService forgotPasswordService
    ) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.loginEventService = loginEventService;
        this.emailVerificationService = emailVerificationService;
        this.passwordValidationService = passwordValidationService;
        this.forgotPasswordService = forgotPasswordService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        passwordValidationService.validateLoginPassword(request.password());
        UserAccount existingUser = userAccountRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existingUser != null && !existingUser.isEmailVerified()) {
            loginEventService.recordFailure(existingUser, email, "Email address is not verified");
            throw new DisabledException("Email verification required");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
            UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
            user.markLoggedIn();
            loginEventService.recordSuccess(user, email);
            return authResponse(user);
        } catch (AuthenticationException exception) {
            UserAccount user = userAccountRepository.findByEmailIgnoreCase(email).orElse(null);
            loginEventService.recordFailure(user, email, "Invalid email or password");
            throw exception;
        }
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        passwordValidationService.validateRegistrationPassword(request.password());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        UserAccount user = userAccountRepository.save(new UserAccount(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.MANAGER
        ));
        user.markPendingVerification();
        userProfileRepository.save(new UserProfileData(user, user.getName(), null, Role.MANAGER.name()));
        VerificationDelivery delivery = emailVerificationService.sendRegistrationCode(user);
        return new RegistrationResponse(
                email,
                verificationMessage(delivery),
                delivery.expiresAt().toString(),
                delivery.nextResendAt().toString()
        );
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        UserAccount user = emailVerificationService.verifyRegistrationCode(normalizeEmail(request.email()), request.code());
        return authResponse(user);
    }

    @Transactional
    public RegistrationResponse resendVerification(ResendVerificationRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("This account is already verified");
        }
        VerificationDelivery delivery = emailVerificationService.resendRegistrationCode(user);
        return new RegistrationResponse(
                email,
                verificationMessage(delivery),
                delivery.expiresAt().toString(),
                delivery.nextResendAt().toString()
        );
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        return new MessageResponse(forgotPasswordService.requestPasswordUpdate(request.email()));
    }

    private AuthResponse authResponse(UserAccount user) {
        String token = jwtService.createToken(user);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpiresInMinutes(),
                new UserProfile(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.isEmailVerified())
        );
    }

    private String verificationMessage(VerificationDelivery delivery) {
        if (delivery.emailSent()) {
            return "Verification code sent to your email address.";
        }
        return "Email delivery is not enabled. Check the backend console for the verification code.";
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
