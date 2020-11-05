package fp.yeyu.tos.client

import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import fp.yeyu.tos.entity.SpiritEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.feature.FeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.SkullEntityModel
import net.minecraft.client.render.entity.model.SkullOverlayEntityModel
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import java.util.*

class SpiritEntityHeadFeatureRenderer(context: FeatureRendererContext<SpiritEntity?, SpiritEntityModel?>?) : FeatureRenderer<SpiritEntity, SpiritEntityModel?>(context) {
    override fun render(matrixStack: MatrixStack, vertexConsumerProvider: VertexConsumerProvider, light: Int, entity: SpiritEntity, limbAngle: Float, limbDistance: Float, tickDelta: Float, animationProgress: Float, headYaw: Float, headPitch: Float) {
        matrixStack.push()
        val contextModel = this.contextModel ?: return
        contextModel.head.rotate(matrixStack)
        matrixStack.scale(0.9f, -0.9f, -0.9f)
        val gameProfile = entity.closestPlayer?.gameProfile ?: MinecraftClient.getInstance()?.player?.gameProfile ?: dummyGameProfile
        matrixStack.translate(-0.5, -0.23, -0.5)
        render(gameProfile, entity.pitch, matrixStack, vertexConsumerProvider, light, entity.headAlphaLevelPlayer.next().toFloat())
        matrixStack.pop()
    }

    private val skullEntityModel: SkullEntityModel = SkullOverlayEntityModel()
    private val dummyGameProfile = GameProfile(UUID(1, 0), "Steve")

    private fun render(gameProfile: GameProfile, limbAngle: Float, matrixStack: MatrixStack, vertexConsumerProvider: VertexConsumerProvider, light: Int, alpha: Float) {
        matrixStack.push()
        matrixStack.translate(0.5, 0.0, 0.5)
        matrixStack.scale(-1.0f, -1.0f, 1.0f)
        val vertexConsumer = vertexConsumerProvider.getBuffer(getTransparencyRenderLayer(gameProfile))
        setAngle(limbAngle, 180f, 0.0f)
        skullEntityModel.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, alpha)
        matrixStack.pop()
    }

    private fun getTransparencyRenderLayer(gameProfile: GameProfile): RenderLayer {
        val minecraftClient = MinecraftClient.getInstance()
        val map = minecraftClient.skinProvider.getTextures(gameProfile)
        return if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) RenderLayer.getEntityTranslucent(minecraftClient.skinProvider.loadSkin(map[MinecraftProfileTexture.Type.SKIN], MinecraftProfileTexture.Type.SKIN)) else RenderLayer.getEntityTranslucent(DefaultSkinHelper.getTexture(PlayerEntity.getUuidFromProfile(gameProfile)))
    }

    private fun setAngle(factor: Float, from: Float, to: Float) {
        skullEntityModel.method_2821(factor, from, to)
    }
}