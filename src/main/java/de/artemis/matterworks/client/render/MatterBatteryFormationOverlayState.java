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

    public static void show(BlockPos originPos, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || durationTicks <= 0) {
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        ACTIVE_PULSES.put(originPos.asLong(), new FormationPulse(originPos.immutable(), gameTime + durationTicks, durationTicks));
    }

    public static Collection<FormationPulse> getActivePulses(long gameTime) {
        ACTIVE_PULSES.values().removeIf(pulse -> pulse.expireTick() <= gameTime);
        return ACTIVE_PULSES.values();
    }

    public static void clear() {
        ACTIVE_PULSES.clear();
    }

    public record FormationPulse(BlockPos originPos, long expireTick, int durationTicks) {
    }
}
