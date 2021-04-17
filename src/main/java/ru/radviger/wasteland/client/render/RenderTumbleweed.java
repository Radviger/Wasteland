package com.legacy.wasteland.client.render;

import com.legacy.wasteland.client.model.ModelTumbleweed;
import com.legacy.wasteland.common.entity.EntityTumbleweed;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;

import java.nio.FloatBuffer;

import static com.legacy.wasteland.WastelandVersion.MOD_ID;

public class RenderTumbleweed extends Render<EntityTumbleweed> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MOD_ID, "textures/entity/tumbleweed.png");

    private static final FloatBuffer TEMP_BUF = BufferUtils.createFloatBuffer(16);
    private static final Matrix4f TEMP_MATRIX = new Matrix4f();
    public static final Quaternion TEMP_QUAT = new Quaternion();

    private ModelTumbleweed tumbleweed;
    private int lastV = 0;

    public RenderTumbleweed(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.4f;
        this.shadowOpaque = 0.8f;
        this.tumbleweed = new ModelTumbleweed();
        this.lastV = this.tumbleweed.getV();
    }

    @Override
    public void doRender(EntityTumbleweed entity, double x, double y, double z, float renderAlpha, float partialTicks) {
        if (lastV != tumbleweed.getV()) {
            this.tumbleweed = new ModelTumbleweed();
            this.lastV = tumbleweed.getV();
        }

        GlStateManager.pushMatrix();

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float alpha = 1f - entity.fadeAge / (float) EntityTumbleweed.FADE_TIME;
        alpha *= 0.7f;

        this.shadowOpaque = alpha;

        GlStateManager.color(1F, 1F, 1F, alpha);
        GlStateManager.translate((float) x, (float) y + 0.25F, (float) z);

        TEMP_BUF.clear();
        toMatrix(lerp(entity.prevQuat, entity.quat, partialTicks)).store(TEMP_BUF);
        TEMP_BUF.flip();
        GlStateManager.multMatrix(TEMP_BUF);

        GlStateManager.rotate(entity.rot1, 1, 0, 0);
        GlStateManager.rotate(entity.rot2, 0, 1, 0);
        GlStateManager.rotate(entity.rot3, 0, 0, 1);

        float size = 1F + entity.getSize() / 8F;
        GlStateManager.scale(size, size, size);

        this.bindTexture(TEXTURE);
        this.tumbleweed.render(entity, 0, 0, 0, 0, 0, 0.0625F);

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, renderAlpha, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityTumbleweed entity) {
        return TEXTURE;
    }

    private static Matrix4f toMatrix(Quaternion quat) {
        final float xx = quat.x * quat.x;
        final float xy = quat.x * quat.y;
        final float xz = quat.x * quat.z;
        final float xw = quat.x * quat.w;
        final float yy = quat.y * quat.y;
        final float yz = quat.y * quat.z;
        final float yw = quat.y * quat.w;
        final float zz = quat.z * quat.z;
        final float zw = quat.z * quat.w;

        TEMP_MATRIX.m00 = 1f - 2f * (yy + zz);
        TEMP_MATRIX.m10 = 2f * (xy - zw);
        TEMP_MATRIX.m20 = 2f * (xz + yw);
        TEMP_MATRIX.m30 = 0f;
        TEMP_MATRIX.m01 = 2f * (xy + zw);
        TEMP_MATRIX.m11 = 1f - 2f * (xx + zz);
        TEMP_MATRIX.m21 = 2f * (yz - xw);
        TEMP_MATRIX.m31 = 0f;
        TEMP_MATRIX.m02 = 2f * (xz - yw);
        TEMP_MATRIX.m12 = 2f * (yz + xw);
        TEMP_MATRIX.m22 = 1f - 2f * (xx + yy);
        TEMP_MATRIX.m32 = 0f;
        TEMP_MATRIX.m03 = 0f;
        TEMP_MATRIX.m13 = 0f;
        TEMP_MATRIX.m23 = 0f;
        TEMP_MATRIX.m33 = 1f;

        TEMP_MATRIX.transpose();

        return TEMP_MATRIX;
    }

    private static Quaternion lerp(Quaternion start, Quaternion end, float partialTicks) {
        Quaternion result = new Quaternion();
        final float d = start.x * end.x + start.y * end.y + start.z * end.z + start.w * end.w;
        float absDot = d < 0F ? -d : d;

        float scale0 = 1F - partialTicks;
        float scale1 = partialTicks;

        if ((1 - absDot) > 0.1) {
            final float angle = (float) Math.acos(absDot);
            final float invSinTheta = 1F / (float) Math.sin(angle);

            scale0 = ((float) Math.sin((1F - partialTicks) * angle) * invSinTheta);
            scale1 = ((float) Math.sin((partialTicks * angle)) * invSinTheta);
        }

        if (d < 0F) scale1 = -scale1;

        result.x = (scale0 * start.x) + (scale1 * end.x);
        result.y = (scale0 * start.y) + (scale1 * end.y);
        result.z = (scale0 * start.z) + (scale1 * end.z);
        result.w = (scale0 * start.w) + (scale1 * end.w);

        return result;
    }
}
