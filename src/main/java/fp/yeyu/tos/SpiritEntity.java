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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Arm;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class SpiritEntity extends MobEntity {

    public static final DefaultedList<ItemStack> DEF = DefaultedList.ofSize(1, ItemStack.EMPTY);
    public static final double Y_MODIFIER_FACTOR = 0.5;
    private static final float MAX_DISTANCE_TO_TRACK_NEW = 20;
    protected ReversingFramefulEasePlayer<Double> posPlayer = null;
    protected static final int PARTICLE_TICK_DEF = 120;
    protected static final int PARTICLE_TICK_DEF_OFFSET = 10;
    protected static final double PARTICLE_LOWER_OFFSET = -1.5;
    protected static final double PARTICLE_UPPER_OFFSET = 2.5;
    protected static final double PARTICLE_OFFSET_RANGE = PARTICLE_UPPER_OFFSET - PARTICLE_LOWER_OFFSET;
    protected static final int TRACKING_TICK_DEF = 15;
    protected int particleTick = 5;
    protected int trackingTick = 5;
    protected Vec3d actualPosition = null;
    protected PlayerEntity tracking = null;


    protected boolean showSelf = false;
    protected boolean showHead = false;
    protected PlayerEntity closestPlayer = null;
    protected final int ALPHA_FRAMES = 35;

    private final PersistentFramefulEasePlayer<Double> selfAlphaLevelPlayer = new PersistentFramefulEasePlayer<>(
            new EaseInImpl<>(1d, 0d, QuadraticFunction.INSTANCE, DoubleToDoubleInterpolator.INSTANCE),
            ALPHA_FRAMES
    );

    public PersistentFramefulEasePlayer<Double> getSelfAlphaLevelPlayer() {
        return selfAlphaLevelPlayer;
    }

    public PersistentFramefulEasePlayer<Double> getHeadAlphaLevelPlayer() {
        return headAlphaLevelPlayer;
    }

    private final PersistentFramefulEasePlayer<Double> headAlphaLevelPlayer = new PersistentFramefulEasePlayer<>(
            new EaseInImpl<>(1d, 0d, QuadraticFunction.INSTANCE, DoubleToDoubleInterpolator.INSTANCE),
            ALPHA_FRAMES
    );

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
        this.world.getProfiler().swap("spirit_transparency");
        tickTransparency();
        this.world.getProfiler().swap("spirit_hover");
        if (posPlayer == null) configureEasePlayer();
        final double x = getPos().x;
        final double y = posPlayer.next();
        final double z = getPos().z;


        trackingTick = --trackingTick % TRACKING_TICK_DEF;
        if (trackingTick == 0) updateTracking();
        updatePosition(x, y, z);
        this.world.getProfiler().pop();
        if (tracking == null) return;
        this.world.getProfiler().push("spirit_look");
        lookAtEntity(tracking, 360f, 360f);
        this.world.getProfiler().pop();
    }

    private void tickTransparency() {
        closestPlayer = world.getClosestPlayer(this, 9);
        if (closestPlayer != null ^ showSelf) {
            showSelf = closestPlayer != null;
            selfAlphaLevelPlayer.setTransitionTo(showSelf ? 1d : 0d);
        }

        final PlayerEntity closestPlayerForHead = world.getClosestPlayer(this, 5);
        final boolean shouldShowHead = Optional.ofNullable(closestPlayerForHead).map(e -> {
            final int selectedSlot = e.inventory.selectedSlot;
            return e.inventory.getStack(selectedSlot).getItem();
        }).filter(e -> e == Items.LANTERN || e == Items.SOUL_LANTERN).isPresent();

        if (shouldShowHead != showHead) {
            showHead = shouldShowHead;
            headAlphaLevelPlayer.setTransitionTo(showHead ? 0.65 : 0.1);
        }

        if (showHead && random.nextDouble() < 0.05) {
            headAlphaLevelPlayer.setTransitionTo(random.nextBoolean() ? 0.65 : 0.75d);
        }
    }

    @Override
    public void lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange) {
        super.lookAtEntity(targetEntity, maxYawChange, maxPitchChange);
        this.headYaw = yaw;
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
