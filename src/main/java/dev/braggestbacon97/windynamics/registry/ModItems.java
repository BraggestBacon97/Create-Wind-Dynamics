package dev.braggestbacon97.windynamics.registry;

import dev.braggestbacon97.windynamics.WindDynamicsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, WindDynamicsMod.MOD_ID);

    public static final DeferredHolder<Item, BlockItem> WIND_SENSOR = ITEMS.register("wind_sensor",
            () -> new BlockItem(ModBlocks.WIND_SENSOR.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> WIND_SAIL_HUB = ITEMS.register("wind_sail_hub",
            () -> new BlockItem(ModBlocks.WIND_SAIL_HUB.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
