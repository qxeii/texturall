package net.qxeii.texturall.client.texture;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.render.model.BlockStateModel;

public final class TexturallModelLoadingPlugin {
    private static boolean registered;

    private TexturallModelLoadingPlugin() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ModelLoadingPlugin.register(pluginContext -> pluginContext.modifyBlockModelAfterBake().register(
            ModelModifier.WRAP_LAST_PHASE,
            (BlockStateModel model, net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBakeBlock.Context context) -> {
                WorldAlignedTextureMaterial material = TexturallTextureOverrides.materialFor(context.state().getBlock());
                if (material == null) {
                    return model;
                }

                return new WorldAlignedBlockStateModel(model, material);
            }
        ));
    }
}
