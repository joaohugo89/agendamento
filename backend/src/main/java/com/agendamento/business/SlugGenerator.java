package com.agendamento.business;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Turns a business name into a URL-safe slug, suffixing it when already taken. */
public final class SlugGenerator {

    private static final int MAX_LENGTH = 60;
    private static final int MAX_ATTEMPTS = 50;

    private SlugGenerator() {
    }

    public static String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (normalized.length() > MAX_LENGTH) {
            normalized = normalized.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return normalized.isBlank() ? "estabelecimento" : normalized;
    }

    public static String uniqueSlug(String value, Predicate<String> taken) {
        String base = slugify(value);
        if (!taken.test(base)) {
            return base;
        }
        for (int suffix = 2; suffix <= MAX_ATTEMPTS; suffix++) {
            String candidate = base + "-" + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        return base + "-" + System.currentTimeMillis();
    }
}
