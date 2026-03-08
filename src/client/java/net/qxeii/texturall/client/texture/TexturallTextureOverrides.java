package net.qxeii.texturall.client.texture;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TexturallTextureOverrides {
    private static final int PALETTE_STEPS = 16;
    private static final int MAX_EDGE_MATERIAL_ID = 63;
    private static final int NORMAL_ATLAS_GRID_SIZE = 8;
    private static boolean bootstrapped;
    private static final Identifier MATERIAL_SHADER_INCLUDE_ID = Identifier.ofVanilla("shaders/include/texturall_materials.glsl");
    private static final Identifier MATERIAL_NORMAL_ATLAS_ID = Identifier.of("texturall", "textures/material_normal_atlas.png");
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

        MaterialNoiseSettings stoneOreNoise = noise(4.5, 1.0, 5.0, 1.15);
        MaterialNoiseSettings deepslateOreNoise = noise(4.9, 1.7, 0.95, 1.35);
        MaterialNoiseSettings netherOreNoise = noise(3.3, 1.0, 1.0, 1.45);
        MaterialNoiseSettings ancientDebrisNoise = noise(4.8, 1.35, 0.9, 0.65);

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
        registerVanillaBlock(Blocks.COAL_ORE, "coal_ore", "coal_ore", 0x8D7324C8A1E40F55L, stoneOreNoise, "coal_ore");
        registerVanillaBlock(Blocks.IRON_ORE, "iron_ore", "iron_ore", 0x3E7BC2D15A48E90FL, stoneOreNoise, "iron_ore");
        registerVanillaBlock(Blocks.COPPER_ORE, "copper_ore", "copper_ore", 0x54AE3FC9186DB772L, stoneOreNoise, "copper_ore");
        registerVanillaBlock(Blocks.GOLD_ORE, "gold_ore", "gold_ore", 0x1FA09B4EC3D87261L, stoneOreNoise, "gold_ore");
        registerVanillaBlock(Blocks.REDSTONE_ORE, "redstone_ore", "redstone_ore", 0xC4B27E1950F384A2L, stoneOreNoise, "redstone_ore");
        registerVanillaBlock(Blocks.EMERALD_ORE, "emerald_ore", "emerald_ore", 0x6DE439A17C82B54FL, stoneOreNoise, "emerald_ore");
        registerVanillaBlock(Blocks.LAPIS_ORE, "lapis_ore", "lapis_ore", 0xA2F8C54D31E7068BL, stoneOreNoise, "lapis_ore");
        registerVanillaBlock(Blocks.DIAMOND_ORE, "diamond_ore", "diamond_ore", 0x72D184B30FAC695EL, stoneOreNoise, "diamond_ore");
        registerVanillaBlock(Blocks.DEEPSLATE_COAL_ORE, "deepslate_coal_ore", "deepslate_coal_ore", 0x847E235AB1D60C39L, deepslateOreNoise, "deepslate_coal_ore");
        registerVanillaBlock(Blocks.DEEPSLATE_IRON_ORE, "deepslate_iron_ore", "deepslate_iron_ore", 0x5CBF1234AE6192D7L, deepslateOreNoise, "deepslate_iron_ore");
        registerVanillaBlock(Blocks.DEEPSLATE_COPPER_ORE, "deepslate_copper_ore", "deepslate_copper_ore", 0xAF9431C87D5E20B4L, deepslateOreNoise, "deepslate_copper_ore");
        registerVanillaBlock(Blocks.DEEPSLATE_GOLD_ORE, "deepslate_gold_ore", "deepslate_gold_ore", 0x39A6E7C20D51F48BL, deepslateOreNoise, "deepslate_gold_ore");
        registerVanillaBlock(Blocks.DEEPSLATE_REDSTONE_ORE, "deepslate_redstone_ore", "deepslate_redstone_ore", 0xE1865B0FA43DC279L, deepslateOreNoise, "deepslate_redstone_ore");
        registerVanillaBlock(Blocks.DEEPSLATE_EMERALD_ORE, "deepslate_emerald_ore", "deepslate_emerald_ore", 0xB7D41293F0E46C28L, deepslateOreNoise, "deepslate_emerald_ore");
        registerVanillaBlock(Blocks.DEEPSLATE_LAPIS_ORE, "deepslate_lapis_ore", "deepslate_lapis_ore", 0x24CE98A17BD35FE1L, deepslateOreNoise, "deepslate_lapis_ore");
        registerVanillaBlock(Blocks.DEEPSLATE_DIAMOND_ORE, "deepslate_diamond_ore", "deepslate_diamond_ore", 0xD9AF3572B41C80E6L, deepslateOreNoise, "deepslate_diamond_ore");
        registerVanillaBlock(Blocks.NETHER_QUARTZ_ORE, "nether_quartz_ore", "nether_quartz_ore", 0x2E4B7D18C0F36A95L, netherOreNoise, "nether_quartz_ore");
        registerVanillaBlock(Blocks.NETHER_GOLD_ORE, "nether_gold_ore", "nether_gold_ore", 0x91CE26B4A5D83074L, netherOreNoise, "nether_gold_ore");
        registerVanillaBlock(Blocks.ANCIENT_DEBRIS, "ancient_debris", "ancient_debris_side", 0xFA1048C26E93B57DL, ancientDebrisNoise, "ancient_debris_side", "ancient_debris_top");
        registerVanillaBlock(Blocks.GILDED_BLACKSTONE, "gilded_blackstone", "gilded_blackstone", 0x47B0E29D5AF316C8L, noise(3.8, 0.95, 1.05, 1.05), "gilded_blackstone");

        ProceduralTextureRegistry.register(MATERIAL_SHADER_INCLUDE_ID, new TexturallMaterialShaderGenerator(MATERIALS.values()));
        ProceduralTextureRegistry.register(MATERIAL_NORMAL_ATLAS_ID, new TexturallNormalAtlasGenerator(NORMAL_ATLAS_GRID_SIZE, MATERIALS.values()));
    }

    public static WorldAlignedTextureMaterial materialFor(Block block) {
        return MATERIALS.get(block);
    }

    public static Identifier normalTextureForIndex(int materialIndex) {
        return NORMAL_TEXTURES.get(materialIndex);
    }

    public static Identifier normalAtlasTexture() {
        return MATERIAL_NORMAL_ATLAS_ID;
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
        if (materialIndex > MAX_EDGE_MATERIAL_ID) {
            throw new IllegalStateException("Texturall edge blending supports at most " + MAX_EDGE_MATERIAL_ID + " materials");
        }
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
