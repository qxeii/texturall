package net.qxeii.texturall.client.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class NormalTextureGenerator implements ProceduralTextureGenerator {
    private static final double NORMAL_STRENGTH = 5.0;

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
        float height = (float) sampleWrapped(pixelX, pixelY);
        float hL = (float) sampleWrapped(pixelX - 1, pixelY);
        float hR = (float) sampleWrapped(pixelX + 1, pixelY);
        float hU = (float) sampleWrapped(pixelX, pixelY - 1);
        float hD = (float) sampleWrapped(pixelX, pixelY + 1);

        float nx = (hL - hR) * (float) NORMAL_STRENGTH;
        float ny = (hU - hD) * (float) NORMAL_STRENGTH;
        float nz = 1.0F;
        float invLen = (float) (1.0 / Math.sqrt(nx * nx + ny * ny + nz * nz));

        int red = encode(nx * invLen);
        int green = encode(ny * invLen);
        int blue = encode(nz * invLen);
        int alpha = encode(height * 2.0F - 1.0F);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private double sampleWrapped(int pixelX, int pixelY) {
        return noiseField.sample(noiseField.wrap(pixelX), noiseField.wrap(pixelY));
    }

    private static int encode(float value) {
        return Math.max(0, Math.min(255, Math.round((value * 0.5F + 0.5F) * 255.0F)));
    }
}
