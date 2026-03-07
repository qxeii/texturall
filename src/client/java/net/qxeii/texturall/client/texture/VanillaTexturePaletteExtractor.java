package net.qxeii.texturall.client.texture;

import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VanillaTexturePaletteExtractor {
    private static final double TRIM_FRACTION = 0.05;
    private static final double PALETTE_CONTRAST = 2.1;

    private VanillaTexturePaletteExtractor() {
    }

    public static int[] derivePalette(int steps, Identifier... textureIds) {
        if (steps < 2) {
            throw new IllegalArgumentException("Palette steps must be at least 2");
        }
        if (textureIds.length == 0) {
            throw new IllegalArgumentException("At least one vanilla texture is required");
        }

        Map<Integer, Integer> histogram = new HashMap<>();
        for (Identifier textureId : textureIds) {
            try (InputStream stream = openVanillaTexture(textureId)) {
                if (stream == null) {
                    throw new IllegalStateException("Missing vanilla texture " + textureId);
                }

                BufferedImage image = ImageIO.read(stream);
                if (image == null) {
                    throw new IllegalStateException("Failed to decode vanilla texture " + textureId);
                }

                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = image.getRGB(x, y);
                        if (((argb >>> 24) & 0xFF) == 0) {
                            continue;
                        }
                        histogram.merge(argb & 0xFFFFFF, 1, Integer::sum);
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to read vanilla texture " + textureId, exception);
            }
        }

        if (histogram.isEmpty()) {
            throw new IllegalStateException("Vanilla textures contained no opaque pixels");
        }

        List<WeightedColor> colors = histogram.entrySet().stream()
            .map(entry -> new WeightedColor(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingDouble(WeightedColor::luminance))
            .toList();

        double totalWeight = colors.stream().mapToDouble(WeightedColor::weight).sum();
        double lowerBound = totalWeight * TRIM_FRACTION;
        double upperBound = totalWeight * (1.0 - TRIM_FRACTION);
        if (upperBound <= lowerBound) {
            lowerBound = 0.0;
            upperBound = totalWeight;
        }

        int[] palette = new int[steps];
        for (int i = 0; i < steps; i++) {
            double bucketStart = lerp(lowerBound, upperBound, (double) i / steps);
            double bucketEnd = lerp(lowerBound, upperBound, (double) (i + 1) / steps);
            palette[i] = averageBucket(colors, bucketStart, bucketEnd);
        }
        int meanColor = averageBucket(colors, lowerBound, upperBound);
        for (int i = 0; i < palette.length; i++) {
            palette[i] = stretchContrast(palette[i], meanColor);
        }
        normalizePaletteMean(palette, meanColor);
        return palette;
    }

    private static int averageBucket(List<WeightedColor> colors, double bucketStart, double bucketEnd) {
        double cumulative = 0.0;
        double totalWeight = 0.0;
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;

        for (WeightedColor color : colors) {
            double next = cumulative + color.weight();
            double overlap = Math.min(next, bucketEnd) - Math.max(cumulative, bucketStart);
            if (overlap > 0.0) {
                totalWeight += overlap;
                red += ((color.rgb() >>> 16) & 0xFF) * overlap;
                green += ((color.rgb() >>> 8) & 0xFF) * overlap;
                blue += (color.rgb() & 0xFF) * overlap;
            }
            cumulative = next;
        }

        if (totalWeight <= 0.0) {
            return sampleNearestColor(colors, (bucketStart + bucketEnd) * 0.5);
        }

        int r = clampColor(Math.round((float) (red / totalWeight)));
        int g = clampColor(Math.round((float) (green / totalWeight)));
        int b = clampColor(Math.round((float) (blue / totalWeight)));
        return (r << 16) | (g << 8) | b;
    }

    private static int sampleNearestColor(List<WeightedColor> colors, double targetWeight) {
        double cumulative = 0.0;
        for (WeightedColor color : colors) {
            cumulative += color.weight();
            if (targetWeight <= cumulative) {
                return color.rgb();
            }
        }
        return colors.get(colors.size() - 1).rgb();
    }

    private static InputStream openVanillaTexture(Identifier textureId) {
        String path = "assets/" + textureId.getNamespace() + "/" + textureId.getPath();
        return VanillaTexturePaletteExtractor.class.getClassLoader().getResourceAsStream(path);
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static int clampColor(int channel) {
        return Math.max(0, Math.min(255, channel));
    }

    private static int stretchContrast(int color, int meanColor) {
        int r = stretchChannel((color >>> 16) & 0xFF, (meanColor >>> 16) & 0xFF);
        int g = stretchChannel((color >>> 8) & 0xFF, (meanColor >>> 8) & 0xFF);
        int b = stretchChannel(color & 0xFF, meanColor & 0xFF);
        return (r << 16) | (g << 8) | b;
    }

    private static void normalizePaletteMean(int[] palette, int targetMeanColor) {
        int currentMean = averageColors(palette);
        int redShift = ((targetMeanColor >>> 16) & 0xFF) - ((currentMean >>> 16) & 0xFF);
        int greenShift = ((targetMeanColor >>> 8) & 0xFF) - ((currentMean >>> 8) & 0xFF);
        int blueShift = (targetMeanColor & 0xFF) - (currentMean & 0xFF);

        for (int i = 0; i < palette.length; i++) {
            int color = palette[i];
            int r = clampColor(((color >>> 16) & 0xFF) + redShift);
            int g = clampColor(((color >>> 8) & 0xFF) + greenShift);
            int b = clampColor((color & 0xFF) + blueShift);
            palette[i] = (r << 16) | (g << 8) | b;
        }
    }

    private static int averageColors(int[] colors) {
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        for (int color : colors) {
            red += (color >>> 16) & 0xFF;
            green += (color >>> 8) & 0xFF;
            blue += color & 0xFF;
        }
        int count = colors.length;
        int r = clampColor(Math.round((float) red / count));
        int g = clampColor(Math.round((float) green / count));
        int b = clampColor(Math.round((float) blue / count));
        return (r << 16) | (g << 8) | b;
    }

    private static int stretchChannel(int channel, int meanChannel) {
        return clampColor(Math.round((float) ((channel - meanChannel) * PALETTE_CONTRAST + meanChannel)));
    }

    private static double luminance(int rgb) {
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private record WeightedColor(int rgb, int weight) {
        private double luminance() {
            return VanillaTexturePaletteExtractor.luminance(rgb);
        }
    }
}
