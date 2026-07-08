package io.blume.config;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class ConfigPlaceholders {

    static final String PACK_URL_TEMPLATE =
        "https://github.com/${github-repo}/releases/download/v${version}/blume-pack.zip";

    private static final Pattern GITHUB_PACK_URL = Pattern.compile(
        "https://github\\.com/([^/]+/[^/]+)/releases/download/v[^/]+/blume-pack\\.zip"
    );

    private ConfigPlaceholders() {
    }

    static boolean isReleaseVersion(@NotNull String version) {
        if (version.isBlank() || version.contains("SNAPSHOT") || version.startsWith("0.0.0")) {
            return false;
        }
        return !version.contains("-");
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
        String withPlaceholders = template
            .replace("${version}", version)
            .replace("${github-repo}", githubRepo);

        var matcher = GITHUB_PACK_URL.matcher(withPlaceholders);
        if (matcher.matches() && matcher.group(1).equals(githubRepo)) {
            return canonicalPackUrl(githubRepo, version);
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
        assert isReleaseVersion("0.4.3");
        assert !isReleaseVersion("0.4.3-2-gabc");
        assert !isReleaseVersion("0.0.0-SNAPSHOT");
        assert resolvePackUrl(PACK_URL_TEMPLATE, "0.0.0-SNAPSHOT", "junerhobart/blume").isEmpty();
        assert resolvePackUrl(PACK_URL_TEMPLATE, "0.4.3", "junerhobart/blume")
            .equals("https://github.com/junerhobart/blume/releases/download/v0.4.3/blume-pack.zip");
    }

    public static void main(String[] args) {
        selfCheck();
    }
}
