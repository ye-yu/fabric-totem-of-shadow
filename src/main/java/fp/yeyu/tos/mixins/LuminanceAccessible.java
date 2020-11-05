package fp.yeyu.tos.mixins;

import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractBlock.AbstractBlockState.class)
public interface LuminanceAccessible {
    @Accessor
    void setLuminance(int luminance);
}
