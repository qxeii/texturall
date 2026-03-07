package net.qxeii.texturall.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderPass;
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

    @Inject(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setIndexBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V"
        )
    )
    private void texturall$bindSunDirection(BuiltBuffer buffer, CallbackInfo ci, @Local RenderPass renderPass) {
        RenderLayer self = (RenderLayer) (Object) this;
        if (self.getRenderPipeline().getFragmentShader().equals(TERRAIN_SHADER)) {
            renderPass.setUniform("SunDirectionInfo", SunDirectionUniform.slice());
        }
    }
}
