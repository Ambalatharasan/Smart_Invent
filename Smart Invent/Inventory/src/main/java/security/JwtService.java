package com.stockwise.api.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockwise.api.entity.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expiresInMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${stockwise.security.jwt.secret}") String secret,
            @Value("${stockwise.security.jwt.expires-in-minutes}") long expiresInMinutes
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expiresInMinutes = expiresInMinutes;
    }

    public String createToken(UserAccount user) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getEmail());
        claims.put("name", user.getName());
        claims.put("role", "ROLE_" + user.getRole().name());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(expiresInMinutes * 60).getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedClaims = encodeJson(claims);
        String unsignedToken = encodedHeader + "." + encodedClaims;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public long getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public String extractSubject(String token) {
        Object subject = readClaims(token).get("sub");
        return subject == null ? null : subject.toString();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        try {
            Map<String, Object> claims = readClaims(token);
            Object subject = claims.get("sub");
            Object expiresAt = claims.get("exp");
            if (subject == null || !(expiresAt instanceof Number expiration)) {
                return false;
            }
            return userDetails.getUsername().equalsIgnoreCase(subject.toString())
                    && Instant.now().getEpochSecond() < expiration.longValue();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Map<String, Object> readClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        boolean signatureMatches = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII)
        );
        if (!signatureMatches) {
            throw new IllegalArgumentException("Invalid token signature");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid token payload", ex);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encode token", ex);
        }
    }

    private String sign(String unsignedToken) {
        try {
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                throw new IllegalStateException("JWT secret must be at least 32 bytes");
            }
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign token", ex);
        }
    }
}
