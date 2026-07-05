package de.artemis.matterworks.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MatterBatteryFormationOverlayState {
    private static final Map<Long, FormationPulse> ACTIVE_PULSES = new LinkedHashMap<>();

    private MatterBatteryFormationOverlayState() {
    }

    public static void show(BlockPos minPos, int sizeX, int sizeY, int sizeZ, int durationTicks, PulseType pulseType) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || durationTicks <= 0) {
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        long key = minPos.asLong() ^ ((long) pulseType.ordinal() << 60);
        ACTIVE_PULSES.put(key, new FormationPulse(minPos.immutable(), sizeX, sizeY, sizeZ, gameTime + durationTicks, durationTicks, pulseType));
    }

    public static Collection<FormationPulse> getActivePulses(long gameTime) {
        ACTIVE_PULSES.values().removeIf(pulse -> pulse.expireTick() <= gameTime);
        return ACTIVE_PULSES.values();
    }

    public static void clear() {
        ACTIVE_PULSES.clear();
    }

    public record FormationPulse(BlockPos minPos, int sizeX, int sizeY, int sizeZ, long expireTick, int durationTicks, PulseType pulseType) {
    }

    public enum PulseType {
        FORMED,
        BROKEN
    }
}
