package fp.yeyu.tos.mixins;

import fp.yeyu.tos.enchanments.TotemCurse;
import fp.yeyu.tos.item.TotemOfShadowItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "getPossibleEntries", at = @At("HEAD"), cancellable = true)
    private static void onGetPossibleEntries(int power, ItemStack stack, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        if (stack.getItem() != TotemOfShadowItem.INSTANCE) return;
        cir.setReturnValue(TotemCurse.Companion.getEnchantments(power));
    }
}
