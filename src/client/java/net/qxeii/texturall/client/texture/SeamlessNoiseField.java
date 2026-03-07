package net.qxeii.texturall.client.texture;

public final class SeamlessNoiseField {
    private static final double RELIEF_NOISE_SCALE = 0.1;
    private static final double RELIEF_NOISE_MEDIUM_SCALE = 0.5;
    private static final double RELIEF_NOISE_FINE_SCALE = 1.0;
    private static final double RELIEF_MEDIUM_WEIGHT = 0.45;
    private static final double RELIEF_FINE_WEIGHT = 0.2;
    private static final double RELIEF_TOTAL_WEIGHT = 1.0 + RELIEF_MEDIUM_WEIGHT + RELIEF_FINE_WEIGHT;
    private static final double CRACK_FREQUENCY = 0.58;
    private static final double CRACK_MEDIUM_FREQUENCY = 1.22;
    private static final double CRACK_SPREAD_FREQUENCY = 0.52;
    private static final double CRACK_HALF_WIDTH = 0.11;
    private static final double CRACK_SPREAD_SOFTNESS = 0.10;
    private static final double BASE_CRACK_QUANTITY = 0.62;

    private final int size;
    private final double scaleX;
    private final double scaleY;
    private final double periodX;
    private final double periodY;
    private final double crackDensity;
    private final SimplexNoise reliefNoise;
    private final SimplexNoise reliefMediumNoise;
    private final SimplexNoise reliefFineNoise;
    private final SimplexNoise crackNoise;
    private final SimplexNoise crackMediumNoise;
    private final SimplexNoise crackSpreadNoise;

    public SeamlessNoiseField(int size, long seed, MaterialNoiseSettings noiseSettings) {
        this.size = size;
        this.scaleX = noiseSettings.scale() * noiseSettings.squashX();
        this.scaleY = noiseSettings.scale() * noiseSettings.squashY();
        this.periodX = size / scaleX;
        this.periodY = size / scaleY;
        this.crackDensity = noiseSettings.crackDensity();
        this.reliefNoise = new SimplexNoise(seed ^ 89L);
        this.reliefMediumNoise = new SimplexNoise(seed ^ 97L);
        this.reliefFineNoise = new SimplexNoise(seed ^ 113L);
        this.crackNoise = new SimplexNoise(seed ^ 0x94D049BB133111EBL);
        this.crackMediumNoise = new SimplexNoise(seed ^ 0x369DEA0F31A53F85L);
        this.crackSpreadNoise = new SimplexNoise(seed ^ 0xDB4F0B9175AE2165L);
    }

    public double sample(int pixelX, int pixelY) {
        double x = pixelX / scaleX;
        double y = pixelY / scaleY;
        double base = sampleSeamless(
            reliefNoise,
            x * RELIEF_NOISE_SCALE,
            y * RELIEF_NOISE_SCALE,
            periodX * RELIEF_NOISE_SCALE,
            periodY * RELIEF_NOISE_SCALE
        );
        double medium = sampleSeamless(
            reliefMediumNoise,
            x * RELIEF_NOISE_MEDIUM_SCALE,
            y * RELIEF_NOISE_MEDIUM_SCALE,
            periodX * RELIEF_NOISE_MEDIUM_SCALE,
            periodY * RELIEF_NOISE_MEDIUM_SCALE
        ) * RELIEF_MEDIUM_WEIGHT;
        double fine = sampleSeamless(
            reliefFineNoise,
            x * RELIEF_NOISE_FINE_SCALE,
            y * RELIEF_NOISE_FINE_SCALE,
            periodX * RELIEF_NOISE_FINE_SCALE,
            periodY * RELIEF_NOISE_FINE_SCALE
        ) * RELIEF_FINE_WEIGHT;
        return clamp01(((base + medium + fine) / RELIEF_TOTAL_WEIGHT) * 0.5 + 0.5);
    }

    public int wrap(int pixel) {
        return Math.floorMod(pixel, size);
    }

    public double sampleCrackNoiseValue(int pixelX, int pixelY) {
        if (crackDensity <= 1.0e-6) {
            return 0.5;
        }
        double x = pixelX / scaleX;
        double y = pixelY / scaleY;
        double crackFrequency = CRACK_FREQUENCY * crackDensity;
        double crackMediumFrequency = CRACK_MEDIUM_FREQUENCY * crackDensity;
        double crackBase = sampleSeamless(
            crackNoise,
            x * crackFrequency,
            y * crackFrequency,
            periodX * crackFrequency,
            periodY * crackFrequency
        );
        double crackMedium = sampleSeamless(
            crackMediumNoise,
            x * crackMediumFrequency,
            y * crackMediumFrequency,
            periodX * crackMediumFrequency,
            periodY * crackMediumFrequency
        ) * 0.5;
        return clamp01(((crackBase + crackMedium) / 1.5) * 0.5 + 0.5);
    }

    public double sampleCrackCoreFactor(int pixelX, int pixelY) {
        if (crackDensity <= 1.0e-6) {
            return 0.0;
        }
        double crackNoiseValue = sampleCrackNoiseValue(pixelX, pixelY);
        double x = pixelX / scaleX;
        double y = pixelY / scaleY;
        double crackSpreadFrequency = CRACK_SPREAD_FREQUENCY * crackDensity;
        double crackQuantity = clamp01(BASE_CRACK_QUANTITY * Math.sqrt(crackDensity));
        double crackSpread = sampleSeamless(
            crackSpreadNoise,
            x * crackSpreadFrequency,
            y * crackSpreadFrequency,
            periodX * crackSpreadFrequency,
            periodY * crackSpreadFrequency
        ) * 0.5 + 0.5;
        double crackBand = smoothstep(0.0, Math.max(CRACK_HALF_WIDTH, 1.0e-6), Math.abs(crackNoiseValue - 0.5));
        double crackPresence = crackQuantity * smoothstep(
            crackSpread - CRACK_SPREAD_SOFTNESS,
            crackSpread + CRACK_SPREAD_SOFTNESS,
            crackQuantity
        );
        return clamp01(crackPresence * (1.0 - crackBand));
    }

    public double sampleCrackMask(int pixelX, int pixelY) {
        return sampleCrackCoreFactor(pixelX, pixelY);
    }

    private static double sampleSeamless(SimplexNoise noise, double x, double y, double periodX, double periodY) {
        double wrappedX = wrap(x, periodX);
        double wrappedY = wrap(y, periodY);
        double blendX = fade(wrappedX / periodX);
        double blendY = fade(wrappedY / periodY);

        double s00 = noise.sample(wrappedX, wrappedY);
        double s10 = noise.sample(wrappedX - periodX, wrappedY);
        double s01 = noise.sample(wrappedX, wrappedY - periodY);
        double s11 = noise.sample(wrappedX - periodX, wrappedY - periodY);

        double sx0 = lerp(s00, s10, blendX);
        double sx1 = lerp(s01, s11, blendX);
        return lerp(sx0, sx1, blendY);
    }

    private static double wrap(double value, double period) {
        double wrapped = value % period;
        return wrapped < 0.0 ? wrapped + period : wrapped;
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0 : 1.0;
        }
        double t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
