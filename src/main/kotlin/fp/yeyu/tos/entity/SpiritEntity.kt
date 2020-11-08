package fp.yeyu.tos.entity

import fp.yeyu.tos.TotemOfShadowEntry
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
import net.minecraft.entity.ai.control.LookControl
import net.minecraft.entity.ai.control.MoveControl
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.ai.goal.LookAroundGoal
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.FlyingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Arm
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import java.util.*
import kotlin.random.asKotlinRandom

class SpiritEntity(entityType: EntityType<out FlyingEntity>?, world: World?) : FlyingEntity(entityType, world) {
    private val posPlayer = ReversingFramefulEasePlayer(
            EaseInOutImpl(-0.005,
                    0.005,
                    InverseQuadratic,
                    QuadraticFunction,
                    DoubleToDoubleInterpolator
            ),
            30)
    private var particleTick = 5

    private var showSelf = false
    private var showHead = false
    var closestPlayer: PlayerEntity? = null
    var lookingAtEntity: Entity? = null
    private val animationFrames = 35
    val selfAlphaLevelPlayer = PersistentFramefulEasePlayer(
            EaseInImpl(1.0, 0.0, QuadraticFunction, DoubleToDoubleInterpolator),
            animationFrames
    )

    val headAlphaLevelPlayer = PersistentFramefulEasePlayer(
            EaseInImpl(1.0, 0.1, QuadraticFunction, DoubleToDoubleInterpolator),
            animationFrames
    )

    override fun canImmediatelyDespawn(distanceSquared: Double): Boolean {
        return false
    }

