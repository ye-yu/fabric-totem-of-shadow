package fp.yeyu.tos;

import io.github.yeyu.easing.EaseInImpl;
import io.github.yeyu.easing.EaseInOutImpl;
import io.github.yeyu.easing.function.InverseQuadratic;
import io.github.yeyu.easing.function.QuadraticFunction;
import io.github.yeyu.easing.interpolator.DoubleToDoubleInterpolator;
import io.github.yeyu.easing.player.PersistentFramefulEasePlayer;
import io.github.yeyu.easing.player.ReversingFramefulEasePlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Arm;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.List;

public class SpiritEntity extends MobEntity {

    public static final DefaultedList<ItemStack> DEF = DefaultedList.ofSize(1, ItemStack.EMPTY);
    public static final double Y_MODIFIER_FACTOR = 0.5;
    private static final float MAX_DISTANCE_TO_TRACK_NEW = 20;
    protected ReversingFramefulEasePlayer<Double> posPlayer = null;
    protected PersistentFramefulEasePlayer<Double> pitchPlayer = null;
    protected PersistentFramefulEasePlayer<Double> yawPlayer = null;
    protected static final int PARTICLE_TICK_DEF = 50;
    protected static final int PARTICLE_TICK_DEF_OFFSET = 10;
    protected static final double PARTICLE_LOWER_OFFSET = -0.5;
    protected static final double PARTICLE_UPPER_OFFSET = 1.5;
    protected static final double PARTICLE_OFFSET_RANGE = PARTICLE_UPPER_OFFSET - PARTICLE_LOWER_OFFSET;
    protected static final int TRACKING_TICK_DEF = 15;
    protected int particleTick = 5;
    protected int trackingTick = 5;
    protected Vec3d actualPosition = null;
    protected PlayerEntity tracking = null;


    protected SpiritEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
        setCanPickUpLoot(true);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (world.isClient) return;
        particleTick = --particleTick % PARTICLE_TICK_DEF;
        if (particleTick == 0) {
            particleTick = PARTICLE_TICK_DEF + random.nextInt(PARTICLE_TICK_DEF_OFFSET);
            playAmbientParticle();
        }
    }

    private void updateTracking() {
        if (tracking == null || tracking.distanceTo(this) > MAX_DISTANCE_TO_TRACK_NEW) findNewPlayerToTrack();
    }

    private void findNewPlayerToTrack() {
        tracking = world.getClosestPlayer(this, MAX_DISTANCE_TO_TRACK_NEW);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handleStatus(byte status) {
        if (status == 80) playAmbientParticle();
        else super.handleStatus(status);
    }

    private void playAmbientParticle() {
        if (world.isClient) {
            final int maxCount = random.nextInt(5);
            for (int i = 0; i < 1 + maxCount; ++i) {
                double x = getBlockPos().getX() + PARTICLE_LOWER_OFFSET + random.nextDouble() * PARTICLE_OFFSET_RANGE;
                double y = getBlockPos().getY() + PARTICLE_LOWER_OFFSET + random.nextDouble() * PARTICLE_OFFSET_RANGE;
                double z = getBlockPos().getZ() + PARTICLE_LOWER_OFFSET + random.nextDouble() * PARTICLE_OFFSET_RANGE;
                world.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z, 0, 0.02, 0);
            }
        } else {
            world.sendEntityStatus(this, (byte) 80);
        }
    }

    @Override
    public void tickMovement() {
        this.world.getProfiler().push("looting");
        if (!this.world.isClient && this.canPickUpLoot() && this.isAlive() && !this.dead && this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            List<ItemEntity> list = this.world.getNonSpectatingEntities(ItemEntity.class, this.getBoundingBox().expand(1.0D, 0.0D, 1.0D));

            for (ItemEntity itemEntity : list) {
                if (!itemEntity.removed && !itemEntity.getStack().isEmpty() && !itemEntity.cannotPickup() && this.canGather(itemEntity.getStack())) {
                    this.loot(itemEntity);
                }
            }
        }
        this.world.getProfiler().pop();
        if (posPlayer == null) configureEasePlayer();
        final double x = getPos().x;
        final double y = posPlayer.next();
        final double z = getPos().z;


        trackingTick = --trackingTick % TRACKING_TICK_DEF;
        if (trackingTick == 0 || yawPlayer == null || pitchPlayer == null) {
            updateTracking();
        }
        updatePosition(x, y, z);
        lookAtEntity(tracking, 360f, 360f);
    }

    @Override
    public void lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange) {
        double dirX = targetEntity.getX() - this.getX();
        double dirZ = targetEntity.getZ() - this.getZ();
        double multiplier;
        if (targetEntity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)targetEntity;
            multiplier = livingEntity.getEyeY() - this.getEyeY();
        } else {
            multiplier = (targetEntity.getBoundingBox().minY + targetEntity.getBoundingBox().maxY) / 2.0D - this.getEyeY();
        }

        double h = MathHelper.sqrt(dirX * dirX + dirZ * dirZ);
        float yaw = (float)(MathHelper.atan2(dirZ, dirX) * 57.2957763671875D) - 90.0F;
        float pitch = (float)(-(MathHelper.atan2(multiplier, h) * 57.2957763671875D));
        onYawPitchUpdate(yaw, pitch);
        this.headYaw = yaw;
