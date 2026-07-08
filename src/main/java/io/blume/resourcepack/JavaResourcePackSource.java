package io.blume.resourcepack;

import io.blume.config.BlumeConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.logging.Logger;

public final class JavaResourcePackSource implements AutoCloseable {

    private static final String PACK_RESOURCE = "java-pack.zip";
    private static final String PACK_FILE = "blume-pack.zip";
    private static final String PACK_PATH = "/blume-pack.zip";

    public record PackInfo(@NotNull String url, @NotNull String sha1) {
    }

    private final PackInfo packInfo;
    private final @Nullable HttpServer server;

    private JavaResourcePackSource(@NotNull PackInfo packInfo, @Nullable HttpServer server) {
        this.packInfo = packInfo;
        this.server = server;
    }

    public static @Nullable JavaResourcePackSource start(
        @NotNull Plugin plugin,
        @NotNull BlumeConfig config,
        @NotNull Logger log
    ) {
        if (!config.isResourcePackEnabled()) {
            return null;
        }

        try (InputStream bundled = plugin.getResource(PACK_RESOURCE)) {
            if (bundled == null) {
                log.severe("[Blume] Bundled java-pack.zip is missing from the plugin jar.");
                return null;
            }

            Path dataDir = plugin.getDataFolder().toPath();
            Files.createDirectories(dataDir);
            Path destination = dataDir.resolve(PACK_FILE);
            Files.copy(bundled, destination, StandardCopyOption.REPLACE_EXISTING);

            String sha1 = sha1Hex(destination);
            if (config.isResourcePackBuiltinHost() || config.shouldServeBundledPack()) {
                if (config.shouldServeBundledPack()) {
                    log.warning(
                        "[Blume] Non-release build ("
                            + plugin.getPluginMeta().getVersion()
                            + "); serving bundled resource pack locally instead of GitHub releases URL."
                    );
                }
                return startBuiltinHost(plugin, config, log, destination, sha1);
            }

            String url = config.getResourcePackUrl();
            log.info("[Blume] Java resource pack sha1 resolved from jar; clients download from " + url);
            return new JavaResourcePackSource(new PackInfo(url, sha1), null);
        } catch (IOException ex) {
            log.severe("[Blume] Failed to prepare Java resource pack: " + ex.getMessage());
            return null;
        }
    }

    public @NotNull PackInfo packInfo() {
        return packInfo;
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static @Nullable JavaResourcePackSource startBuiltinHost(
        @NotNull Plugin plugin,
        @NotNull BlumeConfig config,
        @NotNull Logger log,
        @NotNull Path packPath,
        @NotNull String sha1
    ) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.getResourcePackPort()), 0);
        server.createContext(PACK_PATH, exchange -> servePack(exchange, packPath));
        server.setExecutor(null);
        server.start();

        String host = resolveHost(plugin, config);
        String url = "http://" + host + ":" + config.getResourcePackPort() + PACK_PATH;
        if (isLocalHost(host)) {
            log.warning(
                "[Blume] Built-in pack host uses " + host
                    + ". Remote Java clients need resource-pack.host set to your public address."
            );
        }
        log.info("[Blume] Serving Java resource pack at " + url);
        return new JavaResourcePackSource(new PackInfo(url, sha1), server);
    }

    private static void servePack(@NotNull HttpExchange exchange, @NotNull Path packPath) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        byte[] bytes = Files.readAllBytes(packPath);
        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static @NotNull String sha1Hex(@NotNull Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 unavailable", ex);
        }
    }

    private static @NotNull String resolveHost(@NotNull Plugin plugin, @NotNull BlumeConfig config) {
        if (!config.getResourcePackHost().isBlank()) {
            return config.getResourcePackHost();
        }
        String serverIp = plugin.getServer().getIp();
        if (serverIp != null && !serverIp.isBlank()) {
            return serverIp;
        }
        return "127.0.0.1";
    }

    private static boolean isLocalHost(@NotNull String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "0.0.0.0".equals(host);
    }
}
