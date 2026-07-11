package dev.braggestbacon97.windynamics.integration.create.contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public record ContraptionSailScanResult(
        List<BlockPos> sailPositions,
        float totalSailPower,
        Vec3 sailCentroid,
        AABB sailBounds,
        Map<Integer, Tuple<Integer, Integer>> layers,
        int scannedBlockCount
) {
    public static final ContraptionSailScanResult EMPTY = new ContraptionSailScanResult(
            List.of(),
            0.0f,
            new Vec3(0.0, 0.0, 0.0),
            null,
            Map.of(),
            0
    );

    public boolean hasSails() {
        return totalSailPower > 0.0f && !sailPositions.isEmpty();
    }

    public int roundedSailBlocks() {
        return Math.max(0, Math.round(totalSailPower));
    }
}
