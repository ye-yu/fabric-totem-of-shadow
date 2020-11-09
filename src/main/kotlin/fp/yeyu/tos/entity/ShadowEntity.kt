package fp.yeyu.tos.entity

import fp.yeyu.tos.TotemOfShadowEntry
import fp.yeyu.tos.enchanments.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.client.render.entity.PlayerModelPart
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.client.world.ClientWorld
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.ai.goal.EscapeDangerGoal
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Packet
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.text.LiteralText
import net.minecraft.util.Arm
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Box
import net.minecraft.world.GameRules
import net.minecraft.world.World
import net.minecraft.world.explosion.Explosion.DestructionType
import java.util.*

class ShadowEntity(shadowEntityEntityType: EntityType<out ShadowEntity>?, world: World?) : PathAwareEntity(shadowEntityEntityType, world) {
    private var main: ItemStack = ItemStack.EMPTY
    private val armor: DefaultedList<ItemStack> = DefaultedList.ofSize(4, ItemStack.EMPTY)
    private var offHand: ItemStack = ItemStack.EMPTY

    private var copyingClientEntry: PlayerListEntry? = null
    private var copyingUuid: UUID? = null
    var copyingEntity: PlayerEntity? = null

    private val badUuid = UUID(1, 0)
    private val updateMobCallingTickDefault = 60

    private var updateMobCallingTick = 5
    private var fireProtectionLevel = 0
    private var blastProtectionLevel = 0
    private var explosionCount: Int = 0
    private var thornsLevel: Int = 0
    private var resistanceMultiplier: Int = 0
    private var speedMultiplier: Int = 1

    companion object {
        private val TRACKED_COPYING_UUID = DataTracker.registerData(ShadowEntity::class.java, TrackedDataHandlerRegistry.OPTIONAL_UUID)
    }

    override fun initDataTracker() {
        super.initDataTracker()
        dataTracker.startTracking<Optional<UUID>>(TRACKED_COPYING_UUID, Optional.empty())
    }

    override fun initGoals() {
        super.initGoals()
        goalSelector.add(0, ShadowRunGoal(this))
    }

    override fun canImmediatelyDespawn(distanceSquared: Double): Boolean {
        return false
    }

    override fun getArmorItems(): Iterable<ItemStack> {
        return armor
    }

    override fun getEquippedStack(slot: EquipmentSlot): ItemStack {
        return when (slot) {
            EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.FEET -> armor[slot.ordinal - EquipmentSlot.FEET.ordinal]
            EquipmentSlot.OFFHAND -> offHand
            EquipmentSlot.MAINHAND -> main
        }
    }

    override fun equipStack(slot: EquipmentSlot, stack: ItemStack) {
        when (slot) {
            EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.FEET -> {
                armor[slot.ordinal - EquipmentSlot.FEET.ordinal] = stack
                run { offHand = stack }
            }
            EquipmentSlot.OFFHAND -> {
                offHand = stack
            }
            EquipmentSlot.MAINHAND -> main = stack
        }
    }

    override fun getMainArm(): Arm {
        return Arm.RIGHT
    }

    fun setCopyingUUID(uuid: UUID) {
        dataTracker.set<Optional<UUID>>(TRACKED_COPYING_UUID, Optional.of(uuid))
        copyingUuid = uuid
    }

    @get:Environment(EnvType.CLIENT)
    val playerListEntry: PlayerListEntry?
        get() {
            if (copyingClientEntry == null && TRACKED_COPYING_UUID != null) {
                val instance = MinecraftClient.getInstance() ?: return null
                val networkHandler = instance.networkHandler ?: return null
                val uuid = dataTracker.get(TRACKED_COPYING_UUID)
                copyingClientEntry = networkHandler.getPlayerListEntry(uuid.orElse(Optional.ofNullable(instance.player).map { obj: ClientPlayerEntity -> obj.uuid }.orElse(null)))
                        ?: return null
                copyingEntity = OtherClientPlayerEntity(world as ClientWorld, copyingClientEntry?.profile)
                this.customName = copyingEntity?.displayName
            }
            return copyingClientEntry
        }

