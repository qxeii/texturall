package net.qxeii.texturall.client.texture;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TexturallMaterialShaderGenerator implements ProceduralTextureGenerator {
    private final List<WorldAlignedTextureMaterial> materials;

    public TexturallMaterialShaderGenerator(Iterable<WorldAlignedTextureMaterial> materials) {
        this.materials = new ArrayList<>();
        for (WorldAlignedTextureMaterial material : materials) {
            this.materials.add(material);
        }
        this.materials.sort(Comparator.comparingInt(WorldAlignedTextureMaterial::materialIndex));
    }

    @Override
    public byte[] generatePng() {
        StringBuilder shader = new StringBuilder();
        shader.append("vec3 texturallRgbHex(int rgb) {\n");
        shader.append("    return vec3(\n");
        shader.append("        float((rgb >> 16) & 0xFF),\n");
        shader.append("        float((rgb >> 8) & 0xFF),\n");
        shader.append("        float(rgb & 0xFF)\n");
        shader.append("    ) / 255.0;\n");
        shader.append("}\n\n");

        appendMaterialPredicate(shader);
        shader.append('\n');
        shader.append("vec3 texturallPaletteSample5(int c0, int c1, int c2, int c3, int c4, float value) {\n");
        shader.append("    float scaled = clamp(value, 0.0, 1.0) * 4.0;\n");
        shader.append("    if (scaled < 1.0) return mix(texturallRgbHex(c0), texturallRgbHex(c1), scaled);\n");
        shader.append("    if (scaled < 2.0) return mix(texturallRgbHex(c1), texturallRgbHex(c2), scaled - 1.0);\n");
        shader.append("    if (scaled < 3.0) return mix(texturallRgbHex(c2), texturallRgbHex(c3), scaled - 2.0);\n");
        shader.append("    return mix(texturallRgbHex(c3), texturallRgbHex(c4), scaled - 3.0);\n");
        shader.append("}\n\n");
        appendPaletteSampleFunction(shader);
        shader.append('\n');
        appendPaletteFunction(shader, "materialPaletteStartColor", 0);
        shader.append('\n');
        appendPaletteFunction(shader, "materialPaletteEndColor", -1);
        return shader.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendMaterialPredicate(StringBuilder shader) {
        shader.append("bool isTexturallMaterial(int materialId) {\n");
        shader.append("    switch (materialId) {\n");
        for (WorldAlignedTextureMaterial material : materials) {
            shader.append("        case ").append(material.materialIndex()).append(": return true;\n");
        }
        shader.append("        default: return false;\n");
        shader.append("    }\n");
        shader.append("}\n");
    }

    private void appendPaletteFunction(StringBuilder shader, String functionName, int paletteIndex) {
        shader.append("vec3 ").append(functionName).append("(int materialId) {\n");
        shader.append("    switch (materialId) {\n");
        for (WorldAlignedTextureMaterial material : materials) {
            int[] palette = material.palette();
            int color = palette[paletteIndex >= 0 ? paletteIndex : palette.length + paletteIndex];
            shader.append("        case ").append(material.materialIndex())
                .append(": return texturallRgbHex(0x")
                .append(String.format("%06X", color))
                .append(");\n");
        }
        shader.append("        default: return vec3(0.0);\n");
        shader.append("    }\n");
        shader.append("}\n");
    }

    private void appendPaletteSampleFunction(StringBuilder shader) {
        shader.append("vec3 materialPaletteColor(int materialId, float value) {\n");
        shader.append("    switch (materialId) {\n");
        for (WorldAlignedTextureMaterial material : materials) {
            int[] palette = material.palette();
            shader.append("        case ").append(material.materialIndex()).append(": return texturallPaletteSample5(")
                .append(formatHex(palette[0])).append(", ")
                .append(formatHex(palette[1])).append(", ")
                .append(formatHex(palette[2])).append(", ")
                .append(formatHex(palette[3])).append(", ")
                .append(formatHex(palette[4])).append(", value);\n");
        }
        shader.append("        default: return vec3(0.0);\n");
        shader.append("    }\n");
        shader.append("}\n");
    }

    private static String formatHex(int color) {
        return "0x" + String.format("%06X", color);
    }
}