//        this.pitch = this.changeAngle(this.pitch, pitch, maxPitchChange);
//        this.yaw = this.changeAngle(this.yaw, yaw, maxYawChange);
    }


    public void onYawPitchUpdate(float yaw, float pitch) {
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        yaw = MathHelper.clamp(yaw, 0F, 360.0F);
        if (yawPlayer != null) yawPlayer.setTransitionTo((double) yaw);
        else this.yaw = yaw;

        if (pitchPlayer != null) pitchPlayer.setTransitionTo((double) pitch);
        else this.pitch = pitch;
//        this.prevBodyYaw = this.bodyYaw;
//        this.prevHeadYaw = this.headYaw;
//        this.bodyYaw = yaw;
//        this.headYaw = yaw;
//        this.serverHeadYaw = yaw;
//        this.serverYaw = yaw;
//        this.yaw = yaw;
//
//        this.serverPitch = pitch;
//        this.serverPitch = MathHelper.clamp(this.serverPitch, -90.0F, 90.0F);
//        this.pitch = pitch;
//        this.pitch = MathHelper.clamp(this.pitch, -90.0F, 90.0F);
    }

    private void configureEasePlayer() {
        posPlayer = new ReversingFramefulEasePlayer<>(
                new EaseInOutImpl<>(getPos().y,
                        getPos().y + Y_MODIFIER_FACTOR,
                        InverseQuadratic.INSTANCE,
                        QuadraticFunction.INSTANCE,
                        DoubleToDoubleInterpolator.INSTANCE),
                20 * 2);
        actualPosition = new Vec3d(getPos().x, getPos().y, getPos().z);
    }

    @Override
    public boolean isFallFlying() {
        return false;
    }

    @Override
    public void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        if (tag.contains("actualPosition")) {
            final CompoundTag actualPosition = tag.getCompound("actualPosition");
            final double x = actualPosition.getDouble("x");
            final double y = actualPosition.getDouble("y");
            final double z = actualPosition.getDouble("z");
            setPos(x, y, z);
            posPlayer = null;
        }
    }

    @Override
    public void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag(tag);
        final Vec3d pos = actualPosition == null ? getPos() : actualPosition;
        final CompoundTag compoundTag = new CompoundTag();
        compoundTag.putDouble("x", pos.x);
        compoundTag.putDouble("y", pos.y);
        compoundTag.putDouble("z", pos.z);
        tag.put("actualPosition", compoundTag);
    }

    @Override
    public boolean tryEquip(ItemStack equipment) {
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        final Entity attacker = source.getAttacker();
        if (attacker == null) return super.damage(source, amount);
        attacker.damage(DamageSource.mob(this), amount);
        return super.damage(source, amount);
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return DEF;
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return DEF.get(0);
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }
}
