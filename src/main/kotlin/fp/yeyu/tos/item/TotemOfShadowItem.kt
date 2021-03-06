package fp.yeyu.tos.item

import fp.yeyu.tos.TotemOfShadowEntry
import fp.yeyu.tos.entity.ShadowEntity
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.SpawnReason
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Rarity
import org.apache.logging.log4j.LogManager

object TotemOfShadowItem : Item(Settings()
        .rarity(Rarity.RARE)
        .group(ItemGroup.MISC)
        .maxDamage(4)
) {
    private val LOGGER = LogManager.getLogger()

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        if (context.world !is ServerWorld) return super.useOnBlock(context)
        val player = context.player ?: return super.useOnBlock(context)
        val shadowEntity: ShadowEntity = TotemOfShadowEntry.shadowEntity.spawn(
                context.world as ServerWorld,
                null,
                null,
                null,
                context.blockPos.add(0, 1, 0),
                SpawnReason.EVENT,
                false,
                false
        ) ?: run {
            LOGGER.error("Cannot spawn Shadow mob.")
            return super.useOnBlock(context)
        }

        context.stack.decrement(1)
        shadowEntity.sendSpawnPacket(player.uuid)
        shadowEntity.copyingEntity = player
        shadowEntity.playSpawnParticles()
        EnchantmentHelper.fromTag(context.stack.enchantments).forEach(shadowEntity::setAttributeFromEnchantment)
        return ActionResult.CONSUME
    }

    override fun getEnchantability(): Int = 5
    override fun isEnchantable(stack: ItemStack?): Boolean = true

    override fun canRepair(stack: ItemStack?, ingredient: ItemStack?): Boolean {
        return ingredient.let { it != null && it.item == this }
    }
}