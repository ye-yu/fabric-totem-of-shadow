package fp.yeyu.tos;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class ShadowEntityRenderer extends LivingEntityRenderer<ShadowEntity, PlayerEntityModel<ShadowEntity>> {

    private boolean reconfiguredSlim = false;
    public ShadowEntityRenderer(EntityRenderDispatcher dispatcher) {
        super(dispatcher, new PlayerEntityModel<>(0.0F, false), 0.5F);
        this.addFeature(new ArmorFeatureRenderer<>(this, new BipedEntityModel<>(0.5F), new BipedEntityModel<>(1.0F)));
        this.addFeature(new HeldItemFeatureRenderer<>(this));
        this.addFeature(new StuckArrowsFeatureRenderer<>(this));
        this.addFeature(new HeadFeatureRenderer<>(this));
        this.addFeature(new ElytraFeatureRenderer<>(this));
        this.addFeature(new TridentRiptideFeatureRenderer<>(this));
        this.addFeature(new StuckStingersFeatureRenderer<>(this));
    }

    public void render(ShadowEntity shadowEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        final PlayerEntity copyingEntity = shadowEntity.copyingEntity;
        if (copyingEntity instanceof AbstractClientPlayerEntity && !reconfiguredSlim) {
            final boolean isSlim = ((AbstractClientPlayerEntity) copyingEntity).getModel().equals("slim");
            this.model = new PlayerEntityModel<>(0, isSlim);
            reconfiguredSlim = true;
        }
        this.setModelPose(shadowEntity);
        super.render(shadowEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }

    public Vec3d getPositionOffset(ShadowEntity shadowEntity, float tickDelta) {
        return shadowEntity.isInSneakingPose() ? new Vec3d(0.0D, -0.125D, 0.0D) : super.getPositionOffset(shadowEntity, tickDelta);
    }

    private void setModelPose(ShadowEntity shadowEntity) {
        PlayerEntityModel<ShadowEntity> playerEntityModel = this.getModel();
        if (shadowEntity.isSpectator()) {
            playerEntityModel.setVisible(false);
            playerEntityModel.head.visible = true;
            playerEntityModel.helmet.visible = true;
        } else {
            playerEntityModel.setVisible(true);
            playerEntityModel.helmet.visible = shadowEntity.isPartVisible(PlayerModelPart.HAT);
            playerEntityModel.jacket.visible = shadowEntity.isPartVisible(PlayerModelPart.JACKET);
            playerEntityModel.leftPantLeg.visible = shadowEntity.isPartVisible(PlayerModelPart.LEFT_PANTS_LEG);
            playerEntityModel.rightPantLeg.visible = shadowEntity.isPartVisible(PlayerModelPart.RIGHT_PANTS_LEG);
            playerEntityModel.leftSleeve.visible = shadowEntity.isPartVisible(PlayerModelPart.LEFT_SLEEVE);
            playerEntityModel.rightSleeve.visible = shadowEntity.isPartVisible(PlayerModelPart.RIGHT_SLEEVE);
            playerEntityModel.sneaking = shadowEntity.isInSneakingPose();
            BipedEntityModel.ArmPose armPose = getArmPose(shadowEntity, Hand.MAIN_HAND);
            BipedEntityModel.ArmPose armPose2 = getArmPose(shadowEntity, Hand.OFF_HAND);
            if (armPose.method_30156()) {
                armPose2 = shadowEntity.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
            }

            if (shadowEntity.getMainArm() == Arm.RIGHT) {
                playerEntityModel.rightArmPose = armPose;
                playerEntityModel.leftArmPose = armPose2;
            } else {
                playerEntityModel.rightArmPose = armPose2;
                playerEntityModel.leftArmPose = armPose;
            }
        }

    }

    private static BipedEntityModel.ArmPose getArmPose(ShadowEntity shadowEntity, Hand hand) {
        ItemStack itemStack = shadowEntity.getStackInHand(hand);
        if (itemStack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        } else {
            if (shadowEntity.getActiveHand() == hand && shadowEntity.getItemUseTimeLeft() > 0) {
                UseAction useAction = itemStack.getUseAction();
                if (useAction == UseAction.BLOCK) {
                    return BipedEntityModel.ArmPose.BLOCK;
                }

                if (useAction == UseAction.BOW) {
                    return BipedEntityModel.ArmPose.BOW_AND_ARROW;
                }

                if (useAction == UseAction.SPEAR) {
                    return BipedEntityModel.ArmPose.THROW_SPEAR;
                }

                if (useAction == UseAction.CROSSBOW && hand == shadowEntity.getActiveHand()) {
                    return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                }
            } else if (!shadowEntity.handSwinging && itemStack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(itemStack)) {
                return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
            }

            return BipedEntityModel.ArmPose.ITEM;
        }
    }

    public Identifier getTexture(ShadowEntity shadowEntity) {
        return shadowEntity.getSkinTexture();
    }

    protected void scale(ShadowEntity shadowEntity, MatrixStack matrixStack, float amount) {
        matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    protected void renderLabelIfPresent(ShadowEntity shadowEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        double labelDistance = this.dispatcher.getSquaredDistanceToCamera(shadowEntity);
        matrixStack.push();
        if (labelDistance < 100.0D) {
            Scoreboard scoreboard = shadowEntity.world.getScoreboard();
            ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(2);
            if (scoreboardObjective != null) {
                ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(shadowEntity.getEntityName(), scoreboardObjective);
                super.renderLabelIfPresent(shadowEntity, (new LiteralText(Integer.toString(scoreboardPlayerScore.getScore()))).append(" ").append(scoreboardObjective.getDisplayName()), matrixStack, vertexConsumerProvider, light);
                matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
            }
        }

        super.renderLabelIfPresent(shadowEntity, text, matrixStack, vertexConsumerProvider, light);
        matrixStack.pop();
    }

    protected void setupTransforms(ShadowEntity shadowEntity, MatrixStack matrixStack, float animationProgress, float bodyYaw, float tickDelta) {
        float leaningPitch = shadowEntity.getLeaningPitch(tickDelta);
        float rollTickDelta;
        float k;
        if (shadowEntity.isFallFlying()) {
            super.setupTransforms(shadowEntity, matrixStack, animationProgress, bodyYaw, tickDelta);
            rollTickDelta = (float) shadowEntity.getRoll() + tickDelta;
            k = MathHelper.clamp(rollTickDelta * rollTickDelta / 100.0F, 0.0F, 1.0F);
            if (!shadowEntity.isUsingRiptide()) {
                matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(k * (-90.0F - shadowEntity.pitch)));
            }

            Vec3d vec3d = shadowEntity.getRotationVec(tickDelta);
            Vec3d vec3d2 = shadowEntity.getVelocity();
            double d = Entity.squaredHorizontalLength(vec3d2);
            double e = Entity.squaredHorizontalLength(vec3d);
            if (d > 0.0D && e > 0.0D) {
                double l = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
                double m = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
                matrixStack.multiply(Vector3f.POSITIVE_Y.getRadialQuaternion((float) (Math.signum(m) * Math.acos(l))));
            }
        } else if (leaningPitch > 0.0F) {
            super.setupTransforms(shadowEntity, matrixStack, animationProgress, bodyYaw, tickDelta);
            rollTickDelta = shadowEntity.isTouchingWater() ? -90.0F - shadowEntity.pitch : -90.0F;
            k = MathHelper.lerp(leaningPitch, 0.0F, rollTickDelta);
            matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(k));
            if (shadowEntity.isInSwimmingPose()) {
                matrixStack.translate(0.0D, -1.0D, 0.30000001192092896D);
            }
        } else {
            super.setupTransforms(shadowEntity, matrixStack, animationProgress, bodyYaw, tickDelta);
        }

    }
}
