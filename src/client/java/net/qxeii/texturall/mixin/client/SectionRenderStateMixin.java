package net.qxeii.texturall.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.BlockRenderLayerGroup;
import net.minecraft.client.render.SectionRenderState;
import net.qxeii.texturall.client.SunDirectionUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderState.class)
public abstract class SectionRenderStateMixin {
    @Inject(method = "renderSection", at = @At("HEAD"))
    private void texturall$bindTerrainLighting(BlockRenderLayerGroup layerGroup, GpuSampler sampler, CallbackInfo ci) {
        GpuBufferSlice sunDirection = SunDirectionUniform.upload();
        if (sunDirection != null) {
            RenderSystem.setShaderLights(sunDirection);
        }
    }
}
