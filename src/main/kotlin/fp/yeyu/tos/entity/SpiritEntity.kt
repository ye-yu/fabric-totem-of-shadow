package fp.yeyu.tos.entity

import io.github.yeyu.easing.EaseInImpl
import io.github.yeyu.easing.EaseInOutImpl
import io.github.yeyu.easing.function.InverseQuadratic
import io.github.yeyu.easing.function.QuadraticFunction
import io.github.yeyu.easing.interpolator.DoubleToDoubleInterpolator
import io.github.yeyu.easing.player.PersistentFramefulEasePlayer
import io.github.yeyu.easing.player.ReversingFramefulEasePlayer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.Arm
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.World
import java.util.*

class SpiritEntity(entityType: EntityType<out MobEntity?>?, world: World?) : MobEntity(entityType, world) {
    private var posPlayer: ReversingFramefulEasePlayer<Double>? = null
    private var particleTick = 5
    private var trackingTick = 5
    private var actualPosition: Vec3d? = null
    private var tracking: PlayerEntity? = null
    private var showSelf = false
    private var showHead = false

    var closestPlayer: PlayerEntity? = null
    private val animationFrames = 35
    val selfAlphaLevelPlayer = PersistentFramefulEasePlayer(
            EaseInImpl(1.0, 0.0, QuadraticFunction, DoubleToDoubleInterpolator),
            animationFrames
    )

    val headAlphaLevelPlayer = PersistentFramefulEasePlayer(
            EaseInImpl(1.0, 0.0, QuadraticFunction, DoubleToDoubleInterpolator),
            animationFrames
    )

    override fun canImmediatelyDespawn(distanceSquared: Double): Boolean {
        return false
    }

    override fun tick() {
        super.tick()
        setNoGravity(true)
        if (world.isClient) return
        particleTick = --particleTick % PARTICLE_TICK_DEF
        if (particleTick == 0) {
            particleTick = PARTICLE_TICK_DEF + random.nextInt(PARTICLE_TICK_DEF_OFFSET)
            playAmbientParticle()
        }
    }

    private fun updateTracking() {
        val trackNew = tracking.let{ it == null || it.distanceTo(this) > MAX_DISTANCE_TO_TRACK_NEW }
        if (trackNew) findNewPlayerToTrack()
    }

    private fun findNewPlayerToTrack() {
        tracking = world.getClosestPlayer(this, MAX_DISTANCE_TO_TRACK_NEW.toDouble())
    }

    @Environment(EnvType.CLIENT)
    override fun handleStatus(status: Byte) {
        if (status.toInt() == 80) playAmbientParticle() else super.handleStatus(status)
    }

