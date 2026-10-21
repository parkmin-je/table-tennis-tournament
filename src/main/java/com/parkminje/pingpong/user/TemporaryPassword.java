package com.parkminje.pingpong.user;

import java.security.SecureRandom;

final class TemporaryPassword {

    private static final String LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPassword() {
    }

    static String generate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        sb.append(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));
        return sb.toString();
    }
}
