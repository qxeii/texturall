package net.qxeii.texturall.client.texture;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TexturallTextureOverrides {
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

        registerVanillaBlock(Blocks.STONE, "stone", "stone", 0x51A2D39CB53F7E1DL, 4.25, "stone");
        registerVanillaBlock(Blocks.GRANITE, "granite", "granite", 0xA9B5F2C43E8D16F1L, 4.0, "granite");
        registerVanillaBlock(Blocks.DIORITE, "diorite", "diorite", 0xC74E19A25D30B86FL, 4.6, "diorite");
        registerVanillaBlock(Blocks.ANDESITE, "andesite", "andesite", 0x67BF6E0C11E2B7A4L, 4.5, "andesite");
        registerVanillaBlock(Blocks.DEEPSLATE, "deepslate", "deepslate", 0x2D4A57A9B13ED0F1L, 3.75, "deepslate", "deepslate_top");
        registerVanillaBlock(Blocks.TUFF, "tuff", "tuff", 0x4B0F92AD73CE1183L, 4.0, "tuff");
        registerVanillaBlock(Blocks.CALCITE, "calcite", "calcite", 0x9D73E1045CB2AF67L, 5.1, "calcite");
        registerVanillaBlock(Blocks.DRIPSTONE_BLOCK, "dripstone_block", "dripstone_block", 0x5D1F94C83E24A771L, 4.2, "dripstone_block");
        registerVanillaBlock(Blocks.BASALT, "basalt", "basalt_side", 0xE3A1C6704FB2DD19L, 3.8, "basalt_side", "basalt_top");
        registerVanillaBlock(Blocks.SMOOTH_BASALT, "smooth_basalt", "smooth_basalt", 0x13D4AF5E78C920B3L, 3.6, "smooth_basalt");
        registerVanillaBlock(Blocks.BLACKSTONE, "blackstone", "blackstone", 0x7EB19A0D54F28361L, 3.9, "blackstone", "blackstone_top");

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
        double scale,
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
        int[] palette = VanillaTexturePaletteExtractor.derivePalette(5, paletteTextureIds);

        ProceduralTextureRegistry.register(
            tileId,
            new NoiseTextureGenerator(16, seed, scale, palette)
        );
        ProceduralTextureRegistry.register(sheetResourceId, new NoiseTextureGenerator(256, seed, scale, palette));
        ProceduralTextureRegistry.register(normalTextureResourceId, new NormalTextureGenerator(256, seed, scale));
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
            scale,
            palette,
            16
        ));
    }
}