    @get:Environment(EnvType.CLIENT)
    val skinTexture: Identifier
        get() {
            val playerListEntry = playerListEntry
            return if (playerListEntry?.hasSkinTexture() != true) DefaultSkinHelper.getTexture() else playerListEntry.skinTexture
        }

    @Environment(EnvType.CLIENT)
    fun isPartVisible(modelPart: PlayerModelPart?): Boolean = copyingEntity?.isPartVisible(modelPart) ?: false

    private fun createSpawnPacket(uuid: UUID): Packet<*> = ShadowEntityMobSpawnS2CPacket(this, uuid)

    fun sendSpawnPacket(uuid: UUID) {
        if (world.isClient) return
        (world as ServerWorld).server.playerManager.sendToAll(createSpawnPacket(uuid))
    }

    override fun tick() {
        super.tick()
        if (world !is ServerWorld) return
        if (copyingEntity == null) return
        updateMobCallingTick = --updateMobCallingTick % updateMobCallingTickDefault
        if (updateMobCallingTick != 0) return
        world.getEntitiesByClass(MobEntity::class.java,
                Box(blockPos).expand(40.0)
        ) { he: MobEntity ->
            he.target == copyingEntity
        }.forEach { he: MobEntity -> if (random.nextDouble() < 0.67) he.target = this }
    }

    override fun writeCustomDataToTag(tag: CompoundTag) {
        super.writeCustomDataToTag(tag)
        val uuid = copyingEntity?.uuid ?: copyingUuid ?: badUuid
        tag.putUuid("copyingUuid", uuid)
        tag.putString("copyingName", copyingEntity?.entityName ?: entityName)
        tag.putInt("fireProtectionLevel", fireProtectionLevel)
        tag.putInt("blastProtectionLevel", blastProtectionLevel)
        tag.putInt("explosionCount", explosionCount)
        tag.putInt("thornsLevel", thornsLevel)
        tag.putInt("resistanceMultiplier", resistanceMultiplier)
        tag.putInt("speedMultiplier", speedMultiplier)
    }

    override fun readCustomDataFromTag(tag: CompoundTag) {
        super.readCustomDataFromTag(tag)
        if (tag.contains("copyingUuid")) {
            val copyingUuid = tag.getUuid("copyingUuid")
            setCopyingUUID(copyingUuid)
            val entity = (world as ServerWorld).getEntity(copyingUuid)
            if (entity is PlayerEntity) copyingEntity = entity
            world.sendEntityStatus(this, 80.toByte())
        }
        if (tag.contains("copyingName")) {
            val copyingName = tag.getString("copyingName")
            setCustomName(copyingName)
        }

        if (tag.contains("fireProtectionLevel")) {
            fireProtectionLevel = tag.getInt("fireProtectionLevel")
        }

        if (tag.contains("blastProtectionLevel")) {
            blastProtectionLevel = tag.getInt("blastProtectionLevel")
        }

        if (tag.contains("explosionCount")) {
            explosionCount = tag.getInt("explosionCount")
        }

        if (tag.contains("thornsLevel")) {
            thornsLevel = tag.getInt("thornsLevel")
        }


        if (tag.contains("resistanceMultiplier")) {
            resistanceMultiplier = tag.getInt("resistanceMultiplier")
        }

        if (tag.contains("speedMultiplier")) {
            speedMultiplier = tag.getInt("speedMultiplier")
        }
    }

    private fun setCustomName(copyingName: String) {
        customName = LiteralText(copyingName)
    }

    @Environment(EnvType.CLIENT)
    override fun handleStatus(status: Byte) {
        when (status) {
            80.toByte() -> playerListEntry
            81.toByte() -> playSpawnParticles()
            else -> super.handleStatus(status)
        }
    }

