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
import net.minecraft.entity.*
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Arm
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import java.util.*
import kotlin.random.asKotlinRandom

class SpiritEntity(entityType: EntityType<out MobEntity?>?, world: World?) : MobEntity(entityType, world) {
    private val posPlayer = ReversingFramefulEasePlayer(
            EaseInOutImpl(0.0,
                    Y_MODIFIER_FACTOR,
                    InverseQuadratic,
                    QuadraticFunction,
                    DoubleToDoubleInterpolator
            ),
            30)
    private var particleTick = 5
    private var trackingTick = 5
    private var actualPosition = Vec3d.ZERO
    private var configuredActualPosition = false

    private var tracking: Entity? = null
    private var showSelf = false
    private var showHead = false
    var closestPlayer: PlayerEntity? = null
    private val animationFrames = 35
    val selfAlphaLevelPlayer = PersistentFramefulEasePlayer(
            EaseInImpl(1.0, 0.0, QuadraticFunction, DoubleToDoubleInterpolator),
            animationFrames
    )

    val headAlphaLevelPlayer = PersistentFramefulEasePlayer(
            EaseInImpl(1.0, 0.1, QuadraticFunction, DoubleToDoubleInterpolator),
            animationFrames
    )

    private val movementPlayer = mutableMapOf<String, PersistentFramefulEasePlayer<Double>>()
    private var movementTick = 30
    private val movementTickDefault = 300
    private val movementDistance = 6.0
    private val movementSpeed = 200
    private var movedTick = 0
    private var isMoving = false

    override fun canImmediatelyDespawn(distanceSquared: Double): Boolean {
        return false
    }

    override fun tick() {
        super.tick()
        if (world.isClient) return
        particleTick = --particleTick % PARTICLE_TICK_DEF
        if (particleTick == 0) {
            particleTick = PARTICLE_TICK_DEF + random.nextInt(PARTICLE_TICK_DEF_OFFSET)
            playAmbientParticle()
        }
    }

    private fun updateTracking() {
        val trackNew = tracking.let { it == null || it.distanceTo(this) > MAX_DISTANCE_TO_TRACK_NEW }
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
        setNoGravity(true)
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
        world.profiler.pop()

        if (world !is ServerWorld) return tickClientMovement()

        if (isMoving) {
            world.profiler.push("spirit_movement")
            val x = movementPlayer["x"]?.next() ?: pos.x
            val y = (movementPlayer["y"]?.next() ?: pos.y) + posPlayer.next()
            val z = movementPlayer["z"]?.next() ?: pos.z
            updatePosition(x, y, z)
            updateActualPosition(x, y, z)
            isMoving = ++movedTick < movementSpeed
            if (movedTick % 5 == 0) {
                findNewPlayerToTrack()
            }

            val tracking = this.closestPlayer ?: return world.profiler.pop()

            world.profiler.swap("spirit_look_at")
            val packet = EntityLookAtS2CPacket.makePacket(this, tracking.pos.x, tracking.eyeY, tracking.pos.z)
            (world as ServerWorld).chunkManager.sendToNearbyPlayers(this, packet)
            world.profiler.pop()
        } else {
            movementTick = if (tracking == null) --movementTick % movementTickDefault else movementTick
            if (movementTick == 0) {
                world.profiler.push("spirit_new_movement_calculation")
                var x: Double = pos.x
                var y: Double = pos.y
                var z: Double = pos.z
                var found = false
                for (i in 0 until 5) {
                    x = pos.x + random.asKotlinRandom().nextDouble(-movementDistance, movementDistance)
                    y = getRandomY()
                    z = pos.z + random.asKotlinRandom().nextDouble(-movementDistance, movementDistance)
                    if (rayCastTo(x, y, z).type == HitResult.Type.MISS) {
                        found = true
                        break
                    }
                }

                world.profiler.pop()

                if (found) moveTo(x, y, z)
                else tickIdle()
            } else tickIdle()
        }
    }

    private fun tickClientMovement() {
        tickTransparency()
        if (!isMoving) {
            val x = trackedPosition.x
            val y = actualPosition.y + posPlayer.next()
            val z = trackedPosition.z
            updatePosition(x, y, z)
            updateActualPosition(x, actualPosition.y, z)
        } else {
            val x = movementPlayer["x"]?.next() ?: trackedPosition.x
            val y = (movementPlayer["y"]?.next() ?: trackedPosition.y) + posPlayer.next()
            val z = movementPlayer["z"]?.next() ?: trackedPosition.z
            updatePosition(x, y, z)
            updateActualPosition(x, y, z)
            isMoving = ++movedTick < movementSpeed
        }
    }

