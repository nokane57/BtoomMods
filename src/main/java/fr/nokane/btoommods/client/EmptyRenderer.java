// fr/nokane/btoommods/client/EmptyRenderer.java
package fr.nokane.btoommods.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;

public class EmptyRenderer<T extends Entity> extends EntityRenderer<T> {
    public EmptyRenderer(EntityRendererManager ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }
    @Override public void render(T e, float yaw, float partialTicks, MatrixStack stack, IRenderTypeBuffer buf, int light) {
        // rien : l’entité affiche uniquement des particules côté client
    }
    @Override public ResourceLocation getTextureLocation(T e) {
        return AtlasTexture.LOCATION_BLOCKS; // jamais null
    }
}
