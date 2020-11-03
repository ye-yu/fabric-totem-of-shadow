package fp.yeyu.tos;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.util.Identifier;

public class SpiritEntityRenderer extends MobEntityRenderer<SpiritEntity, SpiritEntityModel> {

    private static final Identifier TEXTURE = new Identifier(TOSEntry.NAMESPACE, "textures/entity/spirit_entity.png");

    public SpiritEntityRenderer(EntityRenderDispatcher entityRenderDispatcher, SpiritEntityModel entityModel, float shadowRadius) {
        super(entityRenderDispatcher, entityModel, shadowRadius);
        addFeature(new SpiritEntityHeadFeatureRenderer(this));
    }

    public SpiritEntityRenderer(EntityRenderDispatcher dispatcher) {
        this(dispatcher, new SpiritEntityModel(), 0.2f);
    }

    @Override
    public Identifier getTexture(SpiritEntity entity) {
        return TEXTURE;
    }
}