    fun moveTo(x: Double, y: Double, z: Double) {
        movementTick = movementTickDefault
        isMoving = true
        setMovementAnimation("x", pos.x, x)
        setMovementAnimation("y", pos.y, y)
        setMovementAnimation("z", pos.z, z)
        movedTick = 0
        if (world.isClient) return
        LOGGER.info("Moving to: $x $y $z")
        val headingToPacket = SpiritEntityHeadingToS2CPacket.makePacket(this, x, y, z)
        (world as ServerWorld).chunkManager.sendToNearbyPlayers(this, headingToPacket)
    }

    private fun setMovementAnimation(coordinate: String, from: Double, to: Double) {
        val player = movementPlayer[coordinate] ?: return run {
            movementPlayer[coordinate] = PersistentFramefulEasePlayer(
                    EaseInOutImpl(from, to, InverseQuadratic, QuadraticFunction, DoubleToDoubleInterpolator),
                    movementSpeed
            )
        }
        player.transitionTo = to
    }

    private fun rayCastTo(x: Double, y: Double, z: Double): BlockHitResult {
        return world.raycast(RaycastContext(pos, Vec3d(x, y, z), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, this))
    }

    private fun getRandomY(): Double {
        val mutableBlockPos = blockPos.mutableCopy()
        for (i in 0 until 6) {
            if (!world.getBlockState(mutableBlockPos.downMutable()).isAir || mutableBlockPos.y == 0) break
        }
        val minY = mutableBlockPos.y + 1
        mutableBlockPos.y = blockPos.y

        for (i in 0 until 6) {
            if (!world.getBlockState(mutableBlockPos.upMutable()).isAir || mutableBlockPos.y == 0) break
        }
        val maxY = mutableBlockPos.y - 1
        return if (minY == maxY) pos.y
        else random.asKotlinRandom().nextDouble(minY.toDouble(), maxY.toDouble())
    }

    private fun tickIdle() {
        world.profiler.push("spirit_hover")

        if (!configuredActualPosition) updateActualPosition(pos.x, pos.y, pos.z)

        val x = pos.x
        val y = actualPosition.y + posPlayer.next()
        val z = pos.z

        trackingTick = --trackingTick % TRACKING_TICK_DEF
        if (trackingTick == 0) updateTracking()

        updatePosition(x, y, z)
        updateActualPosition(x, actualPosition.y, z)
        findNewPlayerToTrack()

        val tracking = this.tracking ?: getNonPlayerEntity() ?: return world.profiler.pop()
        world.profiler.swap("spirit_look")
        val packet = EntityLookAtS2CPacket.makePacket(this, tracking.pos.x, tracking.eyeY, tracking.pos.z)
        (world as ServerWorld).chunkManager.sendToNearbyPlayers(this, packet)
        world.profiler.pop()
    }

    private fun getNonPlayerEntity(): Entity? = world.getEntitiesByClass(LivingEntity::class.java, Box(blockPos).expand(10.0)) { true }.getOrNull(0)

    private fun tickTransparency() {
        world.profiler.push("spirit_transparency")
        closestPlayer = world.getClosestPlayer(this, 6.0)
        val closestIsNull = closestPlayer == null
        if (closestIsNull != showSelf) {
            showSelf = closestIsNull
            selfAlphaLevelPlayer.transitionTo = if (showSelf) 1.0 else 0.0
        }
        val closestPlayerForHead = world.getClosestPlayer(this, 4.5)
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
        world.profiler.pop()
    }

    override fun lookAtEntity(targetEntity: Entity, maxYawChange: Float, maxPitchChange: Float) {
        super.lookAtEntity(targetEntity, maxYawChange, maxPitchChange)
        headYaw = yaw
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
            updateActualPosition(x, y, z)
        }
    }

    private fun updateActualPosition(x: Double, y: Double, z: Double) {
        actualPosition = Vec3d(x, y, z)
        configuredActualPosition = true
    }

    override fun writeCustomDataToTag(tag: CompoundTag) {
        super.writeCustomDataToTag(tag)
        val pos = if (!configuredActualPosition) pos else actualPosition
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
        private val DEF = DefaultedList.ofSize(1, ItemStack.EMPTY)
        private const val Y_MODIFIER_FACTOR = 0.5
        private const val MAX_DISTANCE_TO_TRACK_NEW = 11f
        private const val PARTICLE_TICK_DEF = 120
        private const val PARTICLE_TICK_DEF_OFFSET = 10
        private const val PARTICLE_LOWER_OFFSET = -1.5
        private const val PARTICLE_UPPER_OFFSET = 2.5
        private const val PARTICLE_OFFSET_RANGE = PARTICLE_UPPER_OFFSET - PARTICLE_LOWER_OFFSET
        private const val TRACKING_TICK_DEF = 15
    }

    init {
        setCanPickUpLoot(true)
    }

    private fun BlockPos.Mutable.downMutable(): BlockPos {
        this.y = this.y - 1
        return this
    }

    private fun BlockPos.Mutable.upMutable(): BlockPos {
        this.y = this.y + 1
        return this
    }
}

