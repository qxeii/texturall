package net.qxeii.texturall.client.texture;

import net.minecraft.block.Block;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public record WorldAlignedTextureMaterial(
    Block block,
    Identifier tileTextureId,
    Identifier tileSpriteId,
    Identifier sheetTextureResourceId,
    Identifier sheetSpriteId,
    Identifier normalTextureResourceId,
    Identifier normalSpriteId,
    int materialIndex,
    long seed,
    MaterialNoiseSettings noiseSettings,
    int[] palette,
    int sheetSize
) {
    public SpriteIdentifier tileSprite() {
        return new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, tileSpriteId);
    }

    public SpriteIdentifier normalSprite() {
        return new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, normalSpriteId);
    }
}
