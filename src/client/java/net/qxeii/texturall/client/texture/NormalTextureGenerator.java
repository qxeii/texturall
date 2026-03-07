package net.qxeii.texturall.client.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class NormalTextureGenerator implements ProceduralTextureGenerator {
    private static final double NORMAL_STRENGTH = 5.0;
    private static final double CONTRAST = 2.35;
    private static final double CRACK_DARKENING = 0.28;
    private static final double CRACK_NORMAL_STRENGTH = 1.2;

    private final int size;
    private final SeamlessNoiseField noiseField;

    public NormalTextureGenerator(int size, long seed, MaterialNoiseSettings noiseSettings) {
        this.size = size;
        this.noiseField = new SeamlessNoiseField(size, seed, noiseSettings);
    }

    @Override
    public byte[] generatePng() {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double[] variation = sampleVariationField();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, sampleNormal(x, y, (float) variation[(y * size) + x]));
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode generated normal texture", exception);
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

    private int sampleNormal(int pixelX, int pixelY, float colorVariation) {
        float height = (float) sampleWrapped(pixelX, pixelY);
        float hL = (float) sampleWrapped(pixelX - 1, pixelY);
        float hR = (float) sampleWrapped(pixelX + 1, pixelY);
        float hU = (float) sampleWrapped(pixelX, pixelY - 1);
        float hD = (float) sampleWrapped(pixelX, pixelY + 1);

        float nx = (hL - hR) * (float) NORMAL_STRENGTH;
        float ny = (hU - hD) * (float) NORMAL_STRENGTH;
        float crackNoise = (float) noiseField.sampleCrackNoiseValue(pixelX, pixelY);
        float crackCoreFactor = (float) noiseField.sampleCrackCoreFactor(pixelX, pixelY);
        float crackLeft = (float) noiseField.sampleCrackNoiseValue(pixelX - 1, pixelY);
        float crackRight = (float) noiseField.sampleCrackNoiseValue(pixelX + 1, pixelY);
        float crackDown = (float) noiseField.sampleCrackNoiseValue(pixelX, pixelY - 1);
        float crackUp = (float) noiseField.sampleCrackNoiseValue(pixelX, pixelY + 1);
        float crackDerivativeX = (crackRight - crackLeft) * 0.5F;
        float crackDerivativeY = (crackUp - crackDown) * 0.5F;
        float crackGradLengthSquared = crackDerivativeX * crackDerivativeX + crackDerivativeY * crackDerivativeY;
        if (crackGradLengthSquared > 1.0e-8F) {
            float crackSign = crackNoise >= 0.5F ? 1.0F : -1.0F;
            float crackInvLength = (float) (1.0 / Math.sqrt(crackGradLengthSquared));
            float crackNormalFactor = crackCoreFactor * (float) CRACK_NORMAL_STRENGTH;
            nx -= crackSign * crackDerivativeX * crackInvLength * crackNormalFactor;
            ny -= crackSign * crackDerivativeY * crackInvLength * crackNormalFactor;
        }
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

    private double sampleColorVariation(int pixelX, int pixelY) {
        double base = sampleWrapped(pixelX, pixelY);
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

    private static int encode(float value) {
        return Math.max(0, Math.min(255, Math.round((value * 0.5F + 0.5F) * 255.0F)));
    }
}
