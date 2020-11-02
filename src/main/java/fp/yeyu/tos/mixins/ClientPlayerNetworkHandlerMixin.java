package fp.yeyu.tos.mixins;

import fp.yeyu.tos.ShadowEntity;
import fp.yeyu.tos.ShadowEntityMobSpawnS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayerNetworkHandlerMixin {

    @Shadow
    private MinecraftClient client;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    private ClientWorld world;

    @Environment(EnvType.CLIENT)
    @Inject(method = "onMobSpawn", at = @At("HEAD"), cancellable = true)
    private void onMobSpawnEntity(MobSpawnS2CPacket p, CallbackInfo callbackInfo) {
        if (!(p instanceof ShadowEntityMobSpawnS2CPacket)) return;
        ShadowEntityMobSpawnS2CPacket packet = (ShadowEntityMobSpawnS2CPacket) p;
        NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler) (Object) this, client);
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        float g = (float) (packet.getYaw() * 360) / 256.0F;
        float h = (float) (packet.getPitch() * 360) / 256.0F;
        LivingEntity livingEntity = (LivingEntity) EntityType.createInstanceFromId(packet.getEntityTypeId(), this.client.world);
        if (livingEntity instanceof ShadowEntity) {
            livingEntity.updateTrackedPosition(d, e, f);
            livingEntity.bodyYaw = (float) (packet.getHeadYaw() * 360) / 256.0F;
            livingEntity.headYaw = (float) (packet.getHeadYaw() * 360) / 256.0F;
            livingEntity.setEntityId(packet.getId());
            livingEntity.setUuid(packet.getUuid());
            livingEntity.updatePositionAndAngles(d, e, f, g, h);
            livingEntity.setVelocity(packet.getVelocityX() / 8000.0F, packet.getVelocityY() / 8000.0F, packet.getVelocityZ() / 8000.0F);
            if (packet.getCopyingUuid() != null) ((ShadowEntity) livingEntity).setCopyingUUID(packet.getCopyingUuid());
            ((ShadowEntity) livingEntity).getPlayerListEntry();
            world.addEntity(packet.getId(), livingEntity);
        } else {
            LOGGER.warn("Skipping Entity with id {}", packet.getEntityTypeId());
        }
        callbackInfo.cancel();
    }
}