    override fun initGoals() {
        super.initGoals()
        this.goalSelector.add(1, SpiritLookAtPlayerGoal(this))
        this.goalSelector.add(2, SpiritLookAroundGoal(this))
        this.goalSelector.add(3, SpiritFlyRandomlyGoal(this))
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

    private fun hasPlayerAround(): Boolean = closestPlayer != null

    private fun isLookingAtSomeEntity(): Boolean =
            closestPlayer.let { it != null && squaredDistanceTo(it) < 81 } || lookingAtEntity.let { it != null && squaredDistanceTo(it) < 81 }

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
            val list = world.getNonSpectatingEntities(ItemEntity::class.java, this.boundingBox.expand(2.0))
            for (itemEntity in list) {
                if (!itemEntity.removed && !itemEntity.stack.isEmpty && !itemEntity.cannotPickup() && canGather(itemEntity.stack)) {
                    loot(itemEntity)
                }
            }
        }
        world.profiler.pop()
        tickTransparency()
        tickIdle()
    }

    private fun rayCastTo(x: Double, y: Double, z: Double): BlockHitResult {
        return world.raycast(RaycastContext(pos, Vec3d(x, y, z), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, this))
    }

    private fun tickIdle() {
        world.profiler.push("spirit_hover")
        addVelocity(0.0, posPlayer.next(), 0.0)
        return world.profiler.pop()
    }

    private fun tickTransparency() {
        world.profiler.push("spirit_transparency")
        lookForClosestPlayer()
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

    override fun isFallFlying(): Boolean = false

    override fun tryEquip(equipment: ItemStack): Boolean = true

    override fun damage(source: DamageSource, amount: Float): Boolean {
        source.attacker?.damage(DamageSource.mob(this), amount)
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

    private fun lookForClosestPlayer() {
        closestPlayer = world.getClosestPlayer(this, 6.0)
    }

    private fun lookForAnyEntity() {
        lookingAtEntity = world.getNonSpectatingEntities(LivingEntity::class.java, Box(blockPos).expand(11.0)).getOrNull(1)
    }

    override fun getLookPitchSpeed(): Int = 360
    override fun getLookYawSpeed(): Int = 360

    override fun getAmbientSound(): SoundEvent = TotemOfShadowEntry.spiritEntityAmbientSound

    override fun getSoundPitch(): Float = 0.8f + random.nextFloat() * 0.4f

    companion object {
        private val DEF = DefaultedList.ofSize(1, ItemStack.EMPTY)
        private const val PARTICLE_TICK_DEF = 120
        private const val PARTICLE_TICK_DEF_OFFSET = 10
        private const val PARTICLE_LOWER_OFFSET = -1.5
        private const val PARTICLE_UPPER_OFFSET = 2.5
        private const val PARTICLE_OFFSET_RANGE = PARTICLE_UPPER_OFFSET - PARTICLE_LOWER_OFFSET

        fun createMobAttributes(): DefaultAttributeContainer.Builder {
            return createLivingAttributes()
                    .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5)
                    .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0)
                    .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK)
        }
    }

    init {
        this.moveControl = SpiritMoveControl(this)
        this.lookControl = SpiritLookControl(this)
        setCanPickUpLoot(true)
    }

    class SpiritFlyRandomlyGoal(private val spiritEntity: SpiritEntity) : Goal() {
        override fun canStart(): Boolean {
            if (spiritEntity.hasPlayerAround()) return false
            val moveControl = spiritEntity.getMoveControl() as SpiritMoveControl
            return moveControl.movementTick > 200 || moveControl.movementTick == 0
        }

        override fun shouldContinue(): Boolean = false

        override fun canStop(): Boolean = true

        override fun start() {
            (spiritEntity.moveControl as SpiritMoveControl).moveToRandomTarget()
        }

        init {
            controls = EnumSet.of(Control.MOVE)
        }
    }

    class SpiritLookAtPlayerGoal(private val spiritEntity: SpiritEntity) : Goal() {

        override fun canStart(): Boolean {
            spiritEntity.lookForClosestPlayer()
            return !spiritEntity.getMoveControl().isMoving && hasPlayerAround()
        }

        private fun hasPlayerAround(): Boolean = spiritEntity.hasPlayerAround()

        override fun tick() {
            val lookingAt = spiritEntity.closestPlayer ?: return
            spiritEntity.lookControl.lookAt(lookingAt, 360f, 360f)
        }
    }

    class SpiritLookAroundGoal(private val spiritEntity: SpiritEntity) : LookAroundGoal(spiritEntity) {
        override fun canStart(): Boolean = !spiritEntity.getMoveControl().isMoving && !spiritEntity.hasPlayerAround() && super.canStart() && spiritEntity.random.nextDouble() < 0.3
        override fun shouldContinue(): Boolean = if (spiritEntity.isLookingAtSomeEntity()) false else super.shouldContinue()
        override fun canStop(): Boolean = spiritEntity.hasPlayerAround()

        override fun start() {
            spiritEntity.lookForAnyEntity()
            super.start()
        }

        override fun tick() {
            if (!spiritEntity.isLookingAtSomeEntity() || spiritEntity.lookingAtEntity == null) return super.tick()
            spiritEntity.lookControl.lookAt(spiritEntity.lookingAtEntity, 360f, 360f)
            spiritEntity.headYaw = spiritEntity.yaw
        }
    }

    class SpiritMoveControl(private val spiritEntity: SpiritEntity) : MoveControl(spiritEntity) {

        var movementTick: Int = 0

        override fun tick() {
            movementTick++

            if (state == State.MOVE_TO || state == State.JUMPING) {
                state = State.MOVE_TO
                super.tick()
                return
            }
        }

        fun moveToRandomTarget() {
            val movementRadius = 6.0
            val random = spiritEntity.random.asKotlinRandom()
            val x = spiritEntity.pos.x + random.nextDouble(-movementRadius, movementRadius)
            val y = spiritEntity.pos.y + random.nextDouble(-movementRadius, movementRadius)
            val z = spiritEntity.pos.z + random.nextDouble(-movementRadius, movementRadius)
            if (spiritEntity.rayCastTo(x, y, z).type != HitResult.Type.MISS) return
            moveTo(x, y, z, 1.0)
        }

        override fun moveTo(x: Double, y: Double, z: Double, speed: Double) {
            super.moveTo(x, y, z, speed)
            movementTick = 0
        }
    }

    class SpiritLookControl(spiritEntity: SpiritEntity) : LookControl(spiritEntity) {
        override fun tick() {
            yawSpeed = 360f
            pitchSpeed = 360f
            if (active) {
                active = false
                entity.headYaw = changeAngle(entity.headYaw, this.targetYaw, 360f)
                entity.pitch = changeAngle(entity.pitch, this.targetPitch, 360f)
            }
        }
    }
}

