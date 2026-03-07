package net.qxeii.texturall.client.texture;

import net.minecraft.block.Block;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public record WorldAlignedTextureMaterial(
    Block block,
    Identifier tileTextureId,
    Identifier sheetTextureResourceId,
    Identifier sheetSpriteId,
    Identifier normalTextureResourceId,
    Identifier normalSpriteId,
    int materialIndex,
    long seed,
    double scale,
    int[][] palette,
    int[] minDirectionalColor,
    int[] maxDirectionalColor,
    int sheetSize
) {
    public SpriteIdentifier spriteId() {
        return new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, normalSpriteId);
    }
}
