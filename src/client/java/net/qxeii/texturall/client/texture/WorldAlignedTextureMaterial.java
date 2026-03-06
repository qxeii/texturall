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
    long seed,
    double scale,
    int[][] palette,
    int sheetSize
) {
    public SpriteIdentifier spriteId() {
        return new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, sheetSpriteId);
    }
}
