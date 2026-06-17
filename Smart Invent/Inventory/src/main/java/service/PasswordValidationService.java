package com.stockwise.api.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordValidationService {
    public static final String MIN_LENGTH_MESSAGE = "Password must be at least 8 characters.";
    public static final String UPPERCASE_MESSAGE = "Password must include at least one uppercase letter.";
    public static final String LOWERCASE_MESSAGE = "Password must include at least one lowercase letter.";
    public static final String NUMBER_MESSAGE = "Password must include at least one number.";
    public static final String SPACES_MESSAGE = "Password must not contain spaces.";

    public void validateLoginPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(MIN_LENGTH_MESSAGE);
        }
    }

    public void validateRegistrationPassword(String password) {
        validateLoginPassword(password);
        if (password.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(SPACES_MESSAGE);
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException(UPPERCASE_MESSAGE);
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException(LOWERCASE_MESSAGE);
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException(NUMBER_MESSAGE);
        }
    }
}
