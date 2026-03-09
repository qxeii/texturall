package net.qxeii.texturall.mixin.client;

import net.fabricmc.fabric.impl.client.indigo.renderer.helper.ColorHelper;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.qxeii.texturall.client.texture.WorldAlignedBlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractTerrainRenderContext.class)
public abstract class AbstractTerrainRenderContextMixin {
    private static final int TEXTURALL_LIGHT_BYTES_MASK = 0x00FF00FF;
    private static final int TEXTURALL_EDGE_PAYLOAD_MASK = 0xFF00FF00;

    @Redirect(
        method = "shadeQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/helper/ColorHelper;maxLight(II)I"
        )
    )
    private int texturall$preserveNeighborPayload(
        int quadLightmap,
        int computedLightmap,
        MutableQuadViewImpl quad,
        boolean ao,
        boolean emissive,
        boolean vanillaShade
    ) {
        if (quad.tag() == WorldAlignedBlockStateModel.TEXTURALL_TERRAIN_QUAD_TAG) {
            return quadLightmap;
        }

        int mergedLightmap = ColorHelper.maxLight(quadLightmap, computedLightmap);
        if ((quadLightmap & TEXTURALL_EDGE_PAYLOAD_MASK) == 0) {
            return mergedLightmap;
        }

        // Texturall stores edge-material bytes in the otherwise-unused high byte of each lightmap half.
        // Indigo merges lightmaps after the quad is emitted, so restore those payload bytes after the merge.
        return (mergedLightmap & TEXTURALL_LIGHT_BYTES_MASK) | (quadLightmap & TEXTURALL_EDGE_PAYLOAD_MASK);
    }
}
