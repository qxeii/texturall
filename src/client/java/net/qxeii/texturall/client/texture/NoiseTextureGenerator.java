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
    private final SimplexNoise warpNoiseX;
    private final SimplexNoise warpNoiseY;

    public NoiseTextureGenerator(int size, long seed, double scale, int[][] palette) {
        this.size = size;
        this.seed = seed;
        this.scale = scale;
        this.palette = palette;
        this.noise = new SimplexNoise(seed);
        this.warpNoiseX = new SimplexNoise(seed ^ 0x517CC1B727220A95L);
        this.warpNoiseY = new SimplexNoise(seed ^ 0xBF58476D1CE4E5B9L);
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
        int[] lower = palette[lowerIndex];
        int[] upper = palette[upperIndex];
        int red   = lerp(lower[0], upper[0], t);
        int green = lerp(lower[1], upper[1], t);
        int blue  = lerp(lower[2], upper[2], t);
        int alpha = 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public double sample(int pixelX, int pixelY) {
        double x = pixelX / scale;
        double y = pixelY / scale;
        double wx = warpNoiseX.sample(x, y) * 4.0;
        double wy = warpNoiseY.sample(x, y) * 4.0;
        return noise.sample((x + wx) * 0.01, (y + wy) * 0.05) * 0.5 + 0.5;
    }

    private static int lerp(int start, int end, double t) {
        return (int) Math.round(start + (end - start) * t);
    }
}
