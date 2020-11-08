package fp.yeyu.tos.mixins;

import fp.yeyu.tos.TotemOfShadowEntry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ProtectionEnchantment.class)
public abstract class FireProtectionEnchantmentMixin extends Enchantment {
    @Shadow @Final public ProtectionEnchantment.Type protectionType;

    protected FireProtectionEnchantmentMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if (protectionType == ProtectionEnchantment.Type.FIRE)
            return stack.getItem() == TotemOfShadowEntry.INSTANCE.getTotemOfShadow() || super.isAcceptableItem(stack);
        return super.isAcceptableItem(stack);
    }
}
