package net.qxeii.texturall.client.texture;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class TexturallTextureOverrides {
    private static boolean bootstrapped;
    private static final Map<Block, WorldAlignedTextureMaterial> MATERIALS = new HashMap<>();

    private TexturallTextureOverrides() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        registerVanillaBlock(Blocks.STONE, "stone", 0x51A2D39CB53F7E1DL, 4.25, new int[][]{
            {56, 59, 63},
            {74, 77, 82},
            {96, 101, 106},
            {116, 121, 127},
            {140, 145, 151}
        });
        registerVanillaBlock(Blocks.DEEPSLATE, "deepslate", 0x2D4A57A9B13ED0F1L, 3.75, new int[][]{
            {42, 44, 48},
            {56, 59, 64},
            {71, 75, 80},
            {88, 92, 98},
            {108, 112, 119}
        });
        registerVanillaBlock(Blocks.ANDESITE, "andesite", 0x67BF6E0C11E2B7A4L, 4.5, new int[][]{
            {84, 84, 86},
            {106, 106, 108},
            {126, 126, 128},
            {149, 149, 151},
            {171, 171, 173}
        });
        registerVanillaBlock(Blocks.TUFF, "tuff", 0x4B0F92AD73CE1183L, 4.0, new int[][]{
            {79, 84, 76},
            {98, 104, 93},
            {117, 124, 111},
            {137, 144, 129},
            {160, 167, 151}
        });
    }

    public static WorldAlignedTextureMaterial materialFor(Block block) {
        return MATERIALS.get(block);
    }

    private static void registerVanillaBlock(Block block, String name, long seed, double scale, int[][] palette) {
        Identifier tileId = Identifier.ofVanilla("textures/block/" + name + ".png");
        Identifier sheetResourceId = Identifier.ofVanilla("textures/block/world/" + name + "_sheet.png");
        Identifier sheetSpriteId = Identifier.ofVanilla("block/world/" + name + "_sheet");

        ProceduralTextureRegistry.register(
            tileId,
            new NoiseTextureGenerator(16, seed, scale, palette)
        );
        ProceduralTextureRegistry.register(sheetResourceId, new NoiseTextureGenerator(256, seed, scale, palette));

        MATERIALS.put(block, new WorldAlignedTextureMaterial(
            block,
            tileId,
            sheetResourceId,
            sheetSpriteId,
            seed,
            scale,
            palette,
            16
        ));
    }
}
