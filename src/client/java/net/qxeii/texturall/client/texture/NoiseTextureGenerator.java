package net.qxeii.texturall.client.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class NoiseTextureGenerator implements ProceduralTextureGenerator {
    private final int size;
    private final int[] palette;
    private final SeamlessNoiseField noiseField;

    public NoiseTextureGenerator(int size, long seed, double scale, int[] palette) {
        this.size = size;
        this.palette = palette;
        this.noiseField = new SeamlessNoiseField(size, seed, scale);
    }

    @Override
    public byte[] generatePng() {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double h = sample(x, y);
                image.setRGB(x, y, samplePalette(h));
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode generated texture", exception);
        }
    }

    private int samplePalette(double value) {
        double scaled = value * (palette.length - 1);
        int lowerIndex = (int) Math.floor(scaled);
        int upperIndex = Math.min(palette.length - 1, lowerIndex + 1);
        double t = scaled - lowerIndex;
        int lower = palette[lowerIndex];
        int upper = palette[upperIndex];
        int red = lerp((lower >>> 16) & 0xFF, (upper >>> 16) & 0xFF, t);
        int green = lerp((lower >>> 8) & 0xFF, (upper >>> 8) & 0xFF, t);
        int blue = lerp(lower & 0xFF, upper & 0xFF, t);
        int alpha = 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public double sample(int pixelX, int pixelY) {
        return noiseField.sample(pixelX, pixelY);
    }

    private static int lerp(int start, int end, double t) {
        return (int) Math.round(start + (end - start) * t);
    }
}
