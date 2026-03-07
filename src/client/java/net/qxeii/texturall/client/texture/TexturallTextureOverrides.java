package net.qxeii.texturall.client.texture;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TexturallTextureOverrides {
    private static final int PALETTE_STEPS = 16;
    private static boolean bootstrapped;
    private static final Identifier MATERIAL_SHADER_INCLUDE_ID = Identifier.ofVanilla("shaders/include/texturall_materials.glsl");
    private static final Map<Block, WorldAlignedTextureMaterial> MATERIALS = new LinkedHashMap<>();
    private static final Map<Integer, Identifier> NORMAL_TEXTURES = new LinkedHashMap<>();
    private static int nextMaterialIndex = 1;

    private TexturallTextureOverrides() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        registerVanillaBlock(Blocks.STONE, "stone", "stone", 0x51A2D39CB53F7E1DL, noise(4.25, 1.0, 5.0, 1.0), "stone");
        registerVanillaBlock(Blocks.COBBLESTONE, "cobblestone", "cobblestone", 0xD9278A61B43C1FF2L, noise(0.45, 1.0, 1.0, 1.0), "cobblestone");
        registerVanillaBlock(Blocks.MOSSY_COBBLESTONE, "mossy_cobblestone", "mossy_cobblestone", 0x82FAE19D03C44D71L, noise(0.45, 1.0, 1.0, 1.0), "mossy_cobblestone");
        registerVanillaBlock(Blocks.GRAVEL, "gravel", "gravel", 0x3CC8B210DD5AE5F4L, noise(2.5, 1.0, 1.0, 2.0), "gravel");
        registerVanillaBlock(Blocks.GRANITE, "granite", "granite", 0xA9B5F2C43E8D16F1L, noise(4.0, 1.05, 0.95, 0.8), "granite");
        registerVanillaBlock(Blocks.DIORITE, "diorite", "diorite", 0xC74E19A25D30B86FL, noise(4.6, 1.0, 1.0, 0.45), "diorite");
        registerVanillaBlock(Blocks.ANDESITE, "andesite", "andesite", 0x67BF6E0C11E2B7A4L, noise(4.5, 1.0, 1.0, 0.85), "andesite");
        registerVanillaBlock(Blocks.DEEPSLATE, "deepslate", "deepslate", 0x2D4A57A9B13ED0F1L, noise(4.5, 2.5, 1.0, 2.0), "deepslate", "deepslate_top");
        registerVanillaBlock(Blocks.COBBLED_DEEPSLATE, "cobbled_deepslate", "cobbled_deepslate", 0xF4DA07E26C1917A2L, noise(4.0, 0.9, 1.2, 1.2), "cobbled_deepslate");
        registerVanillaBlock(Blocks.TUFF, "tuff", "tuff", 0x4B0F92AD73CE1183L, noise(4.0, 1.0, 1.0, 0.75), "tuff");
        registerVanillaBlock(Blocks.CALCITE, "calcite", "calcite", 0x9D73E1045CB2AF67L, noise(5.1, 1.0, 1.0, 0.25), "calcite");
        registerVanillaBlock(Blocks.DRIPSTONE_BLOCK, "dripstone_block", "dripstone_block", 0x5D1F94C83E24A771L, noise(4.2, 0.9, 1.2, 1.05), "dripstone_block");
        registerVanillaBlock(Blocks.BASALT, "basalt", "basalt_side", 0xE3A1C6704FB2DD19L, noise(4.8, 0.55, 1.7, 0.55), "basalt_side", "basalt_top");
        registerVanillaBlock(Blocks.SMOOTH_BASALT, "smooth_basalt", "smooth_basalt", 0x13D4AF5E78C920B3L, noise(5.0, 0.6, 1.55, 0.3), "smooth_basalt");
        registerVanillaBlock(Blocks.BLACKSTONE, "blackstone", "blackstone", 0x7EB19A0D54F28361L, noise(3.9, 0.95, 1.05, 1.15), "blackstone", "blackstone_top");

        ProceduralTextureRegistry.register(MATERIAL_SHADER_INCLUDE_ID, new TexturallMaterialShaderGenerator(MATERIALS.values()));
    }

    public static WorldAlignedTextureMaterial materialFor(Block block) {
        return MATERIALS.get(block);
    }

    public static Identifier normalTextureForIndex(int materialIndex) {
        return NORMAL_TEXTURES.get(materialIndex);
    }

    private static void registerVanillaBlock(
        Block block,
        String name,
        String spriteName,
        long seed,
        MaterialNoiseSettings noiseSettings,
        String... paletteTextureNames
    ) {
        Identifier tileId = Identifier.ofVanilla("textures/block/" + spriteName + ".png");
        Identifier tileSpriteId = Identifier.ofVanilla("block/" + spriteName);
        Identifier sheetResourceId = Identifier.ofVanilla("textures/block/world/" + name + "_sheet.png");
        Identifier sheetSpriteId = Identifier.ofVanilla("block/world/" + name + "_sheet");
        Identifier normalTextureResourceId = Identifier.ofVanilla("textures/block/world/" + name + "_normal.png");
        Identifier normalSpriteId = Identifier.ofVanilla("block/world/" + name + "_normal");
        int materialIndex = nextMaterialIndex++;
        Identifier[] paletteTextureIds = new Identifier[paletteTextureNames.length];
        for (int i = 0; i < paletteTextureNames.length; i++) {
            paletteTextureIds[i] = Identifier.ofVanilla("textures/block/" + paletteTextureNames[i] + ".png");
        }
        int[] palette = VanillaTexturePaletteExtractor.derivePalette(PALETTE_STEPS, paletteTextureIds);

        ProceduralTextureRegistry.register(
            tileId,
            new NoiseTextureGenerator(16, seed, noiseSettings, palette)
        );
        ProceduralTextureRegistry.register(sheetResourceId, new NoiseTextureGenerator(256, seed, noiseSettings, palette));
        ProceduralTextureRegistry.register(normalTextureResourceId, new NormalTextureGenerator(256, seed, noiseSettings));
        NORMAL_TEXTURES.put(materialIndex, normalTextureResourceId);

        MATERIALS.put(block, new WorldAlignedTextureMaterial(
            block,
            tileId,
            tileSpriteId,
            sheetResourceId,
            sheetSpriteId,
            normalTextureResourceId,
            normalSpriteId,
            materialIndex,
            seed,
            noiseSettings,
            palette,
            16
        ));
    }

    private static MaterialNoiseSettings noise(double scale, double squashX, double squashY, double crackDensity) {
        return new MaterialNoiseSettings(scale, squashX, squashY, crackDensity);
    }
}
