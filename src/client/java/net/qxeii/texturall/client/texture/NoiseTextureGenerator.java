package net.qxeii.texturall.client.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class NoiseTextureGenerator implements ProceduralTextureGenerator {
    private static final double CONTRAST = 2.35;
    private static final double CRACK_DARKENING = 0.28;

    private final int size;
    private final int[] palette;
    private final SeamlessNoiseField noiseField;

    public NoiseTextureGenerator(int size, long seed, MaterialNoiseSettings noiseSettings, int[] palette) {
        this.size = size;
        this.palette = palette;
        this.noiseField = new SeamlessNoiseField(size, seed, noiseSettings);
    }

    @Override
    public byte[] generatePng() {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double[] variation = sampleVariationField();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, samplePalette(variation[(y * size) + x]));
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode generated texture", exception);
        }
    }

    private double[] sampleVariationField() {
        double[] values = new double[size * size];
        double total = 0.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double value = sampleColorVariation(x, y);
                values[(y * size) + x] = value;
                total += value;
            }
        }
        double bias = 0.5 - (total / values.length);
        for (int i = 0; i < values.length; i++) {
            values[i] = clamp01(values[i] + bias);
        }
        return values;
    }

    private int samplePalette(double value) {
        double scaled = clamp01(value) * (palette.length - 1);
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

    private double sampleColorVariation(int pixelX, int pixelY) {
        double base = sample(pixelX, pixelY);
        double crackMask = noiseField.sampleCrackMask(pixelX, pixelY);
        return applyContrast(base - crackMask * CRACK_DARKENING);
    }

    private static double applyContrast(double value) {
        double centered = (clamp01(value) - 0.5) * CONTRAST + 0.5;
        return clamp01(centered);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int lerp(int start, int end, double t) {
        return (int) Math.round(start + (end - start) * t);
    }
}
