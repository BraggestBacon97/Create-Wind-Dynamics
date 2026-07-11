package dev.braggestbacon97.windynamics.registry;

import dev.braggestbacon97.windynamics.WindDynamicsMod;
import dev.braggestbacon97.windynamics.world.block.WindSailHubBlock;
import dev.braggestbacon97.windynamics.world.block.WindSensorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, WindDynamicsMod.MOD_ID);

    public static final DeferredHolder<Block, WindSensorBlock> WIND_SENSOR = BLOCKS.register("wind_sensor",
            () -> new WindSensorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Block, WindSailHubBlock> WIND_SAIL_HUB = BLOCKS.register("wind_sail_hub",
            () -> new WindSailHubBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
