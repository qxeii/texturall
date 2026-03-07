package net.qxeii.texturall.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.qxeii.texturall.client.SunDirectionUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
