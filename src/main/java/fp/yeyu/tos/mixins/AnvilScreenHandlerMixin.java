package fp.yeyu.tos.mixins;

import fp.yeyu.tos.item.EnchantmentUtil;
import fp.yeyu.tos.item.TotemOfShadowItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    @Shadow
    @Final
    private Property levelCost;

    public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        final ItemStack firstSlot = input.getStack(0);
        final ItemStack secondSlot = input.getStack(1);
        if (firstSlot.isEmpty() || secondSlot.isEmpty()) return;
        if (firstSlot.getItem() != TotemOfShadowItem.INSTANCE) return;
        if (secondSlot.getItem() != TotemOfShadowItem.INSTANCE) return;

        final Map<Enchantment, Integer> firstSlotEnchantment = EnchantmentHelper.fromTag(firstSlot.getEnchantments());
        final Map<Enchantment, Integer> secondSlotEnchantment = EnchantmentHelper.fromTag(secondSlot.getEnchantments());

        if (!EnchantmentUtil.INSTANCE.updateEnchantmentEntry(firstSlotEnchantment, secondSlotEnchantment)) return;

        final ItemStack copy = firstSlot.copy();
        final CompoundTag itemTag = copy.getTag() == null ? new CompoundTag() : copy.getTag();
        if (itemTag.contains("Enchantments")) itemTag.remove("Enchantments");
        copy.setTag(itemTag);
        firstSlotEnchantment.forEach((copy::addEnchantment));
        int repairCost = firstSlot.getRepairCost() + (secondSlot.isEmpty() ? 0 : secondSlot.getRepairCost());
        output.setStack(0, copy);
        levelCost.set(repairCost == 0 ? 1 : repairCost);
        ci.cancel();
    }
}
