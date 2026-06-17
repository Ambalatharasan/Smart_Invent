package com.stockwise.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockwise.api.entity.EmailVerificationCode;
import com.stockwise.api.entity.UserAccount;
import com.stockwise.api.repository.EmailVerificationCodeRepository;
import com.stockwise.api.repository.UserAccountRepository;
import com.stockwise.api.service.EmailDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthVerificationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @MockBean
    private EmailDeliveryService emailDeliveryService;

    @Test
    void registeredUserMustVerifyEmailBeforeLogin() throws Exception {
        String email = "verified-user@smartinvent.local";
        String password = "Staff@12345";
        AtomicReference<String> verificationCode = new AtomicReference<>();

        doAnswer(invocation -> {
            verificationCode.set(invocation.getArgument(2));
            return true;
        }).when(emailDeliveryService).sendVerificationCode(eq(email), anyString(), anyString(), anyLong());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Verified User",
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nextResendAt").isNotEmpty());

        UserAccount pendingUser = userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(pendingUser.isActive()).isFalse();
        assertThat(pendingUser.isEmailVerified()).isFalse();
        assertThat(pendingUser.getPasswordHash()).isNotEqualTo(password).startsWith("$2");
        assertThat(verificationCode.get()).matches("\\d{6}");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.fields.secondsRemaining").isNotEmpty());

        assertThat(emailVerificationCodeRepository.findAllByUserAccountAndPurposeAndConsumedAtIsNull(
                pendingUser,
                EmailVerificationCode.PURPOSE_REGISTRATION
        )).hasSize(1);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "code", verificationCode.get()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.emailVerified").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        UserAccount verifiedUser = userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(verifiedUser.isActive()).isTrue();
        assertThat(verifiedUser.isEmailVerified()).isTrue();
        assertThat(verifiedUser.getVerifiedAt()).isNotNull();
        assertThat(verifiedUser.getLastLoginAt()).isNotNull();
    }

    @Test
    void registrationRejectsWeakPasswords() throws Exception {
        assertRegistrationPasswordRejected("Short1", "Password must be at least 8 characters.");
        assertRegistrationPasswordRejected("stockwise123", "Password must include at least one uppercase letter.");
        assertRegistrationPasswordRejected("STOCKWISE123", "Password must include at least one lowercase letter.");
        assertRegistrationPasswordRejected("Stockwise", "Password must include at least one number.");
        assertRegistrationPasswordRejected("Stock wise123", "Password must not contain spaces.");
    }

    @Test
    void loginRejectsShortPasswordBeforeAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "admin@smartinvent.local",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password must be at least 8 characters.")));
    }

    @Test
    void forgotPasswordRequestEmailsAdminWithoutRevealingAccountExistence() throws Exception {
        String email = "password-help@smartinvent.local";

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password update request has been submitted. Admin will contact you if the account is valid."));

        verify(emailDeliveryService).sendPasswordUpdateRequestToAdmin(eq(email), any(Instant.class));
    }

    private void assertRegistrationPasswordRejected(String password, String expectedMessage) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Weak Password User",
                                "email", "weak-" + Math.abs(password.hashCode()) + "@smartinvent.local",
                                "password", password
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(expectedMessage)));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
