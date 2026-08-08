package net.cone.gui.skija;

import net.cone.ConeClient;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FontLib {
    private static final String CSS_URL = "https://api.fontshare.com/v2/css?f%5B%5D=switzer@600";

    private static final Pattern TTF = Pattern.compile("url\\('(//[^']+\\.ttf)'\\)");

    private static final long MIN_BYTES = 8_192;

    public static final String FILE_NAME = "Switzer-Semibold.ttf";

    private static volatile boolean started;

    private FontLib() {}

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("cone").resolve("fonts");
    }

    public static void ensure() {
        if (started) return;
        started = true;
        Path target = dir().resolve(FILE_NAME);
        if (Files.isRegularFile(target) && sizeOf(target) >= MIN_BYTES) return;
        Thread t = new Thread(() -> fetch(target), "cone-font-loader");
        t.setDaemon(true);
        t.start();
    }

    private static void fetch(Path target) {
        try {
            Files.createDirectories(target.getParent());
            HttpClient http = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest cssReq = HttpRequest.newBuilder(URI.create(CSS_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "cone-client")
                    .GET().build();
            HttpResponse<String> css = http.send(cssReq, HttpResponse.BodyHandlers.ofString());
            if (css.statusCode() != 200) {
                ConeClient.LOG.info("[Cone] font: stylesheet returned {}, keeping the system face",
                        css.statusCode());
                return;
            }
            Matcher m = TTF.matcher(css.body());
            if (!m.find()) {
                ConeClient.LOG.info("[Cone] font: no ttf in the stylesheet, keeping the system face");
                return;
            }

            Path tmp = target.resolveSibling(FILE_NAME + ".part");
            HttpRequest fontReq = HttpRequest.newBuilder(URI.create("https:" + m.group(1)))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "cone-client")
                    .GET().build();
            HttpResponse<Path> res = http.send(fontReq, HttpResponse.BodyHandlers.ofFile(tmp));
            if (res.statusCode() != 200 || sizeOf(tmp) < MIN_BYTES) {
                Files.deleteIfExists(tmp);
                ConeClient.LOG.info("[Cone] font: download failed ({}), keeping the system face",
                        res.statusCode());
                return;
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            ConeClient.LOG.info("[Cone] font: cached {} ({} bytes)", FILE_NAME, sizeOf(target));
        } catch (Exception e) {
            ConeClient.LOG.info("[Cone] font: fetch failed ({}), keeping the system face", e.toString());
        }
    }

    private static long sizeOf(Path p) {
        try {
            return Files.size(p);
        } catch (Exception e) {
            return -1;
        }
    }
}
