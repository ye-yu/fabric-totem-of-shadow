package fp.yeyu.tos;

import me.lambdaurora.lambdynlights.api.DynamicLightHandler;
import me.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import me.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import me.lambdaurora.lambdynlights.api.item.ItemLightSource;
import me.lambdaurora.lambdynlights.api.item.ItemLightSources;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DynamicLightingCompat implements DynamicLightsInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Method called when LambDynamicLights is initialized to register custom dynamic light handlers and item light sources.
     *
     * @see DynamicLightHandlers#registerDynamicLightHandler(EntityType, DynamicLightHandler)
     * @see DynamicLightHandlers#registerDynamicLightHandler(BlockEntityType, DynamicLightHandler)
     * @see ItemLightSources#registerItemLightSource(ItemLightSource)
     */
    @Override
    public void onInitializeDynamicLights() {
        DynamicLightHandlers.registerDynamicLightHandler(TotemOfShadowEntry.INSTANCE.getSpiritEntity(), DynamicLightHandler.makeHandler(
                spiritEntity -> 12,
                spiritEntity -> false
        ));

        LOGGER.info("Registered Spirit Entity as a dynamic light source.");
    }
}
