package net.qxeii.texturall.client.texture;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.MissingItemModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.ModelSettings;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.qxeii.texturall.mixin.client.MissingItemModelAccessor;

import java.util.ArrayList;
import java.util.List;

public final class TexturallModelLoadingPlugin {
    private static boolean registered;

    private TexturallModelLoadingPlugin() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyBlockModelAfterBake().register(
                ModelModifier.WRAP_LAST_PHASE,
                (BlockStateModel model, net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBakeBlock.Context context) -> {
                    WorldAlignedTextureMaterial material = TexturallTextureOverrides.materialFor(context.state().getBlock());
                    if (material == null) {
                        return model;
                    }

                    return new WorldAlignedBlockStateModel(model, material);
                }
            );
            pluginContext.modifyItemModelAfterBake().register(
                ModelModifier.WRAP_LAST_PHASE,
                (ItemModel model, net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBakeItem.Context context) -> {
                    Item item = Registries.ITEM.get(context.itemId());
                    if (!(item instanceof BlockItem blockItem)) {
                        return model;
                    }

                    WorldAlignedTextureMaterial material = TexturallTextureOverrides.materialFor(blockItem.getBlock());
                    if (material == null || !(model instanceof MissingItemModel missingItemModel)) {
                        return model;
                    }

                    return retextureBlockItemModel(missingItemModel, material, context);
                }
            );
        });
    }

    private static ItemModel retextureBlockItemModel(
        MissingItemModel model,
        WorldAlignedTextureMaterial material,
        net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBakeItem.Context context
    ) {
        Sprite tileSprite = context.bakeContext().spriteHolder().getSprite(material.tileSprite());
        MissingItemModelAccessor accessor = (MissingItemModelAccessor) model;
        List<BakedQuad> originalQuads = accessor.texturall$getQuads();
        ModelSettings originalSettings = accessor.texturall$getSettings();

        boolean needsRetexture = originalSettings.particleIcon() != tileSprite;
        if (!needsRetexture) {
            for (BakedQuad quad : originalQuads) {
                if (quad.sprite() != tileSprite) {
                    needsRetexture = true;
                    break;
                }
            }
        }

        if (!needsRetexture) {
            return model;
        }

        List<BakedQuad> retexturedQuads = new ArrayList<>(originalQuads.size());
        for (BakedQuad quad : originalQuads) {
            retexturedQuads.add(new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.tintIndex(),
                quad.face(),
                tileSprite,
                quad.shade(),
                quad.lightEmission()
            ));
        }

        ModelSettings retexturedSettings = new ModelSettings(
            originalSettings.usesBlockLight(),
            tileSprite,
            originalSettings.transforms()
        );
        return new MissingItemModel(retexturedQuads, retexturedSettings);
    }
}
