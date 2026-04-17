package org.example.musicbooru.util;

import java.security.SecureRandom;

public class PublicIdGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a Base62 encoded string to be used as a Public ID.
     *
     * @param length The length of the string in characters.
     * @return The generated string.
     */
    public static String generate(final int length) {

        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        return stringBuilder.toString();
    }
}
