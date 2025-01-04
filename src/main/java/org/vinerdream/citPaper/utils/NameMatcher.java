package org.vinerdream.citPaper.utils;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NameMatcher {
    public static final int CASE_INSENSITIVE_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    public static Pattern filterToPattern(String filter) {
        if (filter == null) return null;

        try {
            if (filter.startsWith("pattern:")) {
                return Pattern.compile(patternToRegex(removePrefix(filter, "pattern:")));
            } else if (filter.startsWith("ipattern:")) {
                return Pattern.compile(patternToRegex(removePrefix(filter, "ipattern:")), CASE_INSENSITIVE_FLAGS);
            } else if (filter.startsWith("regex:")) {
                return Pattern.compile(removePrefix(filter, "regex:"));
            } else if (filter.startsWith("iregex:")) {
                return Pattern.compile(removePrefix(filter, "iregex:"), CASE_INSENSITIVE_FLAGS);
            }
            return Pattern.compile(Pattern.quote(filter));
        } catch (PatternSyntaxException e) {
            return Pattern.compile(Pattern.quote(filter.replaceFirst("[^:]+:", "")));
        }
    }

    private static String patternToRegex(String pattern) {
        return "^" + pattern.replace("*", ".*") + "$";
    }

    private static String removePrefix(String string, String prefix) {
        return string.substring(prefix.length());
    }
}
