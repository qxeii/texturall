package net.qxeii.texturall.client.texture;

public record MaterialNoiseSettings(
    double scale,
    double squashX,
    double squashY,
    double crackDensity
) {
    public MaterialNoiseSettings {
        if (scale <= 0.0) {
            throw new IllegalArgumentException("scale must be > 0");
        }
        if (squashX <= 0.0) {
            throw new IllegalArgumentException("squashX must be > 0");
        }
        if (squashY <= 0.0) {
            throw new IllegalArgumentException("squashY must be > 0");
        }
        if (crackDensity < 0.0) {
            throw new IllegalArgumentException("crackDensity must be >= 0");
        }
    }
}
