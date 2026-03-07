package net.qxeii.texturall.client.texture;

public final class SeamlessNoiseField {
    private static final double BASE_FREQUENCY = 0.16;
    private static final double WARP_FREQUENCY = 0.11;
    private static final double WARP_AMPLITUDE = 5.0;
    private static final int OCTAVES = 4;
    private static final double CRACK_FREQUENCY = 0.58;
    private static final double CRACK_MEDIUM_FREQUENCY = 1.22;
    private static final double CRACK_SPREAD_FREQUENCY = 0.52;
    private static final double CRACK_HALF_WIDTH = 0.11;
    private static final double CRACK_SPREAD_SOFTNESS = 0.10;
    private static final double CRACK_QUANTITY = 0.62;

    private final int size;
    private final double scale;
    private final double periodX;
    private final double periodY;
    private final SimplexNoise noise;
    private final SimplexNoise warpNoiseX;
    private final SimplexNoise warpNoiseY;
    private final SimplexNoise crackNoise;
    private final SimplexNoise crackMediumNoise;
    private final SimplexNoise crackSpreadNoise;

    public SeamlessNoiseField(int size, long seed, double scale) {
        this.size = size;
        this.scale = scale;
        this.periodX = size / scale;
        this.periodY = size / scale;
        this.noise = new SimplexNoise(seed);
        this.warpNoiseX = new SimplexNoise(seed ^ 0x517CC1B727220A95L);
        this.warpNoiseY = new SimplexNoise(seed ^ 0xBF58476D1CE4E5B9L);
        this.crackNoise = new SimplexNoise(seed ^ 0x94D049BB133111EBL);
        this.crackMediumNoise = new SimplexNoise(seed ^ 0x369DEA0F31A53F85L);
        this.crackSpreadNoise = new SimplexNoise(seed ^ 0xDB4F0B9175AE2165L);
    }

    public double sample(int pixelX, int pixelY) {
        double x = pixelX / scale;
        double y = pixelY / scale;
        double warpX = sampleOctaves(warpNoiseX, x + 17.0, y - 29.0, WARP_FREQUENCY, 2) * WARP_AMPLITUDE;
        double warpY = sampleOctaves(warpNoiseY, x - 43.0, y + 11.0, WARP_FREQUENCY, 2) * WARP_AMPLITUDE;
        double value = sampleOctaves(noise, x + warpX, y + warpY, BASE_FREQUENCY, OCTAVES);
        return clamp01(value * 0.5 + 0.5);
    }

    public int wrap(int pixel) {
        return Math.floorMod(pixel, size);
    }

    public double sampleCrackMask(int pixelX, int pixelY) {
        double x = pixelX / scale;
        double y = pixelY / scale;
        double crackBase = sampleSeamless(
            crackNoise,
            x * CRACK_FREQUENCY,
            y * CRACK_FREQUENCY,
            periodX * CRACK_FREQUENCY,
            periodY * CRACK_FREQUENCY
        );
        double crackMedium = sampleSeamless(
            crackMediumNoise,
            x * CRACK_MEDIUM_FREQUENCY,
            y * CRACK_MEDIUM_FREQUENCY,
            periodX * CRACK_MEDIUM_FREQUENCY,
            periodY * CRACK_MEDIUM_FREQUENCY
        ) * 0.5;
        double crackNoiseValue = clamp01(((crackBase + crackMedium) / 1.5) * 0.5 + 0.5);
        double crackSpread = sampleSeamless(
            crackSpreadNoise,
            x * CRACK_SPREAD_FREQUENCY,
            y * CRACK_SPREAD_FREQUENCY,
            periodX * CRACK_SPREAD_FREQUENCY,
            periodY * CRACK_SPREAD_FREQUENCY
        ) * 0.5 + 0.5;
        double crackBand = smoothstep(0.0, Math.max(CRACK_HALF_WIDTH, 1.0e-6), Math.abs(crackNoiseValue - 0.5));
        double crackPresence = CRACK_QUANTITY * smoothstep(
            crackSpread - CRACK_SPREAD_SOFTNESS,
            crackSpread + CRACK_SPREAD_SOFTNESS,
            CRACK_QUANTITY
        );
        return clamp01(crackPresence * (1.0 - crackBand));
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

    private double sampleOctaves(SimplexNoise noise, double x, double y, double baseFrequency, int octaves) {
        double frequency = baseFrequency;
        double amplitude = 1.0;
        double value = 0.0;
        double totalAmplitude = 0.0;

        for (int octave = 0; octave < octaves; octave++) {
            double offsetX = octave * 23.17;
            double offsetY = octave * 37.29;
            value += sampleSeamless(
                noise,
                (x + offsetX) * frequency,
                (y + offsetY) * frequency,
                periodX * frequency,
                periodY * frequency
            ) * amplitude;
            totalAmplitude += amplitude;
            frequency *= 2.0;
            amplitude *= 0.5;
        }

        return value / totalAmplitude;
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
