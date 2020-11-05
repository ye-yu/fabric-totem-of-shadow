package fp.yeyu.tos.client

import fp.yeyu.tos.entity.ShadowEntity
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.client.render.entity.PlayerModelPart
import net.minecraft.client.render.entity.feature.*
import net.minecraft.client.render.entity.model.BipedEntityModel
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.entity.Entity
import net.minecraft.item.CrossbowItem
import net.minecraft.item.Items
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Arm
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.UseAction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

class ShadowEntityRenderer(dispatcher: EntityRenderDispatcher?) : LivingEntityRenderer<ShadowEntity, PlayerEntityModel<ShadowEntity>>(dispatcher, PlayerEntityModel(0.0f, false), 0.5f) {
    private var reconfiguredSlim = false
    override fun render(shadowEntity: ShadowEntity, yaw: Float, tickDelta: Float, matrixStack: MatrixStack, vertexConsumerProvider: VertexConsumerProvider, light: Int) {
        val copyingEntity = shadowEntity.copyingEntity
        if (copyingEntity is AbstractClientPlayerEntity && !reconfiguredSlim) {
            val isSlim = copyingEntity.model == "slim"
            model = PlayerEntityModel(0f, isSlim)
            reconfiguredSlim = true
        }
        setModelPose(shadowEntity)
        super.render(shadowEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light)
    }

    override fun getPositionOffset(shadowEntity: ShadowEntity, tickDelta: Float): Vec3d =
            if (shadowEntity.isInSneakingPose) Vec3d(0.0, -0.125, 0.0)
            else super.getPositionOffset(shadowEntity, tickDelta)

    private fun setModelPose(shadowEntity: ShadowEntity) {
        val playerEntityModel = getModel() ?: return
        if (shadowEntity.isSpectator) {
            playerEntityModel.setVisible(false)
            playerEntityModel.head.visible = true
            playerEntityModel.helmet.visible = true
        } else {
            playerEntityModel.setVisible(true)
            playerEntityModel.helmet.visible = shadowEntity.isPartVisible(PlayerModelPart.HAT)
            playerEntityModel.jacket.visible = shadowEntity.isPartVisible(PlayerModelPart.JACKET)
            playerEntityModel.leftPantLeg.visible = shadowEntity.isPartVisible(PlayerModelPart.LEFT_PANTS_LEG)
            playerEntityModel.rightPantLeg.visible = shadowEntity.isPartVisible(PlayerModelPart.RIGHT_PANTS_LEG)
            playerEntityModel.leftSleeve.visible = shadowEntity.isPartVisible(PlayerModelPart.LEFT_SLEEVE)
            playerEntityModel.rightSleeve.visible = shadowEntity.isPartVisible(PlayerModelPart.RIGHT_SLEEVE)
            playerEntityModel.sneaking = shadowEntity.isInSneakingPose
            val armPose = getArmPose(shadowEntity, Hand.MAIN_HAND)
            var armPose2 = getArmPose(shadowEntity, Hand.OFF_HAND)
            if (armPose.method_30156()) {
                armPose2 = if (shadowEntity.offHandStack.isEmpty) ArmPose.EMPTY else ArmPose.ITEM
            }
            if (shadowEntity.mainArm == Arm.RIGHT) {
                playerEntityModel.rightArmPose = armPose
                playerEntityModel.leftArmPose = armPose2
            } else {
                playerEntityModel.rightArmPose = armPose2
                playerEntityModel.leftArmPose = armPose
            }
        }
    }

    override fun getTexture(shadowEntity: ShadowEntity): Identifier = shadowEntity.skinTexture

    override fun scale(shadowEntity: ShadowEntity, matrixStack: MatrixStack, amount: Float) {
        matrixStack.scale(0.9375f, 0.9375f, 0.9375f)
    }

    override fun renderLabelIfPresent(shadowEntity: ShadowEntity, text: Text, matrixStack: MatrixStack, vertexConsumerProvider: VertexConsumerProvider, light: Int) {
        val labelDistance = dispatcher.getSquaredDistanceToCamera(shadowEntity)
        matrixStack.push()
        if (labelDistance < 100.0) {
            val scoreboard = shadowEntity.world.scoreboard
            val scoreboardObjective = scoreboard.getObjectiveForSlot(2)
            if (scoreboardObjective != null) {
                val scoreboardPlayerScore = scoreboard.getPlayerScore(shadowEntity.entityName, scoreboardObjective)
                super.renderLabelIfPresent(shadowEntity, LiteralText(Integer.toString(scoreboardPlayerScore.score)).append(" ").append(scoreboardObjective.displayName), matrixStack, vertexConsumerProvider, light)
                matrixStack.translate(0.0, 9.0f * 1.15f * 0.025f.toDouble(), 0.0)
            }
        }
        super.renderLabelIfPresent(shadowEntity, text, matrixStack, vertexConsumerProvider, light)
        matrixStack.pop()
    }

