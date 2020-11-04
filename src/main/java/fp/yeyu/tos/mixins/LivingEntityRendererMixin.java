package fp.yeyu.tos.mixins;

import fp.yeyu.tos.SpiritEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M> {


    @Shadow protected M model;

    @Shadow protected abstract float getHandSwingProgress(T entity, float tickDelta);

    @Shadow protected abstract float getAnimationProgress(T entity, float tickDelta);

    @Shadow protected abstract void setupTransforms(T entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta);

    @Shadow protected abstract void scale(T entity, MatrixStack matrices, float amount);

    @Shadow protected abstract boolean isVisible(T entity);

    @Shadow protected @Nullable abstract RenderLayer getRenderLayer(T entity, boolean showBody, boolean translucent, boolean showOutline);

    @Shadow protected abstract float getAnimationCounter(T entity, float tickDelta);

    @Shadow @Final protected List<FeatureRenderer<T, M>> features;

    protected LivingEntityRendererMixin(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(T entity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        if (!(entity instanceof SpiritEntity)) return;
        SpiritEntity spiritEntity = (SpiritEntity) entity;
        matrixStack.push();
        model.handSwingProgress = this.getHandSwingProgress(entity, tickDelta);
        this.model.riding = spiritEntity.hasVehicle();
        this.model.child = spiritEntity.isBaby();
        float nextBodyYaw = MathHelper.lerpAngleDegrees(tickDelta, spiritEntity.prevBodyYaw, spiritEntity.bodyYaw);
        float nextHeadYaw = MathHelper.lerpAngleDegrees(tickDelta, spiritEntity.prevHeadYaw, spiritEntity.headYaw);
        float yawDiff = nextHeadYaw - nextBodyYaw;
        float nextYaw;
        if (spiritEntity.hasVehicle() && spiritEntity.getVehicle() instanceof LivingEntity) {
            LivingEntity livingEntity2 = (LivingEntity)spiritEntity.getVehicle();
            nextBodyYaw = MathHelper.lerpAngleDegrees(tickDelta, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
            yawDiff = nextHeadYaw - nextBodyYaw;
            nextYaw = MathHelper.wrapDegrees(yawDiff);
            if (nextYaw < -85.0F) {
                nextYaw = -85.0F;
            }

            if (nextYaw >= 85.0F) {
                nextYaw = 85.0F;
            }

            nextBodyYaw = nextHeadYaw - nextYaw;
            if (nextYaw * nextYaw > 2500.0F) {
                nextBodyYaw += nextYaw * 0.2F;
            }

            yawDiff = nextHeadYaw - nextBodyYaw;
        }

        float nextPitch = MathHelper.lerp(tickDelta, spiritEntity.prevPitch, spiritEntity.pitch);
        float sleepingField;
        if (spiritEntity.getPose() == EntityPose.SLEEPING) {
            Direction direction = spiritEntity.getSleepingDirection();
            if (direction != null) {
                sleepingField = spiritEntity.getEyeHeight(EntityPose.STANDING) - 0.1F;
                matrixStack.translate((float)(-direction.getOffsetX()) * sleepingField, 0.0D, (float)(-direction.getOffsetZ()) * sleepingField);
            }
        }

        nextYaw = this.getAnimationProgress(entity, tickDelta);
        this.setupTransforms(entity, matrixStack, nextYaw, nextBodyYaw, tickDelta);
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        this.scale(entity, matrixStack, tickDelta);
        matrixStack.translate(0.0D, -1.5010000467300415D, 0.0D);
        float nextLimbDistance = 0.0F;
        float nextLimbAndgle = 0.0F;
        if (!spiritEntity.hasVehicle() && spiritEntity.isAlive()) {
            nextLimbDistance = MathHelper.lerp(tickDelta, spiritEntity.lastLimbDistance, spiritEntity.limbDistance);
            nextLimbAndgle = spiritEntity.limbAngle - spiritEntity.limbDistance * (1.0F - tickDelta);
            if (spiritEntity.isBaby()) {
                nextLimbAndgle *= 3.0F;
            }

            if (nextLimbDistance > 1.0F) {
                nextLimbDistance = 1.0F;
            }
        }

        this.model.animateModel(entity, nextLimbAndgle, nextLimbDistance, tickDelta);
        this.model.setAngles(entity, nextLimbAndgle, nextLimbDistance, nextYaw, yawDiff, nextPitch);
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        boolean visible = this.isVisible(entity);
        boolean translucent = !visible && !spiritEntity.isInvisibleTo(minecraftClient.player);
        boolean outline = minecraftClient.hasOutline(spiritEntity);
        RenderLayer renderLayer = this.getRenderLayer(entity, visible, translucent, outline);
        if (renderLayer != null) {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);
            int overlay = LivingEntityRenderer.getOverlay(spiritEntity, this.getAnimationCounter(entity, tickDelta));
            this.model.render(matrixStack, vertexConsumer, light, overlay, 1.0F, 1.0F, 1.0F, spiritEntity.getSelfAlphaLevelPlayer().next().floatValue());
        }

        if (!spiritEntity.isSpectator()) {
            for (FeatureRenderer<T, M> feature : this.features) {
                feature.render(matrixStack, vertexConsumerProvider, light, entity, nextLimbAndgle, nextLimbDistance, tickDelta, nextYaw, yawDiff, nextPitch);
            }
        }

        matrixStack.pop();
        super.render(entity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
        ci.cancel();
    }
}
