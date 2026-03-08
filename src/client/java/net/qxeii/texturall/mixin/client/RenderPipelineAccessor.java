package net.qxeii.texturall.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(RenderPipeline.class)
public interface RenderPipelineAccessor {
    @Accessor("samplers")
    List<String> texturall$getSamplers();

    @Mutable
    @Accessor("samplers")
    void texturall$setSamplers(List<String> samplers);
}
