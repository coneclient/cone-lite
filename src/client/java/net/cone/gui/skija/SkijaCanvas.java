package net.cone.gui.skija;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class SkijaCanvas {
    private static SkijaCanvas hud;

    private final Identifier texId;

    private int w, h;
    private NativeImage image;
    private Surface surface;
    private DynamicTexture texture;

    public SkijaCanvas(String name) {
        texId = Identifier.fromNamespaceAndPath("cone", "skija_" + name);
    }

    public static SkijaCanvas get() {
        if (hud == null) hud = new SkijaCanvas("hud");
        return hud;
    }

    private void resize(int pw, int ph) {
        if (pw == w && ph == h && surface != null) return;
        free();
        w = Math.max(1, pw);
        h = Math.max(1, ph);
        image = new NativeImage(NativeImage.Format.RGBA, w, h, false);

        surface = Surface.makeRasterDirect(
                new ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL),
                image.getPointer(), w * 4L);
        texture = new DynamicTexture(() -> "cone-skija-" + texId.getPath(), image);
        Minecraft.getInstance().getTextureManager().register(texId, texture);
    }

    public Canvas begin(int uiW, int uiH, double scale) {
        double k = cap(uiW, uiH, scale);
        int pw = (int) Math.ceil(uiW * scale * k);
        int ph = (int) Math.ceil(uiH * scale * k);
        resize(pw, ph);
        Canvas c = surface.getCanvas();
        c.clear(0x00000000);
        c.save();
        c.scale((float) (scale * k), (float) (scale * k));
        return c;
    }

    private static double cap(int uiW, int uiH, double scale) {
        int max = SkDesign.rasterCap();
        double pw = uiW * scale, ph = uiH * scale;
        double k = Math.min(max / Math.max(1, pw), max / Math.max(1, ph));
        return Math.min(1, k);
    }

    public void end() {
        surface.getCanvas().restore();

        texture.upload();
    }

    public void blit(GuiGraphicsExtractor g, float x, float y, float logicalW, float logicalH) {
        if (surface == null) return;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(logicalW / w, logicalH / h);
        g.blit(RenderPipelines.GUI_TEXTURED, texId, 0, 0, 0f, 0f, w, h, w, h);
        pose.popMatrix();
    }

    private void free() {
        if (texture != null) { texture.close(); texture = null; }
        if (surface != null) { surface.close(); surface = null; }
        if (image != null) { image.close(); image = null; }
        w = h = 0;
    }
}
