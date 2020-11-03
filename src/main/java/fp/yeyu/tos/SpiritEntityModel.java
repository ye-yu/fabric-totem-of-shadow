package fp.yeyu.tos;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;

public class SpiritEntityModel extends CompositeEntityModel<SpiritEntity> {

    private final ArrayList<ModelPart> modelParts = new ArrayList<>();

    public SpiritEntityModel() {
        super();
        this.textureHeight = 6 * 2;
        this.textureWidth = 6 * 4;
        modelParts.add(new ModelPart(this, 0, 0));
        modelParts.get(0).addCuboid(-3F, 18.0F, -3F, 6.0F, 6.0F, 6.0F);
    }

    @Override
    public Iterable<ModelPart> getParts() {
        return modelParts;
    }

    @Override
    public void setAngles(SpiritEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }
}
