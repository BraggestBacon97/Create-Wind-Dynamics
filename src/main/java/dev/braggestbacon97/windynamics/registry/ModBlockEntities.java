package dev.braggestbacon97.windynamics.registry;

import dev.braggestbacon97.windynamics.WindDynamicsMod;
import dev.braggestbacon97.windynamics.world.block.entity.WindSailHubBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WindDynamicsMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindSailHubBlockEntity>> WIND_SAIL_HUB =
            BLOCK_ENTITIES.register("wind_sail_hub",
                    () -> BlockEntityType.Builder.of(WindSailHubBlockEntity::new, ModBlocks.WIND_SAIL_HUB.get())
                            .build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
