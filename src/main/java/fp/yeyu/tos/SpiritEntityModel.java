package fp.yeyu.tos;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;

public class SpiritEntityModel extends CompositeEntityModel<SpiritEntity> implements ModelWithHead {

    private final ArrayList<ModelPart> modelParts = new ArrayList<>();
    private float leaningPitch = 0;

    public SpiritEntityModel() {
        super(RenderLayer::getEntityTranslucentCull);
        this.textureHeight = 6 * 2;
        this.textureWidth = 6 * 4;
        modelParts.add(new ModelPart(this, 0, 0));
        modelParts.get(0).addCuboid(-3F, -3F, -3F, 6.0F, 6.0F, 6.0F);

        modelParts.add(new ModelPart(this, 0, 0));
        modelParts.get(1).addCuboid(0, 0, 0, 0, 0, 0, 0);
        modelParts.get(1).visible = false;

        modelParts.get(0).setPivot(0, 20f, 0);
        modelParts.get(1).setPivot(0, 20f, 0);
    }

    @Override
    public Iterable<ModelPart> getParts() {
        return modelParts;
    }

    @Override
    public void setAngles(SpiritEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        modelParts.forEach(modelPart -> {
            modelPart.yaw = headYaw * 0.017453292F;
            if (this.leaningPitch > 0.0F)
                modelPart.pitch = this.lerpAngle(this.leaningPitch, modelPart.pitch, headPitch * 0.017453292F);
            else modelPart.pitch = headPitch * 0.017453292F;

        });
    }

    @Override
    public ModelPart getHead() {
        return modelParts.get(1);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        modelParts.get(0).render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    protected float lerpAngle(float to, float from, float maxChange) {
        float i = (maxChange - from) % 6.2831855F;
        if (i < -3.1415927F) {
            i += 6.2831855F;
        }

        if (i >= 3.1415927F) {
            i -= 6.2831855F;
        }

        return from + to * i;
    }

    @Override
    public void animateModel(SpiritEntity entity, float limbAngle, float limbDistance, float tickDelta) {
        this.leaningPitch = entity.getLeaningPitch(tickDelta);
    }
}
