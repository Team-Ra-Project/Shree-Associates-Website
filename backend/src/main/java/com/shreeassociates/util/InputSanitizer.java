package com.shreeassociates.util;

import java.util.regex.Pattern;

/**
 * Minimal, dependency-free sanitizer: strips HTML tags and collapses
 * whitespace so stored/forwarded data can't carry markup or script content.
 * Combined with JPA parameter binding (which already prevents SQL injection)
 * and Bean Validation on the DTO.
 */
public final class InputSanitizer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x09\\x0B\\x0C\\x0E-\\x1F]");

    private InputSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        String noTags = HTML_TAG.matcher(input).replaceAll("");
        String noControlChars = CONTROL_CHARS.matcher(noTags).replaceAll("");
        return noControlChars.trim().replaceAll("\\s{2,}", " ");
    }
}
