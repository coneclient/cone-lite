package net.cone.gui.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.cone.gui.Ui;

public final class Fx {
    private static final long T0 = System.nanoTime();

    private Fx() {}

    public static float time() {
        return (System.nanoTime() - T0) / 1_000_000_000f;
    }

    public static float pulse(float hz, float phase) {
        return 0.5f + 0.5f * (float) Math.sin(time() * hz * (Math.PI * 2) + phase);
    }

    public static void line(PoseStack pose, VertexConsumer buf,
                            double x0, double y0, double z0, double x1, double y1, double z1,
                            int argb, float width) {
        PoseStack.Pose p = pose.last();
        float nx = (float) (x1 - x0), ny = (float) (y1 - y0), nz = (float) (z1 - z0);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-4f) return;
        nx /= len; ny /= len; nz /= len;
        buf.addVertex(p, (float) x0, (float) y0, (float) z0).setColor(argb).setNormal(p, nx, ny, nz).setLineWidth(width);
        buf.addVertex(p, (float) x1, (float) y1, (float) z1).setColor(argb).setNormal(p, nx, ny, nz).setLineWidth(width);
    }

    public static void box(PoseStack pose, VertexConsumer buf,
                           double x0, double y0, double z0, double x1, double y1, double z1,
                           int argb, float width) {
        line(pose, buf, x0, y0, z0, x1, y0, z0, argb, width);
        line(pose, buf, x1, y0, z0, x1, y0, z1, argb, width);
        line(pose, buf, x1, y0, z1, x0, y0, z1, argb, width);
        line(pose, buf, x0, y0, z1, x0, y0, z0, argb, width);

        line(pose, buf, x0, y1, z0, x1, y1, z0, argb, width);
        line(pose, buf, x1, y1, z0, x1, y1, z1, argb, width);
        line(pose, buf, x1, y1, z1, x0, y1, z1, argb, width);
        line(pose, buf, x0, y1, z1, x0, y1, z0, argb, width);

        line(pose, buf, x0, y0, z0, x0, y1, z0, argb, width);
        line(pose, buf, x1, y0, z0, x1, y1, z0, argb, width);
        line(pose, buf, x1, y0, z1, x1, y1, z1, argb, width);
        line(pose, buf, x0, y0, z1, x0, y1, z1, argb, width);
    }

    public static void corners(PoseStack pose, VertexConsumer buf,
                               double x0, double y0, double z0, double x1, double y1, double z1,
                               float frac, int argb, float width) {
        double lx = (x1 - x0) * frac, ly = (y1 - y0) * frac, lz = (z1 - z0) * frac;
        for (int c = 0; c < 8; c++) {
            double cx = (c & 1) == 0 ? x0 : x1;
            double cy = (c & 2) == 0 ? y0 : y1;
            double cz = (c & 4) == 0 ? z0 : z1;
            double sx = (c & 1) == 0 ? lx : -lx;
            double sy = (c & 2) == 0 ? ly : -ly;
            double sz = (c & 4) == 0 ? lz : -lz;
            line(pose, buf, cx, cy, cz, cx + sx, cy, cz, argb, width);
            line(pose, buf, cx, cy, cz, cx, cy + sy, cz, argb, width);
            line(pose, buf, cx, cy, cz, cx, cy, cz + sz, argb, width);
        }
    }

    public static void ring(PoseStack pose, VertexConsumer buf,
                            double cx, double cy, double cz, double r,
                            int segments, float dashFrac, float spin, int argb, float width) {
        double step = Math.PI * 2 / segments;
        for (int i = 0; i < segments; i++) {
            double a0 = i * step + spin;
            double a1 = a0 + step * dashFrac;
            line(pose, buf,
                    cx + Math.cos(a0) * r, cy, cz + Math.sin(a0) * r,
                    cx + Math.cos(a1) * r, cy, cz + Math.sin(a1) * r, argb, width);
        }
    }

    public static void beam(PoseStack pose, VertexConsumer buf,
                            double x, double z, double y0, double y1, int argb, float width) {
        int steps = 8;
        double dy = (y1 - y0) / steps;
        for (int i = 0; i < steps; i++) {
            float fade = 1f - i / (float) steps;
            line(pose, buf, x, y0 + i * dy, z, x, y0 + (i + 1) * dy, z,
                    Ui.alpha(argb, fade), width);
        }
    }

    public static void energyLine(PoseStack pose, VertexConsumer buf,
                                  double x0, double y0, double z0, double x1, double y1, double z1,
                                  float phase, int argb, float width) {
        int steps = 14;
        for (int i = 0; i < steps; i++) {
            float f0 = i / (float) steps, f1 = (i + 1) / (float) steps;

            float mid = (f0 + f1) * 0.5f;
            float d = Math.abs(mid - phase);
            d = Math.min(d, 1f - d);
            float bright = 0.30f + 0.70f * (float) Math.exp(-(d * d) / 0.008f);
            Fx.line(pose, buf,
                    x0 + (x1 - x0) * f0, y0 + (y1 - y0) * f0, z0 + (z1 - z0) * f0,
                    x0 + (x1 - x0) * f1, y0 + (y1 - y0) * f1, z0 + (z1 - z0) * f1,
                    Ui.alpha(argb, bright), width);
        }
    }

    public static void flowLine(PoseStack pose, VertexConsumer buf,
                                double x0, double y0, double z0, double x1, double y1, double z1,
                                double dashLen, float phase, int argb, float width) {
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4) return;
        double period = dashLen * 2;
        double off = (phase % 1f) * period;
        for (double s = -off; s < len; s += period) {
            double a = Math.max(0, s), b = Math.min(len, s + dashLen);
            if (b <= a) continue;
            double fa = a / len, fb = b / len;
            line(pose, buf,
                    x0 + dx * fa, y0 + dy * fa, z0 + dz * fa,
                    x0 + dx * fb, y0 + dy * fb, z0 + dz * fb, argb, width);
        }
    }

    public static void burst(PoseStack pose, VertexConsumer buf,
                             double cx, double cy, double cz, double maxR, float t,
                             int argb, float width) {
        if (t < 0 || t >= 1) return;
        float ease = 1f - (1f - t) * (1f - t);
        float alpha = 1f - t;
        ring(pose, buf, cx, cy, cz, maxR * ease, 24, 0.7f, t * 2f, Ui.alpha(argb, alpha), width);
    }
}