    override fun setupTransforms(shadowEntity: ShadowEntity, matrixStack: MatrixStack, animationProgress: Float, bodyYaw: Float, tickDelta: Float) {
        val leaningPitch = shadowEntity.getLeaningPitch(tickDelta)
        val rollTickDelta: Float
        val fallFlyingFactor: Float
        if (shadowEntity.isFallFlying) {
            super.setupTransforms(shadowEntity, matrixStack, animationProgress, bodyYaw, tickDelta)
            rollTickDelta = shadowEntity.roll.toFloat() + tickDelta
            fallFlyingFactor = MathHelper.clamp(rollTickDelta * rollTickDelta / 100.0f, 0.0f, 1.0f)
            if (!shadowEntity.isUsingRiptide) matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(fallFlyingFactor * (-90.0f - shadowEntity.pitch)))
            val vec3d = shadowEntity.getRotationVec(tickDelta)
            val vec3d2 = shadowEntity.velocity
            val d = Entity.squaredHorizontalLength(vec3d2)
            val e = Entity.squaredHorizontalLength(vec3d)
            if (d <= 0.0 || e <= 0.0) return
            val l = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e)
            val m = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x
            matrixStack.multiply(Vector3f.POSITIVE_Y.getRadialQuaternion((Math.signum(m) * Math.acos(l)).toFloat()))
        } else if (leaningPitch > 0.0f) {
            super.setupTransforms(shadowEntity, matrixStack, animationProgress, bodyYaw, tickDelta)
            rollTickDelta = if (shadowEntity.isTouchingWater) -90.0f - shadowEntity.pitch else -90.0f
            fallFlyingFactor = MathHelper.lerp(leaningPitch, 0.0f, rollTickDelta)
            matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(fallFlyingFactor))
            if (shadowEntity.isInSwimmingPose) matrixStack.translate(0.0, -1.0, 0.3)
        } else super.setupTransforms(shadowEntity, matrixStack, animationProgress, bodyYaw, tickDelta)
    }

    private fun getArmPose(shadowEntity: ShadowEntity, hand: Hand): ArmPose {
        val itemStack = shadowEntity.getStackInHand(hand)
        return if (itemStack.isEmpty) ArmPose.EMPTY else {
            if (shadowEntity.activeHand == hand && shadowEntity.itemUseTimeLeft > 0) {
                val useAction = itemStack.useAction
                if (useAction == UseAction.BLOCK) {
                    return ArmPose.BLOCK
                }
                if (useAction == UseAction.BOW) {
                    return ArmPose.BOW_AND_ARROW
                }
                if (useAction == UseAction.SPEAR) {
                    return ArmPose.THROW_SPEAR
                }
                if (useAction == UseAction.CROSSBOW && hand == shadowEntity.activeHand) {
                    return ArmPose.CROSSBOW_CHARGE
                }
            } else if (!shadowEntity.handSwinging && itemStack.item === Items.CROSSBOW && CrossbowItem.isCharged(itemStack)) {
                return ArmPose.CROSSBOW_HOLD
            }
            ArmPose.ITEM
        }
    }

    init {
        addFeature(ArmorFeatureRenderer<ShadowEntity, PlayerEntityModel<ShadowEntity>, BipedEntityModel<ShadowEntity>>(this, BipedEntityModel(0.5f), BipedEntityModel(1.0f)))
        addFeature(HeldItemFeatureRenderer<ShadowEntity, PlayerEntityModel<ShadowEntity>>(this))
        addFeature(StuckArrowsFeatureRenderer<ShadowEntity, PlayerEntityModel<ShadowEntity>>(this))
        addFeature(HeadFeatureRenderer<ShadowEntity, PlayerEntityModel<ShadowEntity>>(this))
        addFeature(ElytraFeatureRenderer<ShadowEntity, PlayerEntityModel<ShadowEntity>>(this))
        addFeature(TridentRiptideFeatureRenderer(this))
        addFeature(StuckStingersFeatureRenderer<ShadowEntity, PlayerEntityModel<ShadowEntity>>(this))
    }
}