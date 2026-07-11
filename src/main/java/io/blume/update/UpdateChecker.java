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

    private static final Pattern GITHUB_RELEASE = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

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
        String repo = config.getUpdateGithubRepo();
        if (repo.isBlank()) {
            return null;
        }
        return fetchGitHubLatestRelease(repo, userAgentVersion);
    }

    private static @Nullable LatestRelease fetchGitHubLatestRelease(
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
            Matcher matcher = GITHUB_RELEASE.matcher(response.body());
            if (matcher.find()) {
                String tag = matcher.group(1);
                String version = VersionCompare.stripPrefix(tag);
                return new LatestRelease(version, "https://github.com/" + repo + "/releases/tag/" + tag);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        }
        return null;
    }
}
