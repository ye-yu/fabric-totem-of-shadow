package fp.yeyu.tos.mixins;

import fp.yeyu.tos.entity.SpiritEntity;
import fp.yeyu.tos.mixinutil.EntityPacketListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EntityMixin extends Entity implements EntityPacketListener {
    public EntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }
    @Shadow
    public abstract void setHeadYaw(float headYaw);

    @Shadow public float headYaw;


    private double persistentLookAtX = 0f;
    private double persistentLookAtY = 0f;
    private double persistentLookAtZ = 0f;
    private boolean persistentLookAt = false;

    @Override
    @Unique
    public void onLookAtPacket(double x, double y, double z) {
        persistentLookAt = true;
        persistentLookAtX = x;
        persistentLookAtY = y;
        persistentLookAtZ = z;
        double xDirection = persistentLookAtX - this.getX();
        double yDirection = persistentLookAtY - this.getEyeY();
        double zDirection = persistentLookAtZ - this.getZ();

        double h = MathHelper.sqrt(xDirection * xDirection + zDirection * zDirection);
        float i = (float) (MathHelper.atan2(zDirection, xDirection) * 57.2957763671875D) - 90.0F;
        float j = (float) (-(MathHelper.atan2(yDirection, h) * 57.2957763671875D));
        this.pitch = changeAngle(this.pitch, j);
        this.yaw = changeAngle(this.yaw, i);
        this.headYaw = this.yaw;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if (!world.isClient) return;
        if (!persistentLookAt) return;
        onLookAtPacket(persistentLookAtX, persistentLookAtY, persistentLookAtZ);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    @Unique
    public void onHeadingToPacket(double x, double y, double z) {
        if ((Object) this instanceof SpiritEntity) {
            final SpiritEntity spiritEntity = (SpiritEntity) (Object) this;
            onLookAtPacket(x, y, z);
            spiritEntity.moveTo(x, y, z);
        } else updatePosition(x, y, z);
    }

    private float changeAngle(float oldAngle, float newAngle) {
        float f = MathHelper.wrapDegrees(newAngle - oldAngle);
        if (f > 360f) {
            f = 360f;
        }

        if (f < -360f) {
            f = -360f;
        }

        return oldAngle + f;
    }

}
