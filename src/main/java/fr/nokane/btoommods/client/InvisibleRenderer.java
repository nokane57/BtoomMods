package fr.nokane.btoommods.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class InvisibleRenderer<T extends Entity> extends EntityRenderer<T> {
    public InvisibleRenderer(EntityRendererManager m) { super(m); }
    @Override public ResourceLocation getTextureLocation(T e) { return AtlasTexture.LOCATION_BLOCKS; }
    @Override public void render(T e, float yaw, float pt, MatrixStack ms, IRenderTypeBuffer buf, int light) { /* no-op */ }
}
