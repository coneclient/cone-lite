package net.cone.core.event;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class TickBus {
    @FunctionalInterface
    public interface Listener {
        void onTick(Minecraft mc);
    }

    private final List<Listener> listeners = new ArrayList<>();

    public void register(Listener listener) {
        listeners.add(listener);
    }

    public void dispatch(Minecraft mc) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onTick(mc);
        }
    }
}
