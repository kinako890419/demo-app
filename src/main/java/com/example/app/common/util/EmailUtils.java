package com.example.app.common.util;

import com.example.app.common.exception.BadRequestException;

public final class EmailUtils {

    private EmailUtils() {
    }

    /** Returns the part after the {@code @}, e.g. {@code test.com}. */
    public static String getDomain(String email) {
        int index = email == null ? -1 : email.indexOf('@');
        if (index > 0 && index < email.length() - 1) {
            return email.substring(index + 1);
        }
        throw new BadRequestException("Invalid email: " + email);
    }
}
