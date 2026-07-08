package io.blume.config;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

final class ConfigPlaceholders {

    static final String PACK_URL_TEMPLATE =
        "https://github.com/${github-repo}/releases/download/v${version}/blume-pack.zip";

    private static final Pattern GITHUB_PACK_URL = Pattern.compile(
        "https://github\\.com/([^/]+/[^/]+)/releases/download/v[^/]+/blume-pack\\.zip"
    );

    private ConfigPlaceholders() {
    }

    static @NotNull String resolvePackUrl(
        @NotNull String raw,
        @NotNull String version,
        @NotNull String githubRepo
    ) {
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
}
