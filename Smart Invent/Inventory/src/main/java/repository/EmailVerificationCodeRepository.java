package com.stockwise.api.repository;

import com.stockwise.api.entity.EmailVerificationCode;
import com.stockwise.api.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findFirstByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(String email, String purpose);

    Optional<EmailVerificationCode> findFirstByUserAccountAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(UserAccount userAccount, String purpose);

    List<EmailVerificationCode> findAllByUserAccountAndPurposeAndConsumedAtIsNull(UserAccount userAccount, String purpose);
}
