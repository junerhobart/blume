package io.blume.update;

import io.blume.config.BlumeConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    private static final Pattern MODRINTH_VERSION = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern GITHUB_TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private UpdateChecker() {
    }

    public static void checkAsync(@NotNull JavaPlugin plugin, @NotNull BlumeConfig config) {
        if (!config.isUpdateCheckEnabled()) {
            return;
        }

        String current = plugin.getPluginMeta().getVersion();
        Logger log = plugin.getLogger();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            LatestRelease latest = fetchLatestRelease(config, current);
            if (latest == null) {
                return;
            }
            if (VersionCompare.isOlder(current, latest.version())) {
                log.warning(
                    "[Blume] Out of date. Current: " + current
                        + ", latest: " + latest.version()
                        + ". " + latest.downloadUrl()
                );
            }
        });
    }

    private record LatestRelease(@NotNull String version, @NotNull String downloadUrl) {
    }

    private static @Nullable LatestRelease fetchLatestRelease(
        @NotNull BlumeConfig config,
        @NotNull String userAgentVersion
    ) {
        if (!config.getUpdateModrinthSlug().isBlank()) {
            LatestRelease fromModrinth = fetchModrinthRelease(config.getUpdateModrinthSlug(), userAgentVersion);
            if (fromModrinth != null) {
                return fromModrinth;
            }
        }
        return fetchGitHubRelease(config.getUpdateGithubRepo(), userAgentVersion);
    }

    private static @Nullable LatestRelease fetchModrinthRelease(
        @NotNull String slug,
        @NotNull String userAgentVersion
    ) {
        String url = "https://api.modrinth.com/v2/project/" + slug + "/version";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Blume/" + userAgentVersion)
                .GET()
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            Matcher matcher = MODRINTH_VERSION.matcher(response.body());
            if (matcher.find()) {
                String version = matcher.group(1);
                return new LatestRelease(version, "https://modrinth.com/plugin/" + slug);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        }
        return null;
    }

    private static @Nullable LatestRelease fetchGitHubRelease(
        @NotNull String repo,
        @NotNull String userAgentVersion
    ) {
        String url = "https://api.github.com/repos/" + repo + "/releases/latest";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Blume/" + userAgentVersion)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            Matcher matcher = GITHUB_TAG.matcher(response.body());
            if (matcher.find()) {
                String version = VersionCompare.stripPrefix(matcher.group(1));
                return new LatestRelease(version, "https://github.com/" + repo + "/releases/latest");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        }
        return null;
    }
}
