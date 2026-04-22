package org.example.musicbooru.util;

import java.security.SecureRandom;
import java.util.function.Predicate;

public class PublicIdGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a unique Base62 encoded string to be used as a public ID.
     *
     * @param length         The string's character length.
     * @param maxRetries     The maximum number of generation attempts.
     * @param collisionCheck A boolean method that performs the collision check (most likely a repository method).
     * @return the generated string.
     */
    public static String generate(final int length, final int maxRetries, Predicate<String> collisionCheck) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String publicId = generateBase62String(length);
            if (!collisionCheck.test(publicId)) return publicId;
        }

        throw new RuntimeException("Failed to generate a unique public ID within " + maxRetries + " attempts");
    }

    private static String generateBase62String(final int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        return stringBuilder.toString();
    }
}
