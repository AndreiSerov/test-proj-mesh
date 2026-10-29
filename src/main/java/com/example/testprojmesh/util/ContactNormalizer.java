package com.example.testprojmesh.util;

import java.util.Locale;

public final class ContactNormalizer {

    private ContactNormalizer() {
    }

    public static String email(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String phone(String raw) {
        return raw.replaceAll("\\D", "");
    }
}
