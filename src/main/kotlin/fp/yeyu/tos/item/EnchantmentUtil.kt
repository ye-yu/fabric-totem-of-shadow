package fp.yeyu.tos.item

import net.minecraft.enchantment.Enchantment

object EnchantmentUtil {

    fun MutableMap<Enchantment, Int>.updateEnchantmentEntry(entries: Map<Enchantment, Int>): Boolean {
        var updated = false
        for ((enchantment, level) in entries) {
            if (this.containsKey(enchantment)) {
                if (this[enchantment] == level) {
                    updated = true
                    this[enchantment] = level.coerceAtMost(enchantment.maxLevel)
                } else if (this[enchantment].let { it != null && it < level }) {
                    updated = true
                    this[enchantment] = level.coerceAtMost(enchantment.maxLevel)
                }
            } else {
                updated = true
                this[enchantment] = level
            }
        }
        return updated
    }
}