package fp.yeyu.tos;

import fp.yeyu.tos.mixinsutil.EntityYawPitchAccessible;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TOSEntry implements ModInitializer, ClientModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final String NAMESPACE = "totemofshadow";
    private static final Identifier TOTEM_OF_SHADOW_ID = new Identifier(NAMESPACE, "totem_of_shadow");
    private static final Identifier SHADOW_ENTITY_ID = new Identifier(NAMESPACE, "shadow_entity");
    private static final Identifier SPIRIT_ENTITY_ID = new Identifier(NAMESPACE, "spirit_entity");
    public static final Identifier YAW_PITCH_UPDATE_ID = new Identifier(NAMESPACE, "yaw_pitch_update");

    public static final Item TOTEM_OF_SHADOW = new TotemOfShadowItem();
    public static volatile EntityType<ShadowEntity> SHADOW_ENTITY;
    private static volatile EntityType<SpiritEntity> SPIRIT_ENTITY;

    @Override
    @Environment(EnvType.CLIENT)
    public void onInitializeClient() {
        LOGGER.info("Initialized client mod.");
        EntityRendererRegistry.INSTANCE.register(SHADOW_ENTITY, (dispatcher, context) -> new ShadowEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(SPIRIT_ENTITY, (dispatcher, context) -> new SpiritEntityRenderer(dispatcher));
        ClientSidePacketRegistry.INSTANCE.register(YAW_PITCH_UPDATE_ID, (context, buf) -> {
            final int id = buf.readVarInt();
            final float yaw = buf.readFloat();
            final float pitch = buf.readFloat();
            context.getTaskQueue().execute(() -> {
                final Entity entity = context.getPlayer().getEntityWorld().getEntityById(id);
                if (!(entity instanceof EntityYawPitchAccessible)) return;
                ((EntityYawPitchAccessible) entity).onYawPitchUpdate(yaw, pitch);
            });
        });
    }

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, TOTEM_OF_SHADOW_ID, TOTEM_OF_SHADOW);
        SHADOW_ENTITY = Registry.register(
                Registry.ENTITY_TYPE,
                SHADOW_ENTITY_ID,
                FabricEntityTypeBuilder
                        .create(SpawnGroup.MONSTER, ShadowEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build()
        );
        SPIRIT_ENTITY = Registry.register(
                Registry.ENTITY_TYPE,
                SPIRIT_ENTITY_ID,
                FabricEntityTypeBuilder
                        .create(SpawnGroup.MONSTER, SpiritEntity::new)
                        .dimensions(EntityDimensions.fixed(.45f, .45f)).build()
        );
        FabricDefaultAttributeRegistry.register(SHADOW_ENTITY, PathAwareEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(SPIRIT_ENTITY, PathAwareEntity.createMobAttributes());
        LOGGER.info("Initialized main mod.");
    }
}
