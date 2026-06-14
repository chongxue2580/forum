package com.example.blog_froum.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于TOTP的一次性验证码服务，兼容Google Authenticator等验证器。
 */
@Service
public class TwoFactorService {

    public enum ChallengeType {
        LOGIN,
        ADMIN_SETUP
    }

    public static class PendingChallenge {
        private final Long userId;
        private final String secret;
        private final ChallengeType type;
        private final Instant expiresAt;

        public PendingChallenge(Long userId, String secret, ChallengeType type, Instant expiresAt) {
            this.userId = userId;
            this.secret = secret;
            this.type = type;
            this.expiresAt = expiresAt;
        }

        public Long getUserId() {
            return userId;
        }

        public String getSecret() {
            return secret;
        }

        public ChallengeType getType() {
            return type;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String ISSUER = "FroumX";
    private static final long TIME_STEP_SECONDS = 30L;
    private static final int DIGITS = 6;
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, PendingChallenge> pendingChallenges = new ConcurrentHashMap<>();

    public String generateSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String buildOtpAuthUrl(String username, String secret) {
        String label = ISSUER + ":" + username;
        return "otpauth://totp/"
                + urlEncode(label)
                + "?secret=" + urlEncode(secret)
                + "&issuer=" + urlEncode(ISSUER)
                + "&algorithm=SHA1&digits=" + DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    public boolean verifyCode(String secret, String code) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
            return false;
        }

        String normalizedCode = code.replace(" ", "").trim();
        if (!normalizedCode.matches("\\d{" + DIGITS + "}")) {
            return false;
        }

        long currentCounter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            String expected = generateTotp(secret, currentCounter + offset);
            if (constantTimeEquals(expected, normalizedCode)) {
                return true;
            }
        }
        return false;
    }

    public String createLoginChallenge(Long userId) {
        return createChallenge(userId, null, ChallengeType.LOGIN);
    }

    public String createAdminSetupChallenge(Long userId, String secret) {
        return createChallenge(userId, secret, ChallengeType.ADMIN_SETUP);
    }

    public PendingChallenge getChallenge(String token, ChallengeType expectedType) {
        cleanExpiredChallenges();
        if (!StringUtils.hasText(token)) {
            return null;
        }

        PendingChallenge challenge = pendingChallenges.get(token);
        if (challenge == null || challenge.isExpired() || challenge.getType() != expectedType) {
            pendingChallenges.remove(token);
            return null;
        }
        return challenge;
    }

    public void consumeChallenge(String token) {
        if (StringUtils.hasText(token)) {
            pendingChallenges.remove(token);
        }
    }

    private String createChallenge(Long userId, String secret, ChallengeType type) {
        cleanExpiredChallenges();
        String token = UUID.randomUUID().toString().replace("-", "");
        pendingChallenges.put(token, new PendingChallenge(
                userId,
                secret,
                type,
                Instant.now().plus(CHALLENGE_TTL)));
        return token;
    }

    private void cleanExpiredChallenges() {
        pendingChallenges.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String generateTotp(String secret, long counter) {
        try {
            byte[] key = base32Decode(secret);
            byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("两步验证码生成失败", e);
        }
    }

    private String base32Encode(byte[] bytes) {
        StringBuilder result = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

    private byte[] base32Decode(String value) {
        String normalized = value.replace("=", "")
                .replace(" ", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        int buffer = 0;
        int bitsLeft = 0;
        byte[] output = new byte[normalized.length() * 5 / 8];
        int count = 0;

        for (char character : normalized.toCharArray()) {
            int index = BASE32_ALPHABET.indexOf(character);
            if (index < 0) {
                throw new IllegalArgumentException("TOTP密钥格式无效");
            }
            buffer = (buffer << 5) | index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return count == output.length ? output : Arrays.copyOf(output, count);
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestCompat.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static class MessageDigestCompat {
        private static boolean isEqual(byte[] left, byte[] right) {
            if (left == null || right == null) {
                return false;
            }

            int diff = left.length ^ right.length;
            for (int i = 0; i < Math.min(left.length, right.length); i++) {
                diff |= left[i] ^ right[i];
            }
            return diff == 0;
        }
    }
}
