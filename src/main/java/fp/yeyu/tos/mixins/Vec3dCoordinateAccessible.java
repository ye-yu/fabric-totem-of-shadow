package fp.yeyu.tos.mixins;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Vec3d.class)
public interface Vec3dCoordinateAccessible {

    @Accessor(value = "x") @Final @Mutable
    void setX(double x);

    @Accessor(value = "y") @Final @Mutable
    void setY(double y);

    @Accessor(value = "z") @Final @Mutable
    void setZ(double z);
}
