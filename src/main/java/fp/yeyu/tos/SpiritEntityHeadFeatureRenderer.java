package fp.yeyu.tos;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import io.github.yeyu.easing.EaseInOutImpl;
import io.github.yeyu.easing.function.InverseQuadratic;
import io.github.yeyu.easing.function.QuadraticFunction;
import io.github.yeyu.easing.interpolator.DoubleToDoubleInterpolator;
import io.github.yeyu.easing.player.PersistentFramefulEasePlayer;
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
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Map;
import java.util.Random;

public class SpiritEntityHeadFeatureRenderer extends FeatureRenderer<SpiritEntity, SpiritEntityModel> {

    private static PlayerEntity player = null;
    private static final SkullEntityModel skullEntityModel = new SkullOverlayEntityModel();
    private final PersistentFramefulEasePlayer<Double> translucentPlayer;
    private static final double TRANSLUCENT_LOWER = 0.02;
    private static final double TRANSLUCENT_UPPER = 0.55;
    private static final double TRANSLUCENT_UPPER_2 = 0.75;
    private static final int TICK_DEF = 35;
    private static final Random r = new Random();
    private boolean show = false;

    public SpiritEntityHeadFeatureRenderer(FeatureRendererContext<SpiritEntity, SpiritEntityModel> context) {
        super(context);
        translucentPlayer = new PersistentFramefulEasePlayer<>(
                new EaseInOutImpl<>(TRANSLUCENT_LOWER, TRANSLUCENT_UPPER, InverseQuadratic.INSTANCE, QuadraticFunction.INSTANCE, DoubleToDoubleInterpolator.INSTANCE),
                TICK_DEF
        );
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, SpiritEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player == null || player.isDead()) {
            final MinecraftClient instance = MinecraftClient.getInstance();
            if (instance == null) return;
            player = instance.player;
            if (player == null) return;
        }
        final Item heldItem = player.inventory.getStack(player.inventory.selectedSlot).getItem();
        final boolean shouldRenderSkull = heldItem == Items.LANTERN || heldItem == Items.SOUL_LANTERN;
        if (shouldRenderSkull != show) {
            final double destination = shouldRenderSkull ? TRANSLUCENT_UPPER : TRANSLUCENT_LOWER;
            translucentPlayer.setTransitionTo(destination);
            show = shouldRenderSkull;
        } else if (show && r.nextDouble() < 0.1) {
            translucentPlayer.setTransitionTo(r.nextDouble() < 0.5 ? TRANSLUCENT_UPPER_2 : TRANSLUCENT_UPPER);
        }

        matrixStack.push();
        this.getContextModel().getHead().rotate(matrixStack);

        matrixStack.scale(0.9F, -0.9F, -0.9F);
        GameProfile gameProfile = player.getGameProfile();

        matrixStack.translate(-0.5D, -0.23D, -0.5D);
        render(180.0F, gameProfile, entity.pitch, matrixStack, vertexConsumerProvider, light, translucentPlayer.next().floatValue());
        matrixStack.pop();
    }

    public static void render(float limbAngle, GameProfile gameProfile, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, float alpha) {
        matrixStack.push();
        matrixStack.translate(0.5D, 0.0D, 0.5D);
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(method_3578(gameProfile));
        skullEntityModel.method_2821(g, limbAngle, 0.0F);
        skullEntityModel.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, alpha);
        matrixStack.pop();
    }

    private static RenderLayer method_3578(GameProfile gameProfile) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraftClient.getSkinProvider().getTextures(gameProfile);
        return map.containsKey(MinecraftProfileTexture.Type.SKIN) ?
                RenderLayer.getEntityAlpha(minecraftClient.getSkinProvider().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN), 0.5f) :
                RenderLayer.getEntityTranslucent(DefaultSkinHelper.getTexture(PlayerEntity.getUuidFromProfile(gameProfile)));
    }

}
