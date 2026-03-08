package net.qxeii.texturall.client.texture;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.qxeii.texturall.mixin.client.RenderPipelineAccessor;

import java.util.ArrayList;
import java.util.List;

public final class TexturallTerrainPipelineSamplers {
    private static final String MERGE_SAMPLER = "Sampler1";
    private static boolean extended;

    private TexturallTerrainPipelineSamplers() {
    }

    public static void extendTerrainSamplers() {
        if (extended) {
            return;
        }

        extended = true;
        extend(RenderPipelines.SOLID_TERRAIN);
        extend(RenderPipelines.CUTOUT_TERRAIN);
        extend(RenderPipelines.TRIPWIRE_TERRAIN);
    }

    private static void extend(RenderPipeline pipeline) {
        RenderPipelineAccessor accessor = (RenderPipelineAccessor) pipeline;
        List<String> samplers = accessor.texturall$getSamplers();
        if (samplers.contains(MERGE_SAMPLER)) {
            return;
        }

        List<String> updated = new ArrayList<>(samplers);
        updated.add(MERGE_SAMPLER);
        accessor.texturall$setSamplers(List.copyOf(updated));
    }
}
