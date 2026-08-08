package net.cone.gui.skija;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontSlant;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.FontWeight;
import io.github.humbleui.skija.FontWidth;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.RRect;
import net.cone.ConeClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class Sk {
    private static final Paint FILL = new Paint().setAntiAlias(true).setMode(PaintMode.FILL);
    private static final Paint STROKE = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE);

    private static Typeface typeface;
    private static Typeface typefaceBold;
    private static final Map<Integer, Font> FONTS = new HashMap<>();
    private static final Map<Integer, Font> FONTS_BOLD = new HashMap<>();

    private Sk() {}

    private static final String[] FILES = { "Switzer-Semibold.otf", FontLib.FILE_NAME };

    private static final String[] FAMILIES = {
            "SF Pro Text", "SF Pro Display", "SF Pro", "SF UI Text",
            ".SF NS Text", ".SF NS", ".AppleSystemUIFont",
            "Inter", "Segoe UI Variable Text", "Segoe UI", "Roboto", "Helvetica Neue",
    };

    private static final FontStyle MEDIUM =
            new FontStyle(FontWeight.MEDIUM, FontWidth.NORMAL, FontSlant.UPRIGHT);
    private static final FontStyle SEMIBOLD =
            new FontStyle(FontWeight.SEMI_BOLD, FontWidth.NORMAL, FontSlant.UPRIGHT);

    private static Typeface fromFile() {
        try {
            Path dir = FontLib.dir();
            for (String name : FILES) {
                Path p = dir.resolve(name);
                if (!Files.isRegularFile(p)) continue;
                Typeface tf = FontMgr.getDefault().makeFromFile(p.toAbsolutePath().toString());
                if (tf != null) return tf;
            }
        } catch (Exception e) {
            ConeClient.LOG.warn("[Cone] font file load failed, using a system face", e);
        }
        return null;
    }

    private static Typeface match(FontStyle style) {
        FontMgr mgr = FontMgr.getDefault();
        for (String family : FAMILIES) {
            Typeface tf = mgr.matchFamilyStyle(family, style);

            if (tf != null && tf.getFamilyName() != null
                    && tf.getFamilyName().equalsIgnoreCase(family)) {
                return tf;
            }
        }
        return mgr.matchFamilyStyle(null, style);
    }

    private static Typeface typeface() {
        if (typeface == null) {
            typeface = fromFile();
            if (typeface == null) typeface = match(MEDIUM);
        }
        return typeface;
    }

    private static Typeface typefaceBold() {
        if (typefaceBold == null) {
            typefaceBold = fromFile();
            if (typefaceBold == null) typefaceBold = match(SEMIBOLD);
            if (typefaceBold == null) typefaceBold = typeface();
        }
        return typefaceBold;
    }

    static Font font(int size) {
        return FONTS.computeIfAbsent(size, s -> new Font(typeface(), s).setSubpixel(true));
    }

    static Font fontBold(int size) {
        return FONTS_BOLD.computeIfAbsent(size, s -> new Font(typefaceBold(), s).setSubpixel(true));
    }

    static void fillRect(Canvas c, float x, float y, float w, float h, int argb) {
        FILL.setColor(argb);
        c.drawRect(io.github.humbleui.types.Rect.makeXYWH(x, y, w, h), FILL);
    }

    static void fillRound(Canvas c, float x, float y, float w, float h, float r, int argb) {
        FILL.setColor(argb);
        c.drawRRect(RRect.makeXYWH(x, y, w, h, r), FILL);
    }

    static void strokeRound(Canvas c, float x, float y, float w, float h, float r, int argb, float width) {
        STROKE.setColor(argb).setStrokeWidth(width);
        c.drawRRect(RRect.makeXYWH(x, y, w, h, r), STROKE);
    }

    static void fillLeaf(Canvas c, float x, float y, float w, float h, float big, float small, int argb) {
        FILL.setColor(argb);
        c.drawRRect(RRect.makeComplexXYWH(x, y, w, h, new float[] { big, small, big, small }), FILL);
    }

    static void strokeLeaf(Canvas c, float x, float y, float w, float h, float big, float small, int argb, float width) {
        STROKE.setColor(argb).setStrokeWidth(width);
        c.drawRRect(RRect.makeComplexXYWH(x, y, w, h, new float[] { big, small, big, small }), STROKE);
    }

    static void line(Canvas c, float x1, float y1, float x2, float y2, int argb, float width) {
        STROKE.setColor(argb).setStrokeWidth(width);
        c.drawLine(x1, y1, x2, y2, STROKE);
    }

    static void fillTriangle(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        io.github.humbleui.skija.Path p = io.github.humbleui.skija.Path.makePolygon(
                new io.github.humbleui.types.Point[] {
                        new io.github.humbleui.types.Point(x1, y1),
                        new io.github.humbleui.types.Point(x2, y2),
                        new io.github.humbleui.types.Point(x3, y3)
                }, true);
        FILL.setColor(argb);
        c.drawPath(p, FILL);
        p.close();
    }

    static void fillRoundVGradient(Canvas c, float x, float y, float w, float h, float r, int top, int bottom) {
        Paint p = new Paint().setAntiAlias(true);
        p.setShader(io.github.humbleui.skija.Shader.makeLinearGradient(x, y, x, y + h, new int[] { top, bottom }));
        c.drawRRect(RRect.makeXYWH(x, y, w, h, r), p);
        p.close();
    }

    static void clip(Canvas c, float x, float y, float w, float h) {
        c.save();
        c.clipRect(io.github.humbleui.types.Rect.makeXYWH(x, y, w, h));
    }

    static void unclip(Canvas c) {
        c.restore();
    }

    static String ellipsize(int size, String s, float maxW) {
        if (textWidth(size, s) <= maxW) return s;
        String dots = "...";
        int end = s.length();
        while (end > 1 && textWidth(size, s.substring(0, end) + dots) > maxW) end--;
        return s.substring(0, end) + dots;
    }

    static float textWidth(int size, String s) {
        return font(size).measureTextWidth(s);
    }

    static void text(Canvas c, String s, float x, float topY, int size, int argb) {
        draw(c, s, x, topY, font(size), argb);
    }

    static void textBold(Canvas c, String s, float x, float topY, int size, int argb) {
        draw(c, s, x, topY, fontBold(size), argb);
    }

    private static void draw(Canvas c, String s, float x, float topY, Font f, int argb) {
        FontMetrics m = f.getMetrics();
        FILL.setColor(argb);
        c.drawString(s, x, topY - m.getAscent(), f, FILL);
    }

    static void textVCenter(Canvas c, String s, float x, float y, float h, int size, int argb) {
        vCenter(c, s, x, y, h, font(size), argb);
    }

    static void textVCenterBold(Canvas c, String s, float x, float y, float h, int size, int argb) {
        vCenter(c, s, x, y, h, fontBold(size), argb);
    }

    private static void vCenter(Canvas c, String s, float x, float y, float h, Font f, int argb) {
        FontMetrics m = f.getMetrics();
        float baseline = y + (h - (m.getDescent() - m.getAscent())) / 2f - m.getAscent();
        FILL.setColor(argb);
        c.drawString(s, x, baseline, f, FILL);
    }
}