    private fun playAmbientParticle() {
        if (world.isClient) {
            val maxCount = random.nextInt(5)
            for (i in 0 until 1 + maxCount) {
                val x = blockPos.x + PARTICLE_LOWER_OFFSET + random.nextDouble() * PARTICLE_OFFSET_RANGE
                val y = blockPos.y + PARTICLE_LOWER_OFFSET + random.nextDouble() * PARTICLE_OFFSET_RANGE
                val z = blockPos.z + PARTICLE_LOWER_OFFSET + random.nextDouble() * PARTICLE_OFFSET_RANGE
                world.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z, 0.0, 0.02, 0.0)
            }
        } else {
            world.sendEntityStatus(this, 80.toByte())
        }
    }

    override fun tickMovement() {
        super.tickMovement()
        world.profiler.push("looting")
        if (!world.isClient && canPickUpLoot() && this.isAlive && !dead && world.gameRules.getBoolean(GameRules.DO_MOB_GRIEFING)) {
            val list = world.getNonSpectatingEntities(ItemEntity::class.java, this.boundingBox.expand(1.0, 0.0, 1.0))
            for (itemEntity in list) {
                if (!itemEntity.removed && !itemEntity.stack.isEmpty && !itemEntity.cannotPickup() && canGather(itemEntity.stack)) {
                    loot(itemEntity)
                }
            }
        }
        world.profiler.swap("spirit_transparency")
        tickTransparency()
        world.profiler.swap("spirit_hover")
        if (posPlayer == null) configureEasePlayer()
        val x = pos.x
        val y = posPlayer!!.next()
        val z = pos.z
        trackingTick = --trackingTick % TRACKING_TICK_DEF
        if (trackingTick == 0) updateTracking()
        updatePosition(x, y, z)
        world.profiler.pop()
        if (tracking == null) return
        world.profiler.push("spirit_look")
        lookAtEntity(tracking!!, 360f, 360f)
        world.profiler.pop()
    }

    private fun tickTransparency() {
        closestPlayer = world.getClosestPlayer(this, 9.0)
        val closestIsNull = closestPlayer == null
        if (closestIsNull != showSelf) {
            showSelf = closestIsNull
            selfAlphaLevelPlayer.transitionTo = if (showSelf) 1.0 else 0.0
        }
        val closestPlayerForHead = world.getClosestPlayer(this, 5.0)
        val shouldShowHead = Optional.ofNullable(closestPlayerForHead).map { e: PlayerEntity ->
            val selectedSlot = e.inventory.selectedSlot
            e.inventory.getStack(selectedSlot).item
        }.filter { e: Item -> e === Items.LANTERN || e === Items.SOUL_LANTERN }.isPresent
        if (shouldShowHead != showHead) {
            showHead = shouldShowHead
            headAlphaLevelPlayer.transitionTo = if (showHead) 0.65 else 0.1
        }
        if (showHead && random.nextDouble() < 0.05) {
            headAlphaLevelPlayer.transitionTo = if (random.nextBoolean()) 0.65 else 0.75
        }
    }

    override fun lookAtEntity(targetEntity: Entity, maxYawChange: Float, maxPitchChange: Float) {
        super.lookAtEntity(targetEntity, maxYawChange, maxPitchChange)
        headYaw = yaw
    }

    private fun configureEasePlayer() {
        posPlayer = ReversingFramefulEasePlayer(
                EaseInOutImpl(pos.y,
                        pos.y + Y_MODIFIER_FACTOR,
                        InverseQuadratic,
                        QuadraticFunction,
                        DoubleToDoubleInterpolator
                ),
                30)
        actualPosition = Vec3d(pos.x, pos.y, pos.z)
    }

    override fun isFallFlying(): Boolean = false

    override fun readCustomDataFromTag(tag: CompoundTag) {
        super.readCustomDataFromTag(tag)
        if (tag.contains("actualPosition")) {
            val actualPosition = tag.getCompound("actualPosition")
            val x = actualPosition.getDouble("x")
            val y = actualPosition.getDouble("y")
            val z = actualPosition.getDouble("z")
            setPos(x, y, z)
            posPlayer = null
        }
    }

    override fun writeCustomDataToTag(tag: CompoundTag) {
        super.writeCustomDataToTag(tag)
        val pos = if (actualPosition == null) pos else actualPosition!!
        val compoundTag = CompoundTag()
        compoundTag.putDouble("x", pos.x)
        compoundTag.putDouble("y", pos.y)
        compoundTag.putDouble("z", pos.z)
        tag.put("actualPosition", compoundTag)
    }

    override fun tryEquip(equipment: ItemStack): Boolean {
        return true
    }

    override fun damage(source: DamageSource, amount: Float): Boolean {
        val attacker = source.attacker ?: return super.damage(source, amount)
        attacker.damage(DamageSource.mob(this), amount)
        return super.damage(source, amount)
    }

    override fun getArmorItems(): Iterable<ItemStack> {
        return DEF
    }

    override fun getEquippedStack(slot: EquipmentSlot): ItemStack {
        return DEF[0]
    }

    override fun equipStack(slot: EquipmentSlot, stack: ItemStack) {}
    override fun getMainArm(): Arm {
        return Arm.RIGHT
    }

    companion object {
        val DEF = DefaultedList.ofSize(1, ItemStack.EMPTY)
        const val Y_MODIFIER_FACTOR = 0.5
        private const val MAX_DISTANCE_TO_TRACK_NEW = 20f
        protected const val PARTICLE_TICK_DEF = 120
        protected const val PARTICLE_TICK_DEF_OFFSET = 10
        protected const val PARTICLE_LOWER_OFFSET = -1.5
        protected const val PARTICLE_UPPER_OFFSET = 2.5
        protected const val PARTICLE_OFFSET_RANGE = PARTICLE_UPPER_OFFSET - PARTICLE_LOWER_OFFSET
        protected const val TRACKING_TICK_DEF = 15
    }

    init {
        setCanPickUpLoot(true)
    }
}