    private val spread = arrayOfNulls<IntArray>(9)

    init {
        spread[0] = intArrayOf(-1, 0, -1)
        spread[1] = intArrayOf(-1, 0, 0)
        spread[2] = intArrayOf(-1, 0, 1)
        spread[3] = intArrayOf(0, 0, -1)
        spread[4] = intArrayOf(0, 0, 0)
        spread[5] = intArrayOf(0, 0, 1)
        spread[6] = intArrayOf(1, 0, -1)
        spread[7] = intArrayOf(1, 0, 0)
        spread[8] = intArrayOf(1, 0, 1)
    }

    fun playSpawnParticles() {
        if (world.isClient) {
            for (i in 0..19) {
                for (ints in spread) {
                    val add = blockPos.add(ints!![0], ints[1], ints[2])
                    world.addParticle(ParticleTypes.POOF,
                            add.x + random.nextDouble(),
                            add.y + random.nextGaussian() * 0.02,
                            add.z + random.nextDouble(),
                            random.nextGaussian() * 0.02, random.nextGaussian() * 0.35, random.nextGaussian() * 0.02
                    )
                    world.addParticle(ParticleTypes.POOF,
                            add.x + random.nextDouble(),
                            add.y + 1 + random.nextGaussian() * 0.02,
                            add.z + random.nextDouble(),
                            random.nextGaussian() * 0.02, random.nextGaussian() * 0.35, random.nextGaussian() * 0.02
                    )
                }
            }
        } else {
            world.sendEntityStatus(this, 81.toByte())
            world.playSoundFromEntity(
                    null,
                    this,
                    TotemOfShadowEntry.shadowEntitySpawnedSound,
                    SoundCategory.HOSTILE,
                    soundVolume,
                    soundPitch
            )
        }
    }

    fun setAttributeFromEnchantment(enchantment: Enchantment, level: Int) {
        when (enchantment) {
            TotemCurseOfFire -> fireProtectionLevel = level
            TotemCurseOfExplosion -> blastProtectionLevel = level
            TotemCurseOfThorns -> thornsLevel = level
            TotemCurseOfResistance -> resistanceMultiplier = level
            TotemCurseOfSpeed -> speedMultiplier = level
            else -> return
        }
    }

    override fun damage(source: DamageSource, amount: Float): Boolean {
        val correctedAmount = amount * (1 - 0.2f * resistanceMultiplier.coerceAtMost(4))
        if (!super.damage(source, correctedAmount)) return false
        damageByFire(source.attacker)
        damageByExplosion()
        damageByThorns(source.attacker, correctedAmount)
        return true
    }

    private fun damageByThorns(attacker: Entity?, amount: Float) {
        val attackerEntity = attacker ?: return
        val damageAmount = 0.8f * thornsLevel
        attackerEntity.damage(DamageSource.mob(this), amount * damageAmount)
    }

    private fun damageByExplosion() {
        if (world.isClient || blastProtectionLevel == 0) return
        val destructionType = if (world.gameRules.getBoolean(GameRules.DO_MOB_GRIEFING)) DestructionType.DESTROY else DestructionType.NONE
        val radius = blastProtectionLevel * 0.8f
        world.createExplosion(this, this.x, this.y, this.z, 3f * radius, destructionType)
        explosionCount++
        if (random.nextDouble() < 0.25 * explosionCount) {
            dead = true
            remove()
        }
    }

    private fun damageByFire(attacker: Entity?) {
        val attackerEntity = attacker ?: return
        attackerEntity.setOnFireFor(3 * fireProtectionLevel)
    }

    private class ShadowRunGoal(val shadowEntity: ShadowEntity) : EscapeDangerGoal(shadowEntity, 0.4) {
        override fun canStart(): Boolean {
            return findTarget()
        }

        override fun start() {
            shadowEntity.navigation.startMovingTo(targetX, targetY, targetZ, speed + 0.1 * shadowEntity.speedMultiplier)
            active = true
        }
    }

}
