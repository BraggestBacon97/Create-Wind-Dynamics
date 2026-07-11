package dev.braggestbacon97.windynamics.world.block.entity;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import dev.braggestbacon97.windynamics.integration.create.CreateBridge;
import dev.braggestbacon97.windynamics.integration.create.contraption.ContraptionSailScanResult;
import dev.braggestbacon97.windynamics.integration.create.contraption.ContraptionSailScanner;
import dev.braggestbacon97.windynamics.registry.ModBlockEntities;
import dev.braggestbacon97.windynamics.world.WindManager;
import dev.braggestbacon97.windynamics.world.WindSample;
import dev.braggestbacon97.windynamics.world.block.WindSailHubBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class WindSailHubBlockEntity extends GeneratingKineticBlockEntity {
    private static final int RECOMPUTE_INTERVAL_TICKS = 10;
    private static final double CONTRAPTION_SEARCH_RADIUS = 8.0;

    private int manualSailBlocks = 16;
    private float estimatedSU = 0.0f;
    private int tickCounter = 0;
    private ContraptionSailScanResult lastContraptionScan = ContraptionSailScanResult.EMPTY;

    public WindSailHubBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_SAIL_HUB.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WindSailHubBlockEntity be) {
        be.tick();
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        tickCounter++;
        if (tickCounter % RECOMPUTE_INTERVAL_TICKS == 0) {
            refreshKineticOutput();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0.0f;
        }

        WindSample sample = WindManager.sample(serverLevel, worldPosition);
        Direction facing = getHubFacing();
        int sailBlocks = getEffectiveSailBlocks();

        return CreateBridge.computeGeneratedSpeed(sample, facing, sailBlocks);
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0.0f;
        }

        WindSample sample = WindManager.sample(serverLevel, worldPosition);
        Direction facing = getHubFacing();
        int sailBlocks = getEffectiveSailBlocks();
        float heightBonus = CreateBridge.computeHeightBonus(worldPosition.getY());

        return CreateBridge.computeStressCapacity(sample, facing, sailBlocks, heightBonus);
    }

    @Override
    public float calculateStressApplied() {
        return 0.0f;
    }

    public void incrementSails(int delta) {
        manualSailBlocks = Mth.clamp(manualSailBlocks + delta, 1, 256);
        refreshKineticOutput();
    }

    public int getSailBlocks() {
        return getEffectiveSailBlocks();
    }

    public float getEstimatedSU() {
        return estimatedSU;
    }

    public ContraptionSailScanResult getLastContraptionScan() {
        return lastContraptionScan;
    }

    private void refreshKineticOutput() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        lastContraptionScan = scanNearbyContraption(serverLevel);
        WindSample sample = WindManager.sample(serverLevel, worldPosition);
        Direction facing = getHubFacing();
        float heightBonus = CreateBridge.computeHeightBonus(worldPosition.getY());

        estimatedSU = lastContraptionScan.hasSails()
                ? CreateBridge.computeEstimatedSu(sample, facing, lastContraptionScan, heightBonus)
                : CreateBridge.computeEstimatedSu(sample, facing, manualSailBlocks, heightBonus);

        updateGeneratedRotation();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private int getEffectiveSailBlocks() {
        return lastContraptionScan.hasSails() ? lastContraptionScan.roundedSailBlocks() : manualSailBlocks;
    }

    @Nullable
    private Direction getHubFacing() {
        if (getBlockState().hasProperty(WindSailHubBlock.FACING)) {
            return getBlockState().getValue(WindSailHubBlock.FACING);
        }
        return null;
    }

    private ContraptionSailScanResult scanNearbyContraption(ServerLevel serverLevel) {
        ControlledContraptionEntity contraptionEntity = findNearbyContraption(serverLevel);
        if (contraptionEntity == null) {
            return ContraptionSailScanResult.EMPTY;
        }

        Direction facing = getHubFacing();
        if (facing == null) {
            return ContraptionSailScanResult.EMPTY;
        }

        return ContraptionSailScanner.scan(
                contraptionEntity,
                facing.getNormal(),
                this::resolveSailPower
        );
    }

    @Nullable
    private ControlledContraptionEntity findNearbyContraption(ServerLevel serverLevel) {
        AABB searchBox = new AABB(worldPosition).inflate(CONTRAPTION_SEARCH_RADIUS);
        List<ControlledContraptionEntity> candidates = serverLevel.getEntitiesOfClass(ControlledContraptionEntity.class, searchBox);
        return candidates.stream()
                .filter(entity -> entity.getContraption() != null)
                .min(Comparator.comparingDouble(this::distanceToHubSqr))
                .orElse(null);
    }

    private double distanceToHubSqr(ControlledContraptionEntity entity) {
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        return entity.distanceToSqr(cx, cy, cz);
    }

    private float resolveSailPower(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo info) {
        if (info == null || info.state() == null) {
            return 0.0f;
        }

        if (com.simibubi.create.AllTags.AllBlockTags.WINDMILL_SAILS.matches(info.state())) {
            return 1.0f;
        }

        return 0.0f;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ManualSailBlocks", manualSailBlocks);
        tag.putFloat("EstimatedSU", estimatedSU);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        manualSailBlocks = tag.getInt("ManualSailBlocks");
        estimatedSU = tag.getFloat("EstimatedSU");
        tickCounter = tag.getInt("TickCounter");
    }
}
