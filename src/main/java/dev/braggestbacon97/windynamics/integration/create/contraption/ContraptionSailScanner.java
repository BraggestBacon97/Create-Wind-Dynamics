package dev.braggestbacon97.windynamics.integration.create.contraption;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Scans a real Create contraption and extracts the sail layout from the moved blocks.
 *
 * <p>This is the first real geometry layer for Wind Dynamics: the hub no longer counts
 * sails in the world, it reads the assembled contraption and derives a stable shape model
 * from the blocks Create is actually moving.</p>
 */
public final class ContraptionSailScanner {
    private ContraptionSailScanner() {
    }

    public static ContraptionSailScanResult scan(final ControlledContraptionEntity contraptionEntity,
                                                 final Vec3i rotationAxis,
                                                 final Function<StructureBlockInfo, Float> sailPowerResolver) {
        if (contraptionEntity == null || contraptionEntity.getContraption() == null) {
            return ContraptionSailScanResult.EMPTY;
        }
        return scan(contraptionEntity.getContraption().getBlocks(), rotationAxis, sailPowerResolver);
    }

    public static ContraptionSailScanResult scan(final Map<BlockPos, StructureBlockInfo> blocks,
                                                 final Vec3i rotationAxis,
                                                 final Function<StructureBlockInfo, Float> sailPowerResolver) {
        if (blocks == null || blocks.isEmpty()) {
            return ContraptionSailScanResult.EMPTY;
        }

        final Vec3i axis = rotationAxis == null ? Vec3i.ZERO : rotationAxis;
        final List<BlockPos> sailPositions = new ArrayList<>();
        final Map<Integer, Tuple<Integer, Integer>> layers = new HashMap<>();

        double weightedX = 0.0;
        double weightedY = 0.0;
        double weightedZ = 0.0;
        double totalWeight = 0.0;

        BlockPos min = null;
        BlockPos max = null;
        int scannedBlocks = 0;

        for (final Map.Entry<BlockPos, StructureBlockInfo> entry : blocks.entrySet()) {
            scannedBlocks++;
            final StructureBlockInfo info = entry.getValue();
            final float sailPower = sailPowerResolver.apply(info);
            if (sailPower <= 0.0f) {
                continue;
            }

            final BlockPos localPos = entry.getKey();
            sailPositions.add(localPos);

            final double centerX = localPos.getX() + 0.5;
            final double centerY = localPos.getY() + 0.5;
            final double centerZ = localPos.getZ() + 0.5;

            weightedX += centerX * sailPower;
            weightedY += centerY * sailPower;
            weightedZ += centerZ * sailPower;
            totalWeight += sailPower;

            if (min == null) {
                min = localPos;
                max = localPos;
            } else {
                min = new BlockPos(
                        Math.min(min.getX(), localPos.getX()),
                        Math.min(min.getY(), localPos.getY()),
                        Math.min(min.getZ(), localPos.getZ()));
                max = new BlockPos(
                        Math.max(max.getX(), localPos.getX()),
                        Math.max(max.getY(), localPos.getY()),
                        Math.max(max.getZ(), localPos.getZ()));
            }

            final int offset = axis.getX() * localPos.getX()
                    + axis.getY() * localPos.getY()
                    + axis.getZ() * localPos.getZ();

            final BlockPos projected = localPos.offset(axis.multiply(-offset));
            final int radius = projected.getX() * projected.getX()
                    + projected.getY() * projected.getY()
                    + projected.getZ() * projected.getZ();

            final Tuple<Integer, Integer> tuple = layers.get(offset);
            if (tuple == null) {
                layers.put(offset, new Tuple<>(radius, radius));
            } else {
                if (radius < tuple.getA()) {
                    tuple.setA(radius);
                }
                if (radius > tuple.getB()) {
                    tuple.setB(radius);
                }
            }
        }

        if (totalWeight <= 0.0) {
            return ContraptionSailScanResult.EMPTY;
        }

        final Vec3 centroid = new Vec3(weightedX / totalWeight, weightedY / totalWeight, weightedZ / totalWeight);
        final AABB bounds = new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);

        return new ContraptionSailScanResult(sailPositions, (float) totalWeight, centroid, bounds, layers, scannedBlocks);
    }
}
