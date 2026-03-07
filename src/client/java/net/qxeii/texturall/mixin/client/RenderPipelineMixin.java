package net.qxeii.texturall.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(RenderPipeline.class)
public class RenderPipelineMixin {
    private static final Identifier TERRAIN_SHADER = Identifier.ofVanilla("core/terrain");

    @Inject(method = "getUniforms", at = @At("RETURN"), cancellable = true)
    private void texturall$appendSunDirectionUniform(CallbackInfoReturnable<List<RenderPipeline.UniformDescription>> cir) {
        RenderPipeline pipeline = (RenderPipeline) (Object) this;
        if (!pipeline.getFragmentShader().equals(TERRAIN_SHADER)) {
            return;
        }

        List<RenderPipeline.UniformDescription> uniforms = cir.getReturnValue();
        for (RenderPipeline.UniformDescription uniform : uniforms) {
            if (uniform.name().equals("SunDirectionInfo")) {
                return;
            }
        }

        List<RenderPipeline.UniformDescription> expanded = new ArrayList<>(uniforms);
        expanded.add(new RenderPipeline.UniformDescription("SunDirectionInfo", UniformType.UNIFORM_BUFFER));
        cir.setReturnValue(List.copyOf(expanded));
    }
}
