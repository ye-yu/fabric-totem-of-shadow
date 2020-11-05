package fp.yeyu.tos.client

import fp.yeyu.tos.entity.SpiritEntity
import net.minecraft.client.model.ModelPart
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.CompositeEntityModel
import net.minecraft.client.render.entity.model.ModelWithHead
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import java.util.*
import java.util.function.Consumer

class SpiritEntityModel : CompositeEntityModel<SpiritEntity>({ texture: Identifier -> RenderLayer.getEntityTranslucentCull(texture) }), ModelWithHead {
    private val modelParts = ArrayList<ModelPart>()
    private var leaningPitch = 0f
    override fun getParts(): Iterable<ModelPart> {
        return modelParts
    }

    override fun setAngles(entity: SpiritEntity, limbAngle: Float, limbDistance: Float, animationProgress: Float, headYaw: Float, headPitch: Float) {
        modelParts.forEach(Consumer { modelPart: ModelPart ->
            modelPart.yaw = headYaw * 0.017453292f
            if (leaningPitch > 0.0f) modelPart.pitch = lerpAngle(leaningPitch, modelPart.pitch, headPitch * 0.017453292f) else modelPart.pitch = headPitch * 0.017453292f
        })
    }

    override fun getHead(): ModelPart {
        return modelParts[1]
    }

    override fun render(matrices: MatrixStack, vertices: VertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float) {
        modelParts[0].render(matrices, vertices, light, overlay, red, green, blue, alpha)
    }

    private fun lerpAngle(to: Float, from: Float, maxChange: Float): Float {
        var i = (maxChange - from) % 6.2831855f
        if (i < -3.1415927f) {
            i += 6.2831855f
        }
        if (i >= 3.1415927f) {
            i -= 6.2831855f
        }
        return from + to * i
    }

    override fun animateModel(entity: SpiritEntity, limbAngle: Float, limbDistance: Float, tickDelta: Float) {
        leaningPitch = entity.getLeaningPitch(tickDelta)
    }

    init {
        textureHeight = 6 * 2
        textureWidth = 6 * 4
        modelParts.add(ModelPart(this, 0, 0))
        modelParts[0].addCuboid(-3f, -3f, -3f, 6.0f, 6.0f, 6.0f)
        modelParts.add(ModelPart(this, 0, 0))
        modelParts[1].addCuboid(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        modelParts[1].visible = false
        modelParts[0].setPivot(0f, 20f, 0f)
        modelParts[1].setPivot(0f, 20f, 0f)
    }
}