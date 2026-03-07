package net.qxeii.texturall.client.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class NormalTextureGenerator implements ProceduralTextureGenerator {
    private static final double NORMAL_STRENGTH = 5.0;
    private static final double DETAIL_WEIGHT = 0.45;
    private static final double CONTRAST = 2.35;
    private static final double CRACK_DEPTH = 0.16;
    private static final double CRACK_DARKENING = 0.28;

    private final int size;
    private final SeamlessNoiseField noiseField;

    public NormalTextureGenerator(int size, long seed, double scale) {
        this.size = size;
        this.noiseField = new SeamlessNoiseField(size, seed, scale);
    }

    @Override
    public byte[] generatePng() {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, sampleNormal(x, y));
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode generated normal texture", exception);
        }
    }

    private int sampleNormal(int pixelX, int pixelY) {
        float height = (float) sampleHeight(pixelX, pixelY);
        float colorVariation = (float) sampleColorVariation(pixelX, pixelY);
        float hL = (float) sampleHeight(pixelX - 1, pixelY);
        float hR = (float) sampleHeight(pixelX + 1, pixelY);
        float hU = (float) sampleHeight(pixelX, pixelY - 1);
        float hD = (float) sampleHeight(pixelX, pixelY + 1);

        float nx = (hL - hR) * (float) NORMAL_STRENGTH;
        float ny = (hU - hD) * (float) NORMAL_STRENGTH;
        float nz = 1.0F;
        float invLen = (float) (1.0 / Math.sqrt(nx * nx + ny * ny + nz * nz));

        int red = encode(nx * invLen);
        int green = encode(ny * invLen);
        int blue = encode(nz * invLen);
        int alpha = encode(colorVariation * 2.0F - 1.0F);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private double sampleWrapped(int pixelX, int pixelY) {
        return noiseField.sample(noiseField.wrap(pixelX), noiseField.wrap(pixelY));
    }

    private double sampleHeight(int pixelX, int pixelY) {
        return sampleWrapped(pixelX, pixelY) - noiseField.sampleCrackMask(pixelX, pixelY) * CRACK_DEPTH;
    }

    private double sampleColorVariation(int pixelX, int pixelY) {
        double base = sampleWrapped(pixelX, pixelY);
        double detail = sampleWrapped(pixelX * 10, pixelY * 10);
        double crackMask = noiseField.sampleCrackMask(pixelX, pixelY);
        return applyContrast(mix(base, detail, DETAIL_WEIGHT) - crackMask * CRACK_DARKENING);
    }

    private static double applyContrast(double value) {
        double centered = (clamp01(value) - 0.5) * CONTRAST + 0.5;
        return clamp01(centered);
    }

    private static double mix(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int encode(float value) {
        return Math.max(0, Math.min(255, Math.round((value * 0.5F + 0.5F) * 255.0F)));
    }
}
