package io.blume.config;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class ConfigPlaceholders {

    static final String PACK_URL_TEMPLATE =
        "https://github.com/${github-repo}/releases/download/v${version}/blume-pack.zip";

    private static final Pattern GITHUB_PACK_URL = Pattern.compile(
        "https://github\\.com/([^/]+/[^/]+)/releases/download/v[^/]+/blume-pack\\.zip"
    );

    private static final Pattern PINNED_RELEASE_SUFFIX = Pattern.compile(
        "^(\\d+\\.\\d+\\.\\d+)-(\\d+\\.\\d+(?:\\.\\d+)?)$"
    );

    private ConfigPlaceholders() {
    }

    static boolean isReleaseVersion(@NotNull String version) {
        if (version.isBlank() || version.contains("SNAPSHOT") || version.startsWith("0.0.0")) {
            return false;
        }
        if (!version.contains("-")) {
            return true;
        }
        return PINNED_RELEASE_SUFFIX.matcher(version).matches();
    }

    static @NotNull String releaseTag(@NotNull String version) {
        return version;
    }

    static @NotNull String resolvePackUrl(
        @NotNull String raw,
        @NotNull String version,
        @NotNull String githubRepo
    ) {
        if (!isReleaseVersion(version)) {
            return "";
        }

        String template = raw.isBlank() ? PACK_URL_TEMPLATE : raw;
        String tag = releaseTag(version);
        String withPlaceholders = template
            .replace("${version}", tag)
            .replace("${github-repo}", githubRepo);

        var matcher = GITHUB_PACK_URL.matcher(withPlaceholders);
        if (matcher.matches() && matcher.group(1).equals(githubRepo)) {
            return canonicalPackUrl(githubRepo, tag);
        }
        return withPlaceholders;
    }

    static boolean isAutoManagedPackUrl(@NotNull String raw, @NotNull String githubRepo) {
        if (raw.isBlank() || raw.contains("${version}") || raw.contains("${github-repo}")) {
            return true;
        }
        var matcher = GITHUB_PACK_URL.matcher(raw);
        return matcher.matches() && matcher.group(1).equals(githubRepo);
    }

    private static @NotNull String canonicalPackUrl(@NotNull String githubRepo, @NotNull String version) {
        return "https://github.com/" + githubRepo + "/releases/download/v" + version + "/blume-pack.zip";
    }

    static void selfCheck() {
        if (!isReleaseVersion("0.4.3")) {
            throw new AssertionError("expected release version 0.4.3");
        }
        if (!isReleaseVersion("0.4.6-1.21.8")) {
            throw new AssertionError("pinned release tag must be treated as release");
        }
        if (isReleaseVersion("0.4.3-2-gabc")) {
            throw new AssertionError("dirty version must not be treated as release");
        }
        if (isReleaseVersion("0.0.0-SNAPSHOT")) {
            throw new AssertionError("snapshot version must not be treated as release");
        }
        if (!resolvePackUrl(PACK_URL_TEMPLATE, "0.0.0-SNAPSHOT", "junerhobart/blume").isEmpty()) {
            throw new AssertionError("snapshot version must not resolve pack url");
        }
        if (!resolvePackUrl(PACK_URL_TEMPLATE, "0.4.3", "junerhobart/blume")
            .equals("https://github.com/junerhobart/blume/releases/download/v0.4.3/blume-pack.zip")) {
            throw new AssertionError("unexpected pack url for 0.4.3");
        }
        String pinned = resolvePackUrl(PACK_URL_TEMPLATE, "0.4.6-1.21.8", "junerhobart/blume");
        if (!pinned.equals("https://github.com/junerhobart/blume/releases/download/v0.4.6-1.21.8/blume-pack.zip")) {
            throw new AssertionError("unexpected pinned pack url: " + pinned);
        }
    }

    public static void main(String[] args) {
        selfCheck();
    }
}
