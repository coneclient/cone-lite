package net.cone.gui.skija;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class SkijaLib {
    public enum State { IDLE, LOADING, READY, FAILED }

    private static final String SKIJA_VERSION = "0.143.17";
    private static final String TYPES_VERSION = "0.2.0";

    // Maven Central hosts the Skija runtime jars (shared + types + the per-OS native). Downloading
    // from it directly keeps the client off any private mirror.
    private static final String REPO_BASE = "https://repo1.maven.org/maven2";

    private static volatile State state = State.IDLE;
    private static volatile String message = "";

    private SkijaLib() {}

    public static State state() { return state; }

    public static String message() { return message; }

    public static synchronized void ensureStarted() {
        if (state != State.IDLE) return;
        String artifact = platformArtifact();
        if (artifact == null) {
            state = State.FAILED;
            message = "Unsupported platform: " + os() + " / " + arch();
            return;
        }
        state = State.LOADING;
        message = "Preparing renderer…";
        Thread t = new Thread(() -> load(artifact), "cone-skija-loader");
        t.setDaemon(true);
        t.start();
    }

    private static void load(String artifact) {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("cone").resolve("lib").resolve("skija");
            Files.createDirectories(dir);

            record Dep(String group, String id, String version) {
                String file() { return id + "-" + version + ".jar"; }
                String url(String base) {
                    return base + "/" + group.replace('.', '/') + "/" + id + "/" + version + "/" + file();
                }
            }
            List<Dep> deps = List.of(
                    new Dep("io.github.humbleui", "skija-shared", SKIJA_VERSION),
                    new Dep("io.github.humbleui", "types", TYPES_VERSION),
                    new Dep("io.github.humbleui", artifact, SKIJA_VERSION));

            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            for (Dep d : deps) {
                Path jar = dir.resolve(d.file());
                if (!Files.exists(jar) || sizeOf(jar) <= 0) {
                    message = "Downloading " + d.id() + "…";
                    download(http, d.url(REPO_BASE), jar);
                }
                FabricLauncherBase.getLauncher().addToClassPath(jar);
            }

            SkijaNative.load();
            state = State.READY;
            message = "Ready";
        } catch (Throwable e) {
            state = State.FAILED;
            message = "Renderer load failed: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " — " + e.getMessage());
            System.err.println("[Cone] Skija load failed");
            e.printStackTrace();
        }
    }

    private static void download(HttpClient http, String url, Path dest) throws IOException, InterruptedException {
        Path tmp = dest.resolveSibling(dest.getFileName() + ".part");
        HttpResponse<Path> res = http.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofFile(tmp));
        if (res.statusCode() != 200) {
            Files.deleteIfExists(tmp);
            throw new IOException("HTTP " + res.statusCode() + " for " + url);
        }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    private static long sizeOf(Path p) {
        try { return Files.size(p); } catch (IOException e) { return 0; }
    }

    private static String platformArtifact() {
        String os = os(), arch = arch();
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        if (os.contains("mac") || os.contains("darwin")) return arm ? "skija-macos-arm64" : "skija-macos-x64";
        if (os.contains("win")) return "skija-windows-x64";
        if (os.contains("linux")) return arm ? "skija-linux-arm64" : "skija-linux-x64";
        return null;
    }

    private static String os() { return System.getProperty("os.name", "").toLowerCase(); }
    private static String arch() { return System.getProperty("os.arch", "").toLowerCase(); }
}
