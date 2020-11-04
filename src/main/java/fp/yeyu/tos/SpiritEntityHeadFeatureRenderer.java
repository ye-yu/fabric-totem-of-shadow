package fp.yeyu.tos;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.render.entity.model.SkullOverlayEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SpiritEntityHeadFeatureRenderer extends FeatureRenderer<SpiritEntity, SpiritEntityModel> {

    private static final SkullEntityModel skullEntityModel = new SkullOverlayEntityModel();
    private static final GameProfile DUMMY_GP = new GameProfile(new UUID(1, 0), "Steve");


    public SpiritEntityHeadFeatureRenderer(FeatureRendererContext<SpiritEntity, SpiritEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, SpiritEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        matrixStack.push();
        this.getContextModel().getHead().rotate(matrixStack);

        matrixStack.scale(0.9F, -0.9F, -0.9F);
        final GameProfile gameProfileLocal = Optional.ofNullable(MinecraftClient.getInstance()).map(e -> e.player).map(PlayerEntity::getGameProfile).orElse(DUMMY_GP);
        final GameProfile gameProfile = Optional.ofNullable(entity.closestPlayer).map(PlayerEntity::getGameProfile).orElse(gameProfileLocal);

        matrixStack.translate(-0.5D, -0.23D, -0.5D);
        render(180.0F, gameProfile, entity.pitch, matrixStack, vertexConsumerProvider, light, entity.getHeadAlphaLevelPlayer().next().floatValue());
        matrixStack.pop();
    }

    public static void render(float limbAngle, GameProfile gameProfile, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, float alpha) {
        matrixStack.push();
        matrixStack.translate(0.5D, 0.0D, 0.5D);
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(getTransparencyRenderLayer(gameProfile));
        skullEntityModel.method_2821(g, limbAngle, 0.0F);
        skullEntityModel.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, alpha);
        matrixStack.pop();
    }

    private static RenderLayer getTransparencyRenderLayer(GameProfile gameProfile) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraftClient.getSkinProvider().getTextures(gameProfile);
        return map.containsKey(MinecraftProfileTexture.Type.SKIN) ?
                RenderLayer.getEntityTranslucent(minecraftClient.getSkinProvider().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN)) :
                RenderLayer.getEntityTranslucent(DefaultSkinHelper.getTexture(PlayerEntity.getUuidFromProfile(gameProfile)));
    }

}
