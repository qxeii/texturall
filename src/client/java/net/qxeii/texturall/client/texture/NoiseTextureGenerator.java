package net.qxeii.texturall.client.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class NoiseTextureGenerator implements ProceduralTextureGenerator {
    private final int size;
    private final long seed;
    private final double scale;
    private final int[][] palette;
    private final SimplexNoise largeNoise;
    private final SimplexNoise mediumNoise;
    private final SimplexNoise fineNoise;

    public NoiseTextureGenerator(int size, long seed, double scale, int[][] palette) {
        this.size = size;
        this.seed = seed;
        this.scale = scale;
        this.palette = palette;
        this.largeNoise = new SimplexNoise(seed);
        this.mediumNoise = new SimplexNoise(seed ^ 0x9E3779B97F4A7C15L);
        this.fineNoise = new SimplexNoise(seed ^ 0xC6BC279692B5C323L);
    }

    @Override
    public byte[] generatePng() {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double value = sample(x, y);
                image.setRGB(x, y, samplePalette(value));
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
        double alpha = scaled - lowerIndex;
        int[] lower = palette[lowerIndex];
        int[] upper = palette[upperIndex];
        int red = lerp(lower[0], upper[0], alpha);
        int green = lerp(lower[1], upper[1], alpha);
        int blue = lerp(lower[2], upper[2], alpha);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public double sample(int pixelX, int pixelY) {
        double nx = pixelX / scale;
        double ny = pixelY / scale;
        double shape = remap(largeNoise.sample(nx, ny), -1.0, 1.0, 0.0, 1.0);
        double strata = remap(mediumNoise.sample(nx * 2.5, ny * 0.8), -1.0, 1.0, 0.0, 1.0);
        double grain = remap(fineNoise.sample(nx * 6.0, ny * 6.0), -1.0, 1.0, 0.0, 1.0);
        return clamp(shape * 0.55 + strata * 0.3 + grain * 0.15);
    }

    private static int lerp(int start, int end, double alpha) {
        return (int) Math.round(start + (end - start) * alpha);
    }

    private static double remap(double value, double inMin, double inMax, double outMin, double outMax) {
        double normalized = (value - inMin) / (inMax - inMin);
        return outMin + normalized * (outMax - outMin);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
