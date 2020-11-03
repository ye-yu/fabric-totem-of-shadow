package fp.yeyu.tos.mixins;

import fp.yeyu.tos.mixinsutil.EntityYawPitchAccessible;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements EntityYawPitchAccessible {
    @Shadow public float bodyYaw;

    @Shadow public float headYaw;

    @Shadow public float prevBodyYaw;

    @Shadow public float prevHeadYaw;

    @Shadow protected double serverHeadYaw;

    @Shadow protected double serverYaw;

    @Shadow protected double serverPitch;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void onYawPitchUpdate(float yaw, float pitch) {
        this.prevBodyYaw = this.bodyYaw;
        this.prevHeadYaw = this.headYaw;
        this.bodyYaw = yaw;
        this.headYaw = yaw;
        this.serverHeadYaw = yaw;
        this.serverYaw = yaw;

        this.serverPitch = pitch;
        this.pitch = pitch;
        this.pitch = MathHelper.clamp(this.pitch, -90.0F, 90.0F);
        this.serverPitch = MathHelper.clamp(this.serverPitch, -90.0F, 90.0F);
    }
}
