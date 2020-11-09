package fp.yeyu.tos.enchanments

import fp.yeyu.tos.TotemOfShadowEntry.totemOfShadow
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentLevelEntry
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

sealed class TotemCurse : Enchantment(Rarity.UNCOMMON, EnchantmentTarget.WEAPON, arrayOfNulls(0)) {
    override fun isAcceptableItem(stack: ItemStack): Boolean = stack.item == totemOfShadow || stack.item == Items.BOOK

    override fun getMinPower(level: Int): Int = TotemCurse.getMinPower(level)

    override fun getMaxPower(level: Int): Int = TotemCurse.getMaxPower(level)

    override fun getMaxLevel(): Int = 4

    override fun isCursed(): Boolean = true

    abstract val identifierPath: String

    companion object {
        fun getMinPower(level: Int): Int = 10 + (level - 1) * 8
        fun getMaxPower(level: Int): Int = getMinPower(level) + 8
        private val allEnchantments by lazy { listOf(TotemCurseOfFire, TotemCurseOfExplosion, TotemCurseOfThorns, TotemCurseOfResistance) }
        fun getEnchantments(power: Int): List<EnchantmentLevelEntry> {
            val l = mutableListOf<EnchantmentLevelEntry>()
            (1..4).filter { power >= getMinPower(it) && power <= getMaxPower(it) }.forEach {
                allEnchantments.forEach{ e -> l.add(EnchantmentLevelEntry(e, it))}
            }
            return l
        }
    }
}

object TotemCurseOfFire: TotemCurse() {
    override val identifierPath: String = "totem_fire_curse"
}

object TotemCurseOfExplosion: TotemCurse() {
    override val identifierPath: String = "totem_explosion_curse"
}

object TotemCurseOfThorns: TotemCurse() {
    override val identifierPath: String = "totem_thorns_curse"
}

object TotemCurseOfResistance: TotemCurse() {
    override val identifierPath: String = "totem_resistance_curse"
}
