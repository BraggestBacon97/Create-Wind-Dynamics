package dev.braggestbacon97.windynamics.integration.create.contraption;

import dev.braggestbacon97.windynamics.world.WindSample;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Converts wind + scanned contraption geometry into Create-facing kinetic values.
 */
public final class SailSuCalculator {
    private SailSuCalculator() {
    }

    public static float computeGeneratedSpeed(final WindSample sample,
                                              @Nullable final Direction facing,
                                              final ContraptionSailScanResult scan,
                                              final float heightBonus) {
        if (sample.isCalm() || scan == null || !scan.hasSails()) {
            return 0.0f;
        }

        final float exposure = computeExposure(sample, facing);
        final float weatherMultiplier = switch (sample.weather()) {
            case CLEAR -> 1.0f;
            case RAIN -> 1.08f;
            case THUNDER -> 1.2f;
        };

        final float turbulencePenalty = 1.0f - Mth.clamp(sample.turbulence(), 0.0f, 1.0f) * 0.35f;
        final float sailFactor = Mth.clamp(scan.totalSailPower() / 16.0f, 0.25f, 32.0f);
        final float balanceFactor = computeBalanceFactor(scan);

        final float speed = sample.speed() * 16.0f * exposure * weatherMultiplier * turbulencePenalty * sailFactor * balanceFactor * heightBonus;
        return Mth.clamp(speed, 0.0f, 256.0f);
    }

    public static float computeStressCapacity(final WindSample sample,
                                              @Nullable final Direction facing,
                                              final ContraptionSailScanResult scan,
                                              final float heightBonus) {
        if (sample.isCalm() || scan == null || !scan.hasSails()) {
            return 0.0f;
        }

        final float exposure = computeExposure(sample, facing);
        final float weatherMultiplier = switch (sample.weather()) {
            case CLEAR -> 1.0f;
            case RAIN -> 1.12f;
            case THUNDER -> 1.25f;
        };

        final float pressureMultiplier = Mth.clamp(1.0f + (1.0f - sample.pressure()) * 0.15f, 0.85f, 1.25f);
        final float turbulencePenalty = 1.0f - Mth.clamp(sample.turbulence(), 0.0f, 1.0f) * 0.25f;
        final float sailFactor = Mth.clamp(scan.totalSailPower() / 16.0f, 0.25f, 32.0f);
        final float balanceFactor = computeBalanceFactor(scan);

        final float base = 8.0f * sailFactor * exposure * weatherMultiplier * pressureMultiplier * turbulencePenalty * heightBonus * balanceFactor;
        return Mth.clamp(base, 0.0f, 1024.0f);
    }

    public static float computeEstimatedSu(final WindSample sample,
                                           @Nullable final Direction facing,
                                           final ContraptionSailScanResult scan,
                                           final float heightBonus) {
        final float generatedSpeed = computeGeneratedSpeed(sample, facing, scan, heightBonus);
        final float capacityPerSpeed = computeStressCapacity(sample, facing, scan, heightBonus);
        return generatedSpeed * capacityPerSpeed;
    }

    public static float computeHeightBonus(final int y) {
        return Mth.clamp(1.0f + (y - 64) / 256.0f, 0.75f, 1.5f);
    }

    private static float computeExposure(final WindSample sample, @Nullable final Direction facing) {
        if (facing == null) {
            return 1.0f;
        }

        final float x = sample.direction().x;
        final float z = sample.direction().y;

        return switch (facing) {
            case NORTH -> clampExposure(-z);
            case SOUTH -> clampExposure(z);
            case EAST -> clampExposure(x);
            case WEST -> clampExposure(-x);
            default -> 1.0f;
        };
    }

    private static float computeBalanceFactor(final ContraptionSailScanResult scan) {
        if (scan.sailBounds() == null || scan.sailPositions().isEmpty()) {
            return 1.0f;
        }

        final double width = Math.max(1.0, scan.sailBounds().getXsize());
        final double height = Math.max(1.0, scan.sailBounds().getYsize());
        final double depth = Math.max(1.0, scan.sailBounds().getZsize());
        final double volume = width * height * depth;
        final double density = scan.totalSailPower() / volume;

        return Mth.clamp((float) (0.75 + density * 0.45), 0.75f, 1.25f);
    }

    private static float clampExposure(final float value) {
        return Mth.clamp(0.35f + (0.65f * Math.max(0.0f, value)), 0.35f, 1.0f);
    }
}
