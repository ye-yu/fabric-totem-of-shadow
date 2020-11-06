package fp.yeyu.tos.mixins;

import fp.yeyu.tos.mixinutil.CoordinateManipulable;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockPos.Mutable.class)
public abstract class BlockPosMutableMixin extends BlockPos implements CoordinateManipulable {
    public BlockPosMutableMixin(int i, int j, int k) {
        super(i, j, k);
    }

    @Shadow public abstract BlockPos.Mutable set(int x, int y, int z);

    @Override
    public void downMutable() {
        this.set(getX(), getY() - 1, getZ());
    }

    @Override
    public void upMutable() {
        this.set(getX(), getY() + 1, getZ());
    }
}
