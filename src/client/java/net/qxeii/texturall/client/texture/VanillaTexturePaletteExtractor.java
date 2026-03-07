package net.qxeii.texturall.client.texture;

import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VanillaTexturePaletteExtractor {
    private VanillaTexturePaletteExtractor() {
    }

    public static int[] derivePalette(int steps, Identifier... textureIds) {
        if (steps < 2) {
            throw new IllegalArgumentException("Palette steps must be at least 2");
        }
        if (textureIds.length == 0) {
            throw new IllegalArgumentException("At least one vanilla texture is required");
        }

        List<Integer> colors = new ArrayList<>();
        for (Identifier textureId : textureIds) {
            try (InputStream stream = openVanillaTexture(textureId)) {
                if (stream == null) {
                    throw new IllegalStateException("Missing vanilla texture " + textureId);
                }

                BufferedImage image = ImageIO.read(stream);
                if (image == null) {
                    throw new IllegalStateException("Failed to decode vanilla texture " + textureId);
                }

                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = image.getRGB(x, y);
                        if (((argb >>> 24) & 0xFF) == 0) {
                            continue;
                        }
                        colors.add(argb & 0xFFFFFF);
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to read vanilla texture " + textureId, exception);
            }
        }

        if (colors.isEmpty()) {
            throw new IllegalStateException("Vanilla textures contained no opaque pixels");
        }

        colors.sort(Comparator.comparingDouble(VanillaTexturePaletteExtractor::luminance));

        int[] palette = new int[steps];
        for (int i = 0; i < steps; i++) {
            int index = Math.round((colors.size() - 1) * (float) i / (steps - 1));
            palette[i] = colors.get(index);
        }
        return palette;
    }

    private static InputStream openVanillaTexture(Identifier textureId) {
        String path = "assets/" + textureId.getNamespace() + "/" + textureId.getPath();
        return VanillaTexturePaletteExtractor.class.getClassLoader().getResourceAsStream(path);
    }

    private static double luminance(int rgb) {
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
}
