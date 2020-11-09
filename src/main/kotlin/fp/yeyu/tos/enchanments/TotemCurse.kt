package fp.yeyu.tos.enchanments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import fp.yeyu.tos.TotemOfShadowEntry
import fp.yeyu.tos.item.TotemOfShadowItem
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentLevelEntry
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.item.EnchantedBookItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

sealed class TotemCurse : Enchantment(Rarity.UNCOMMON, EnchantmentTarget.WEAPON, arrayOfNulls(0)) {
    override fun isAcceptableItem(stack: ItemStack): Boolean = stack.item == TotemOfShadowItem || stack.item == Items.ENCHANTED_BOOK

    override fun getMinPower(level: Int): Int = TotemCurse.getMinPower(level)

    override fun getMaxPower(level: Int): Int = TotemCurse.getMaxPower(level)

    override fun getMaxLevel(): Int = 4

    override fun isCursed(): Boolean = true

    abstract val identifierPath: String

    fun getEnchantedBook(): ItemStack = ItemStack(Items.ENCHANTED_BOOK).apply {
        EnchantedBookItem.addEnchantment(this, EnchantmentLevelEntry(this@TotemCurse, maxLevel))
    }

    companion object {
        fun getMinPower(level: Int): Int = 10 + (level - 1) * 8
        fun getMaxPower(level: Int): Int = getMinPower(level) + 8
        private val allEnchantments by lazy { listOf(TotemCurseOfFire,
                TotemCurseOfExplosion,
                TotemCurseOfThorns,
                TotemCurseOfResistance,
                TotemCurseOfSpeed
        ) }
        fun getEnchantments(power: Int): List<EnchantmentLevelEntry> {
            val l = mutableListOf<EnchantmentLevelEntry>()
            (1..4).filter { power >= getMinPower(it) && power <= getMaxPower(it) }.forEach {
                allEnchantments.forEach { e -> l.add(EnchantmentLevelEntry(e, it)) }
            }
            return l
        }

        fun getCurseEnchantment(identifier: Identifier): TotemCurse? {
            val enchantment = Registry.ENCHANTMENT[identifier] ?: return null
            if (enchantment !is TotemCurse) return null
            return enchantment
        }
    }
}

object TotemCurseOfFire : TotemCurse() {
    override val identifierPath: String = "totem_fire_curse"
}

object TotemCurseOfExplosion : TotemCurse() {
    override val identifierPath: String = "totem_explosion_curse"
}

object TotemCurseOfThorns : TotemCurse() {
    override val identifierPath: String = "totem_thorns_curse"
}

object TotemCurseOfResistance : TotemCurse() {
    override val identifierPath: String = "totem_resistance_curse"
}

object TotemCurseOfSpeed : TotemCurse() {
    override val identifierPath: String = "totem_speed_curse"
}


