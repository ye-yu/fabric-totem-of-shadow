package fp.yeyu.tos

import fp.yeyu.tos.client.ShadowEntityRenderer
import fp.yeyu.tos.client.SpiritEntityRenderer
import fp.yeyu.tos.entity.SpiritEntity
import fp.yeyu.tos.entity.ShadowEntity
import fp.yeyu.tos.item.TotemOfShadowItem
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object TotemOfShadowEntry : ModInitializer, ClientModInitializer {

    const val NAMESPACE = "totemofshadow"
    private val totemOfShadowId = Identifier(NAMESPACE, "totem_of_shadow")
    private val shadowEntityId = Identifier(NAMESPACE, "shadow_entity")
    private val spiritEntityId = Identifier(NAMESPACE, "spirit_entity")
    private val logger: Logger = LogManager.getLogger()

    val spiritEntity by lazy(TotemOfShadowEntry::registerSpiritEntity)
    val shadowEntity by lazy(TotemOfShadowEntry::registerShadowEntity)
    private val totemOfShadow: Item = TotemOfShadowItem()

    override fun onInitialize() {
        Registry.register(Registry.ITEM, totemOfShadowId, totemOfShadow)
        logger.info("Registered ${spiritEntity.name}")
        logger.info("Registered ${shadowEntity.name}")
    }

    override fun onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(shadowEntity) { dispatcher: EntityRenderDispatcher?, _: Any? -> ShadowEntityRenderer(dispatcher) }
        EntityRendererRegistry.INSTANCE.register(spiritEntity) { dispatcher: EntityRenderDispatcher?, _: Any? -> SpiritEntityRenderer(dispatcher) }
    }

    private fun registerSpiritEntity(): EntityType<SpiritEntity> = Registry.register(
            Registry.ENTITY_TYPE,
            spiritEntityId,
            FabricEntityTypeBuilder
                    .create(SpawnGroup.MONSTER, ::SpiritEntity)
                    .dimensions(EntityDimensions.fixed(.45f, .45f)).build()
    ).apply {
        FabricDefaultAttributeRegistry.register(this, PathAwareEntity.createMobAttributes())
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