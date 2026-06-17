package com.stockwise.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE_NAME = "smartInventDotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = findEnvFile();
        if (envFile == null) {
            validateMysqlProfile(environment);
            return;
        }

        Map<String, Object> values = readEnvFile(envFile);
        if (values.isEmpty()) {
            validateMysqlProfile(environment);
            return;
        }
        values.put("SMART_INVENT_DOTENV_LOADED", "true");
        values.put("SMART_INVENT_DOTENV_PATH", envFile.toAbsolutePath().normalize().toString());
        addSpringPropertyAliases(values);

        MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
        } else {
            environment.getPropertySources().addFirst(propertySource);
        }

        validateMysqlProfile(environment);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private Path findEnvFile() {
        Path directory = Path.of("").toAbsolutePath().normalize();
        while (directory != null) {
            List<Path> candidates = List.of(
                    directory.resolve(".env"),
                    directory.resolve("smart-invent-spring-backend").resolve(".env")
            );
            for (Path candidate : candidates) {
                Path absolutePath = candidate.toAbsolutePath().normalize();
                if (Files.isRegularFile(absolutePath)) {
                    return absolutePath;
                }
            }
            directory = directory.getParent();
        }
        return null;
    }

    private void addSpringPropertyAliases(Map<String, Object> values) {
        putAlias(values, "MAIL_ENABLED", "stockwise.mail.enabled");
        putAlias(values, "MAIL_FROM", "stockwise.mail.from");
        putAlias(values, "ADMIN_EMAIL", "stockwise.mail.admin-address");
        putAlias(values, "STOCKWISE_ADMIN_EMAIL", "stockwise.mail.admin-address");
        putAlias(values, "FORGOT_PASSWORD_COOLDOWN_SECONDS", "stockwise.forgot-password.cooldown-seconds");
        putAlias(values, "SPRING_MAIL_HOST", "spring.mail.host");
        putAlias(values, "SPRING_MAIL_PORT", "spring.mail.port");
        putAlias(values, "SPRING_MAIL_USERNAME", "spring.mail.username");
        putAlias(values, "SPRING_MAIL_PASSWORD", "spring.mail.password");
        putAlias(values, "SPRING_MAIL_SMTP_AUTH", "spring.mail.properties.mail.smtp.auth");
        putAlias(values, "SPRING_MAIL_SMTP_STARTTLS_ENABLE", "spring.mail.properties.mail.smtp.starttls.enable");
    }

    private void putAlias(Map<String, Object> values, String sourceName, String targetName) {
        Object value = values.get(sourceName);
        if (value != null && !values.containsKey(targetName)) {
            values.put(targetName, value);
        }
    }

    private void validateMysqlProfile(ConfigurableEnvironment environment) {
        if (!isMysqlProfileActive(environment)) {
            return;
        }

        String dbUrl = environment.getProperty("DB_URL", "");
        boolean passwordInUrl = dbUrl.toLowerCase(Locale.ROOT).contains("password=");
        boolean passwordConfigured = hasText(environment.getProperty("DB_PASSWORD"))
                || hasText(environment.getProperty("MYSQLPASSWORD"))
                || passwordInUrl;

        if (!passwordConfigured) {
            throw new IllegalStateException(
                    "Smart Invent mysql profile is active, but no database password was provided. "
                            + "Your launch command currently sets only MAIL_* variables, so Spring falls back to "
                            + "the default 'stockwise' MySQL user with no password. Run .\\configure-existing-mysql.ps1 "
                            + "from smart-invent-spring-backend, then start with .\\run-mysql.ps1, or add DB_URL, "
                            + "DB_USERNAME, and DB_PASSWORD to smart-invent-spring-backend\\.env.");
        }
    }

    private boolean isMysqlProfileActive(ConfigurableEnvironment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "mysql".equalsIgnoreCase(profile));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Object> readEnvFile(Path envFile) {
        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String line = stripBom(rawLine).trim();
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex < 1) {
                    continue;
                }

                String name = stripBom(line.substring(0, separatorIndex).trim());
                String value = line.substring(separatorIndex + 1).trim();
                values.put(name, unquote(value));
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return values;
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private String stripBom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }
}
