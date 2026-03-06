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
    private final SimplexNoise noise;
    private final SimplexNoise mediumNoise;
    private final SimplexNoise fineNoise;
    private final SimplexNoise warpNoiseX;
    private final SimplexNoise warpNoiseY;

    public NoiseTextureGenerator(int size, long seed, double scale, int[][] palette) {
        this.size = size;
        this.seed = seed;
        this.scale = scale;
        this.palette = palette;
        this.noise = new SimplexNoise(seed);
        this.mediumNoise = new SimplexNoise(seed ^ 0x9E3779B97F4A7C15L);
        this.fineNoise = new SimplexNoise(seed ^ 0xC6BC279692B5C323L);
        this.warpNoiseX = new SimplexNoise(seed ^ 0x517CC1B727220A95L);
        this.warpNoiseY = new SimplexNoise(seed ^ 0xBF58476D1CE4E5B9L);
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
        double x = pixelX / scale;
        double y = pixelY / scale;
        double wx = warpNoiseX.sample(x, y) * 2.0f;
        double wy = warpNoiseY.sample(x, y) * 2.0f;
        double shape = noise.sample((x + wx) * 0.1, (y + wy) * 0.2) * 0.5 + 0.5;
        return shape;
    }

    private static int lerp(int start, int end, double alpha) {
        return (int) Math.round(start + (end - start) * alpha);
    }
}
