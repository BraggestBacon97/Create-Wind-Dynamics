package dev.braggestbacon97.windynamics.integration.create;

import dev.braggestbacon97.windynamics.integration.create.contraption.ContraptionSailScanResult;
import dev.braggestbacon97.windynamics.integration.create.contraption.SailSuCalculator;
import dev.braggestbacon97.windynamics.world.WeatherState;
import dev.braggestbacon97.windynamics.world.WindSample;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Central Create-facing helpers for the first real Sail Hub integration.
 */
public final class CreateBridge {
    public static final String CREATE_MOD_ID = "create";
    public static final String AERONAUTICS_MOD_ID = "aeronautics";

    private CreateBridge() {
    }

    public static void bootstrap() {
        if (isCreateLoaded()) {
            dev.braggestbacon97.windynamics.WindDynamicsMod.LOGGER.info("Create integration enabled");
        }
        if (isAeronauticsLoaded()) {
            dev.braggestbacon97.windynamics.WindDynamicsMod.LOGGER.info("Create Aeronautics integration enabled");
        }
    }

    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded(CREATE_MOD_ID);
    }

    public static boolean isAeronauticsLoaded() {
        return ModList.get().isLoaded(AERONAUTICS_MOD_ID);
    }

    public static float computeGeneratedSpeed(final WindSample sample, @Nullable final Direction facing, final int sailBlocks) {
        if (sample.isCalm() || sailBlocks <= 0) {
            return 0.0f;
        }

        float exposure = computeExposure(sample, facing);
        float weatherMultiplier = switch (sample.weather()) {
            case CLEAR -> 1.0f;
            case RAIN -> 1.08f;
            case THUNDER -> 1.2f;
        };

        float turbulencePenalty = 1.0f - Mth.clamp(sample.turbulence(), 0.0f, 1.0f) * 0.35f;
        float sailFactor = Mth.clamp(sailBlocks / 16.0f, 0.25f, 16.0f);

        float speed = sample.speed() * 16.0f * exposure * weatherMultiplier * turbulencePenalty * sailFactor;
        return Mth.clamp(speed, 0.0f, 256.0f);
    }

    public static float computeStressCapacity(final WindSample sample, @Nullable final Direction facing, final int sailBlocks, final float heightBonus) {
        if (sample.isCalm() || sailBlocks <= 0) {
            return 0.0f;
        }

        float exposure = computeExposure(sample, facing);
        float weatherMultiplier = switch (sample.weather()) {
            case CLEAR -> 1.0f;
            case RAIN -> 1.12f;
            case THUNDER -> 1.25f;
        };

        float pressureMultiplier = Mth.clamp(1.0f + (1.0f - sample.pressure()) * 0.15f, 0.85f, 1.25f);
        float turbulencePenalty = 1.0f - Mth.clamp(sample.turbulence(), 0.0f, 1.0f) * 0.25f;
        float sailFactor = Mth.clamp(sailBlocks / 16.0f, 0.25f, 16.0f);

        float base = 8.0f * sailFactor * exposure * weatherMultiplier * pressureMultiplier * turbulencePenalty * heightBonus;
        return Mth.clamp(base, 0.0f, 1024.0f);
    }

    public static float computeEstimatedSu(final WindSample sample, @Nullable final Direction facing, final int sailBlocks, final float heightBonus) {
        float generatedSpeed = computeGeneratedSpeed(sample, facing, sailBlocks);
        float capacityPerSpeed = computeStressCapacity(sample, facing, sailBlocks, heightBonus);
        return generatedSpeed * capacityPerSpeed;
    }

    public static float computeEstimatedSu(final WindSample sample,
                                           @Nullable final Direction facing,
                                           final ContraptionSailScanResult scan,
                                           final float heightBonus) {
        return SailSuCalculator.computeEstimatedSu(sample, facing, scan, heightBonus);
    }

    public static float computeHeightBonus(final int y) {
        return SailSuCalculator.computeHeightBonus(y);
    }

    private static float computeExposure(final WindSample sample, @Nullable final Direction facing) {
        if (facing == null) {
            return 1.0f;
        }

        float x = sample.direction().x;
        float z = sample.direction().y;

        return switch (facing) {
            case NORTH -> clampExposure(-z);
            case SOUTH -> clampExposure(z);
            case EAST -> clampExposure(x);
            case WEST -> clampExposure(-x);
            default -> 1.0f;
        };
    }

    private static float clampExposure(float value) {
        return Mth.clamp(0.35f + (0.65f * Math.max(0.0f, value)), 0.35f, 1.0f);
    }
}
