package net.qxeii.texturall.client.texture;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class TexturallNormalAtlasGenerator implements ProceduralTextureGenerator {
    public static final int SHEET_SIZE = 256;

    private final int gridSize;
    private final List<WorldAlignedTextureMaterial> materials;

    public TexturallNormalAtlasGenerator(int gridSize, Iterable<WorldAlignedTextureMaterial> materials) {
        this.gridSize = gridSize;
        this.materials = new ArrayList<>();
        for (WorldAlignedTextureMaterial material : materials) {
            this.materials.add(material);
        }
    }

    @Override
    public byte[] generatePng() {
        BufferedImage atlas = new BufferedImage(gridSize * SHEET_SIZE, gridSize * SHEET_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        try {
            for (WorldAlignedTextureMaterial material : materials) {
                int slot = material.materialIndex() - 1;
                if (slot < 0 || slot >= gridSize * gridSize) {
                    throw new IllegalStateException("Material index " + material.materialIndex() + " does not fit in the normal atlas");
                }
                int atlasX = (slot % gridSize) * SHEET_SIZE;
                int atlasY = (slot / gridSize) * SHEET_SIZE;
                BufferedImage tile = new NormalTextureGenerator(SHEET_SIZE, material.seed(), material.noiseSettings()).generateImage();
                graphics.drawImage(tile, atlasX, atlasY, null);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(atlas, "PNG", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode Texturall normal atlas", exception);
        }
    }
}
