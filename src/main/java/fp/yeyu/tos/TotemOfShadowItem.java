package fp.yeyu.tos;

import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Rarity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TotemOfShadowItem extends Item {

    private static final Logger LOGGER = LogManager.getLogger();

    public TotemOfShadowItem() {
        super(new Item.Settings()
                .rarity(Rarity.EPIC)
                .group(ItemGroup.MISC)
                .maxCount(1)
        );
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld() instanceof ServerWorld)) return super.useOnBlock(context);
        if (context.getPlayer() == null) return super.useOnBlock(context);
        final ShadowEntity shadowEntity = TOSEntry.SHADOW_ENTITY.spawn((ServerWorld) context.getWorld(), null, null, null, context.getBlockPos().add(0, 1, 0), SpawnReason.EVENT, false, false);

        if (shadowEntity == null) {
            LOGGER.error("Cannot spawn Shadow mob.");
            return super.useOnBlock(context);
        }
        context.getStack().decrement(1);
        shadowEntity.sendSpawnPacket(context.getPlayer().getUuid());
        shadowEntity.copyingEntity = context.getPlayer();
        if (!context.getWorld().isClient) shadowEntity.playSpawnParticles();
        return ActionResult.CONSUME;
    }
}
