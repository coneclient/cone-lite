package net.cone.core.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class PlayerState {
    private PlayerState() {}

    public static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    public static boolean valid() {
        return player() != null;
    }

    public static Vec3 pos() {
        return player().position();
    }

    public static BlockPos blockPos() {
        return player().blockPosition();
    }

    public static float yaw() {
        return player().getYRot();
    }

    public static float pitch() {
        return player().getXRot();
    }

    public static boolean onGround() {
        return player().onGround();
    }

    public static float health() {
        return player().getHealth();
    }
}
