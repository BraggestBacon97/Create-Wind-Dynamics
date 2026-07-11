package dev.braggestbacon97.windynamics;

import dev.braggestbacon97.windynamics.integration.create.CreateBridge;
import dev.braggestbacon97.windynamics.registry.ModBlockEntities;
import dev.braggestbacon97.windynamics.registry.ModBlocks;
import dev.braggestbacon97.windynamics.registry.ModItems;
import dev.braggestbacon97.windynamics.world.WindManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(WindDynamicsMod.MOD_ID)
public class WindDynamicsMod {
    public static final String MOD_ID = "windynamics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public WindDynamicsMod(IEventBus modBus, ModContainer modContainer) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);

        CreateBridge.bootstrap();

        NeoForge.EVENT_BUS.addListener(WindManager::onServerLevelTick);
        LOGGER.info("Wind Dynamics loaded");
    }
}
