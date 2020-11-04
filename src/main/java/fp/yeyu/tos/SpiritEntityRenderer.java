package fp.yeyu.tos;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public void render(SpiritEntity mobEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(mobEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
