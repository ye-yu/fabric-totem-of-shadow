package fp.yeyu.tos.client

import fp.yeyu.tos.TotemOfShadowEntry
import fp.yeyu.tos.entity.SpiritEntity
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.util.Identifier

class SpiritEntityRenderer(entityRenderDispatcher: EntityRenderDispatcher?, entityModel: SpiritEntityModel, shadowRadius: Float) : MobEntityRenderer<SpiritEntity, SpiritEntityModel>(entityRenderDispatcher, entityModel, shadowRadius) {
    constructor(dispatcher: EntityRenderDispatcher?) : this(dispatcher, SpiritEntityModel(), 0.2f)

    override fun getTexture(entity: SpiritEntity): Identifier {
        return TEXTURE
    }

    companion object {
        private val TEXTURE = Identifier(TotemOfShadowEntry.NAMESPACE, "textures/entity/spirit_entity.png")
    }

    init {
        addFeature(SpiritEntityHeadFeatureRenderer(this))
    }
}