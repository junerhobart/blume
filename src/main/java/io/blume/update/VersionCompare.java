package io.blume.update;

import org.jetbrains.annotations.NotNull;

final class VersionCompare {

    private VersionCompare() {
    }

    static boolean isOlder(@NotNull String current, @NotNull String latest) {
        return compare(stripPrefix(current), stripPrefix(latest)) < 0;
    }

    static @NotNull String stripPrefix(@NotNull String version) {
        if (version.startsWith("v") || version.startsWith("V")) {
            return version.substring(1);
        }
        return version;
    }

    private static int compare(@NotNull String left, @NotNull String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int leftValue = index < leftParts.length ? parsePart(leftParts[index]) : 0;
            int rightValue = index < rightParts.length ? parsePart(rightParts[index]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static int parsePart(@NotNull String part) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        return Integer.parseInt(part.substring(0, end));
    }
}
