package net.qxeii.texturall.mixin.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import net.qxeii.texturall.client.SunDirectionUniform;
import net.qxeii.texturall.client.texture.TexturallTextureOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin {
    private static final Identifier TERRAIN_SHADER = Identifier.ofVanilla("core/terrain");

    @Inject(method = "draw", at = @At("HEAD"))
    private void texturall$bindSunDirection(BuiltBuffer buffer, CallbackInfo ci) {
        RenderLayer self = (RenderLayer) (Object) this;
        if (self.getRenderPipeline().getFragmentShader().equals(TERRAIN_SHADER)) {
            GpuBufferSlice sunDirection = SunDirectionUniform.upload();
            if (sunDirection != null) {
                RenderSystem.setShaderLights(sunDirection);
            }
        }
    }

    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setIndexBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V"
        )
    )
    private void texturall$bindNormalAtlas(
        RenderPass renderPass,
        GpuBuffer indexBuffer,
        VertexFormat.IndexType indexType,
        BuiltBuffer buffer
    ) {
        RenderLayer self = (RenderLayer) (Object) this;
        if (self.getRenderPipeline().getFragmentShader().equals(TERRAIN_SHADER)) {
            AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(TexturallTextureOverrides.normalAtlasTexture());
            if (texture.getGlTextureView() != null && texture.getSampler() != null) {
                renderPass.bindTexture("Sampler1", texture.getGlTextureView(), texture.getSampler());
            }
        }
        renderPass.setIndexBuffer(indexBuffer, indexType);
    }
}
