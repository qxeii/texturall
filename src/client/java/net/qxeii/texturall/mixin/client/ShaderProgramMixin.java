package net.qxeii.texturall.mixin.client;

import net.minecraft.client.gl.ShaderProgram;
import net.qxeii.texturall.client.SunDirectionUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderProgram.class)
public class ShaderProgramMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void texturall$onCreate(
            net.minecraft.client.gl.CompiledShader vertexShader,
            net.minecraft.client.gl.CompiledShader fragmentShader,
            com.mojang.blaze3d.vertex.VertexFormat vertexFormat,
            String name,
            CallbackInfoReturnable<ShaderProgram> cir) {

        // Don't rely on the debug label — just check whether our custom uniform
        // exists in this program. Only our overridden block shader will have it.
        ShaderProgram program = cir.getReturnValue();
        if (program != null) {
            SunDirectionUniform.tryCapture(program);
        }
    }
}
