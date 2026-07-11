package dev.braggestbacon97.windynamics.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class WindManager {
    private WindManager() {}

    public static WindSample sample(ServerLevel level, BlockPos pos) {
        return WindSavedData.get(level).sample(level, pos);
    }

    public static void onServerLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        WindSavedData.get(serverLevel).setWorldSeed(serverLevel.getSeed());
    }
}
