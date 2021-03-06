package fp.yeyu.tos

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import fp.yeyu.tos.client.ShadowEntityRenderer
import fp.yeyu.tos.client.SpiritEntityRenderer
import fp.yeyu.tos.enchanments.*
import fp.yeyu.tos.entity.EntityLookAtS2CPacket
import fp.yeyu.tos.entity.ShadowEntity
import fp.yeyu.tos.entity.SpiritEntity
import fp.yeyu.tos.entity.SpiritEntityHeadingToS2CPacket
import fp.yeyu.tos.item.TotemOfShadowItem
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.impl.networking.ClientSidePacketRegistryImpl
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.command.argument.ArgumentTypes
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.command.argument.ItemStackArgumentType
import net.minecraft.command.argument.MessageArgumentType
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.SpawnEggItem
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object TotemOfShadowEntry : ModInitializer, ClientModInitializer {

    const val NAMESPACE = "totemofshadow"
    private val totemOfShadowId = Identifier(NAMESPACE, "totem_of_shadow")
    private val spiritEssenceId = Identifier(NAMESPACE, "spirit_essence")
    private val spiritEssenceTrioId = Identifier(NAMESPACE, "spirit_essence_trio")
    private val shadowEntityId = Identifier(NAMESPACE, "shadow_entity")
    private val spiritEntityId = Identifier(NAMESPACE, "spirit_entity")
    private val spiritEntitySpawnEggId = Identifier(NAMESPACE, "spirit_entity_spawn_egg")
    private val logger: Logger = LogManager.getLogger()

    val spiritEntity by lazy(TotemOfShadowEntry::registerSpiritEntity)
    val shadowEntity by lazy(TotemOfShadowEntry::registerShadowEntity)
    private val spiritEntitySpawnEgg: Item by lazy { SpawnEggItem(spiritEntity, 0x014333d, 0x0bbf2b8, Item.Settings().group(ItemGroup.MISC)) }
    internal val spiritEssence: Item = Item(Item.Settings().fireproof().maxCount(16).rarity(Rarity.UNCOMMON))
    internal val spiritEssenceTrio: Item = Item(Item.Settings().fireproof().maxCount(16).rarity(Rarity.UNCOMMON))

    // sound events
    private val spiritEntityAmbientId = Identifier(NAMESPACE, "spirit_entity_ambient")
    internal val spiritEntityAmbientSound = SoundEvent(spiritEntityAmbientId)
    private val shadowEntitySpawnedId = Identifier(NAMESPACE, "shadow_entity_spawned")
    internal val shadowEntitySpawnedSound = SoundEvent(shadowEntitySpawnedId)

    // enchantments
    private val totemCurseOfFireId = Identifier(NAMESPACE, TotemCurseOfFire.identifierPath)
    private val totemCurseOfExplosionId = Identifier(NAMESPACE, TotemCurseOfExplosion.identifierPath)
    private val totemCurseOfThornsId = Identifier(NAMESPACE, TotemCurseOfThorns.identifierPath)
    private val totemCurseOfResistanceId = Identifier(NAMESPACE, TotemCurseOfResistance.identifierPath)
    private val totemCurseOfSpeedId = Identifier(NAMESPACE, TotemCurseOfSpeed.identifierPath)

    override fun onInitialize() {
        Registry.register(Registry.ITEM, totemOfShadowId, TotemOfShadowItem)
        Registry.register(Registry.ITEM, spiritEssenceId, spiritEssence)
        Registry.register(Registry.ITEM, spiritEssenceTrioId, spiritEssenceTrio)
        logger.info("Registered items")

        logger.info("Registered ${spiritEntity.name}")
        Registry.register(Registry.ITEM, spiritEntitySpawnEggId, spiritEntitySpawnEgg)

        logger.info("Registered ${shadowEntity.name}")

        PotionUtil.addHealthBoostPotionAndRecipe()
        logger.info("Registered potion recipes")

        Registry.register(Registry.SOUND_EVENT, spiritEntityAmbientId, spiritEntityAmbientSound)
        Registry.register(Registry.SOUND_EVENT, shadowEntitySpawnedId, shadowEntitySpawnedSound)
        logger.info("Registered sound events")

        Registry.register(Registry.ENCHANTMENT, totemCurseOfFireId, TotemCurseOfFire)
        Registry.register(Registry.ENCHANTMENT, totemCurseOfExplosionId, TotemCurseOfExplosion)
        Registry.register(Registry.ENCHANTMENT, totemCurseOfThornsId, TotemCurseOfThorns)
        Registry.register(Registry.ENCHANTMENT, totemCurseOfResistanceId, TotemCurseOfResistance)
        Registry.register(Registry.ENCHANTMENT, totemCurseOfSpeedId, TotemCurseOfSpeed)
        logger.info("Registered enchantments")

        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<ServerCommandSource>, _ ->
            dispatcher.register(CommandManager.literal("cursetotem").then(
                    CommandManager.argument("curse", IdentifierArgumentType.identifier()).executes { context: CommandContext<ServerCommandSource> ->
                        val identifier = IdentifierArgumentType.getIdentifier(context, "curse")
                        val totemCurse = TotemCurse.getCurseEnchantment(identifier) ?: return@executes 0
                        context.source.player.giveItemStack(totemCurse.getEnchantedBook())
                        1
                    }
            ))
        }
    }

    override fun onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(shadowEntity) { dispatcher: EntityRenderDispatcher?, _: Any? -> ShadowEntityRenderer(dispatcher) }
        EntityRendererRegistry.INSTANCE.register(spiritEntity) { dispatcher: EntityRenderDispatcher?, _: Any? -> SpiritEntityRenderer(dispatcher) }
        ClientSidePacketRegistryImpl.INSTANCE.register(EntityLookAtS2CPacket.identifier, EntityLookAtS2CPacket::accept)
        ClientSidePacketRegistryImpl.INSTANCE.register(SpiritEntityHeadingToS2CPacket.identifier, SpiritEntityHeadingToS2CPacket::accept)
    }

    private fun registerSpiritEntity(): EntityType<SpiritEntity> = Registry.register(
            Registry.ENTITY_TYPE,
            spiritEntityId,
            FabricEntityTypeBuilder
                    .create(SpawnGroup.MONSTER, ::SpiritEntity)
                    .dimensions(EntityDimensions.fixed(.45f, .45f)).build()
    ).apply {
        FabricDefaultAttributeRegistry.register(this, SpiritEntity.createMobAttributes())
    }

    private fun registerShadowEntity(): EntityType<ShadowEntity> = Registry.register(
            Registry.ENTITY_TYPE,
            shadowEntityId,
            FabricEntityTypeBuilder
                    .create(SpawnGroup.MONSTER, ::ShadowEntity)
                    .dimensions(EntityDimensions.fixed(.6f, 1.8f)).build()
    ).apply {
        FabricDefaultAttributeRegistry.register(this, PathAwareEntity.createMobAttributes())
    }
}