package io.blume.update;

import org.jetbrains.annotations.NotNull;

public final class VersionCompare {

    private VersionCompare() {
    }

    static boolean isOlder(@NotNull String current, @NotNull String latest) {
        return compare(stripPrefix(current), stripPrefix(latest)) < 0;
    }

    public static boolean isBeta(@NotNull String version) {
        String v = stripPrefix(version);
        return v.matches("^\\d+\\.\\d+\\.\\d+-beta\\.\\d+.*");
    }

    static @NotNull String stripPrefix(@NotNull String version) {
        if (version.startsWith("v") || version.startsWith("V")) {
            return version.substring(1);
        }
        return version;
    }

    private static int compare(@NotNull String left, @NotNull String right) {
        String leftBase = baseVersion(left);
        String rightBase = baseVersion(right);
        int baseResult = compareBase(leftBase, rightBase);
        if (baseResult != 0) {
            return baseResult;
        }

        boolean leftBeta = isBeta(left);
        boolean rightBeta = isBeta(right);
        if (leftBeta && rightBeta) {
            int betaResult = Integer.compare(betaNumber(left), betaNumber(right));
            if (betaResult != 0) {
                return betaResult;
            }
        } else if (leftBeta != rightBeta) {
            return leftBeta ? -1 : 1;
        }

        boolean leftDirty = hasSuffix(left);
        boolean rightDirty = hasSuffix(right);
        if (leftDirty == rightDirty) {
            return 0;
        }
        return leftDirty ? -1 : 1;
    }

    private static @NotNull String baseVersion(@NotNull String version) {
        int betaIndex = version.indexOf("-beta.");
        if (betaIndex >= 0) {
            return version.substring(0, betaIndex);
        }
        int dashIndex = version.indexOf('-');
        if (dashIndex >= 0) {
            return version.substring(0, dashIndex);
        }
        return version;
    }

    private static int compareBase(@NotNull String left, @NotNull String right) {
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

    private static boolean hasSuffix(@NotNull String version) {
        String v = stripPrefix(version);
        return v.indexOf('-') >= 0;
    }

    private static int betaNumber(@NotNull String version) {
        int index = version.indexOf("-beta.");
        if (index < 0) {
            return 0;
        }
        String suffix = version.substring(index + 6);
        int end = 0;
        while (end < suffix.length() && Character.isDigit(suffix.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        return Integer.parseInt(suffix.substring(0, end));
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
