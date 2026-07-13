package com.bms.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    private PasswordHasher() {}

    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    public static boolean verify(String plain, String hashed) {
        if (plain == null || hashed == null || hashed.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(plain, hashed);
    }
}
