package net.qxeii.texturall.client.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class NormalTextureGenerator implements ProceduralTextureGenerator {
    private static final double NORMAL_STRENGTH = 5.0;

    private final int size;
    private final double scale;
    private final SimplexNoise noise;
    private final SimplexNoise warpNoiseX;
    private final SimplexNoise warpNoiseY;

    public NormalTextureGenerator(int size, long seed, double scale) {
        this.size = size;
        this.scale = scale;
        this.noise = new SimplexNoise(seed);
        this.warpNoiseX = new SimplexNoise(seed ^ 0x517CC1B727220A95L);
        this.warpNoiseY = new SimplexNoise(seed ^ 0xBF58476D1CE4E5B9L);
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
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }

    private double sampleWrapped(int pixelX, int pixelY) {
        return sample(Math.floorMod(pixelX, size), Math.floorMod(pixelY, size));
    }

    private double sample(int pixelX, int pixelY) {
        double x = pixelX / scale;
        double y = pixelY / scale;
        double wx = warpNoiseX.sample(x, y) * 4.0;
        double wy = warpNoiseY.sample(x, y) * 4.0;
        return noise.sample((x + wx) * 0.01, (y + wy) * 0.05) * 0.5 + 0.5;
    }

    private static int encode(float value) {
        return Math.max(0, Math.min(255, Math.round((value * 0.5F + 0.5F) * 255.0F)));
    }
}
