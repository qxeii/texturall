#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:light.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:texturall_materials.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 v_baseColor;
in vec2 v_lightUv;
in vec3 v_faceNormal;
in vec3 v_worldPos;
flat in int v_blockPayload;
flat in int v_neighborPayload;
flat in int v_materialId;

out vec4 fragColor;

// --- Vanilla sampling functions (kept for non-Texturall blocks) ---

vec4 sampleNearest(sampler2D sampler, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    vec2 uvTexelCoords = uv / pixelSize;
    vec2 texelCenter = round(uvTexelCoords) - 0.5f;
    vec2 texelOffset = uvTexelCoords - texelCenter;
    texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
    texelOffset = clamp(texelOffset, 0.0f, 1.0f);
    uv = (texelCenter + texelOffset) * pixelSize;
    return textureGrad(sampler, uv, du, dv);
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);
}

vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);
    float minPixelSize = min(pixelSize.x, pixelSize.y);
    float blendFactor = smoothstep(minPixelSize, minPixelSize * 2.0, maxTexelSize);
    float mipLevelExact = max(0.0, log2(sqrt(length(du) * length(dv)) / minPixelSize));
    float mipLevelLow = floor(mipLevelExact);
    float mipBlend = fract(mipLevelExact);
    const vec2 offsets[4] = vec2[](vec2(0.125, 0.375), vec2(-0.125, -0.375), vec2(0.375, -0.125), vec2(-0.375, 0.125));
    vec4 rgssColorLow = vec4(0.0), rgssColorHigh = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 s = uv + offsets[i] * pixelSize;
        rgssColorLow  += textureLod(source, s, mipLevelLow);
        rgssColorHigh += textureLod(source, s, mipLevelLow + 1.0);
    }
    return mix(sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize),
               mix(rgssColorLow, rgssColorHigh, mipBlend) * 0.25,
               blendFactor);
}

ivec2 snappedTexelCoord(vec2 uv, ivec2 texSize) {
    vec2 texel = round(uv * vec2(texSize) - 0.5);
    return ivec2(clamp(texel, vec2(0.0), vec2(texSize) - 1.0));
}

vec3 decodeNormal(vec4 encoded) {
    return normalize(encoded.xyz * 2.0 - 1.0);
}

vec3 normalizeOr(vec3 direction, vec3 fallback) {
    float lengthSquared = dot(direction, direction);
    if (lengthSquared > 1.0e-6) {
        return direction * inversesqrt(lengthSquared);
    }
    return fallback;
}

float horizonFade(vec3 lightDirection) {
    return smoothstep(0.0, 0.25, lightDirection.y);
}

float lambert(vec3 normal, vec3 lightDirection) {
    return max(dot(normal, lightDirection), 0.0);
}

const float LIGHTMAP_MIN = 0.5 / 16.0;

vec3 sampleLightmapAxis(vec2 uv) {
    return texture(Sampler2, clamp(uv, vec2(LIGHTMAP_MIN), vec2(15.5 / 16.0))).rgb;
}

float maxComponent(vec3 value) {
    return max(value.x, max(value.y, value.z));
}

vec3 axisAlignedNormal(vec3 normal) {
    vec3 absNormal = abs(normal);
    if (absNormal.x > absNormal.y && absNormal.x > absNormal.z) {
        return vec3(sign(normal.x), 0.0, 0.0);
    }
    if (absNormal.y > absNormal.z) {
        return vec3(0.0, sign(normal.y), 0.0);
    }
    return vec3(0.0, 0.0, sign(normal.z));
}

mat3 faceTbn(vec3 faceNormal) {
    if (faceNormal.y > 0.5) {
        return mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, 0.0, -1.0), vec3(0.0, 1.0, 0.0));
    }
    if (faceNormal.y < -0.5) {
        return mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, 0.0, 1.0), vec3(0.0, -1.0, 0.0));
    }
    if (faceNormal.z < -0.5) {
        return mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, -1.0, 0.0), vec3(0.0, 0.0, -1.0));
    }
    if (faceNormal.z > 0.5) {
        return mat3(vec3(-1.0, 0.0, 0.0), vec3(0.0, -1.0, 0.0), vec3(0.0, 0.0, 1.0));
    }
    if (faceNormal.x > 0.5) {
        return mat3(vec3(0.0, 0.0, 1.0), vec3(0.0, -1.0, 0.0), vec3(1.0, 0.0, 0.0));
    }
    return mat3(vec3(0.0, 0.0, -1.0), vec3(0.0, -1.0, 0.0), vec3(-1.0, 0.0, 0.0));
}

mat3 faceLightBasis(vec3 faceNormal) {
    if (faceNormal.y > 0.5) {
        return mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, 0.0, 1.0), vec3(0.0, 1.0, 0.0));
    }
    if (faceNormal.y < -0.5) {
        return mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, 0.0, 1.0), vec3(0.0, -1.0, 0.0));
    }
    if (faceNormal.z < -0.5) {
        return mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0), vec3(0.0, 0.0, -1.0));
    }
    if (faceNormal.z > 0.5) {
        return mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0), vec3(0.0, 0.0, 1.0));
    }
    if (faceNormal.x > 0.5) {
        return mat3(vec3(0.0, 0.0, 1.0), vec3(0.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0));
    }
    return mat3(vec3(0.0, 0.0, 1.0), vec3(0.0, 1.0, 0.0), vec3(-1.0, 0.0, 0.0));
}

vec3 blockOrigin(vec3 worldPos, vec3 faceNormal) {
    return floor(worldPos - max(faceNormal, vec3(0.0)));
}

vec2 faceLocalUv(vec3 localPos, vec3 faceNormal) {
    if (faceNormal.y > 0.5) {
        return vec2(clamp(localPos.x, 0.0, 1.0), clamp(1.0 - localPos.z, 0.0, 1.0));
    }
    if (faceNormal.y < -0.5) {
        return vec2(clamp(localPos.x, 0.0, 1.0), clamp(localPos.z, 0.0, 1.0));
    }
    if (faceNormal.z < -0.5) {
        return vec2(clamp(localPos.x, 0.0, 1.0), clamp(1.0 - localPos.y, 0.0, 1.0));
    }
    if (faceNormal.z > 0.5) {
        return vec2(clamp(1.0 - localPos.x, 0.0, 1.0), clamp(1.0 - localPos.y, 0.0, 1.0));
    }
    if (faceNormal.x > 0.5) {
        return vec2(clamp(localPos.z, 0.0, 1.0), clamp(1.0 - localPos.y, 0.0, 1.0));
    }
    return vec2(clamp(1.0 - localPos.z, 0.0, 1.0), clamp(1.0 - localPos.y, 0.0, 1.0));
}

vec2 faceLightUv(vec3 localPos, vec3 faceNormal) {
    if (abs(faceNormal.y) > 0.5) {
        return vec2(clamp(localPos.x, 0.0, 1.0), clamp(localPos.z, 0.0, 1.0));
    }
    if (abs(faceNormal.z) > 0.5) {
        return vec2(clamp(localPos.x, 0.0, 1.0), clamp(localPos.y, 0.0, 1.0));
    }
    return vec2(clamp(localPos.z, 0.0, 1.0), clamp(localPos.y, 0.0, 1.0));
}

vec2 worldAlignedUv(vec3 worldPos, vec3 faceNormal) {
    vec3 origin = blockOrigin(worldPos, faceNormal);
    float minU;
    if (faceNormal.y > 0.5 || faceNormal.y < -0.5 || faceNormal.z < -0.5) {
        minU = origin.x;
    } else if (faceNormal.z > 0.5) {
        minU = -(origin.x + 1.0);
    } else if (faceNormal.x > 0.5) {
        minU = origin.z;
    } else {
        minU = -(origin.z + 1.0);
    }

    float minV;
    if (faceNormal.y < -0.5) {
        minV = origin.z;
    } else if (faceNormal.y > 0.5) {
        minV = -(origin.z + 1.0);
    } else {
        minV = -(origin.y + 1.0);
    }

    float faceU;
    if (faceNormal.y > 0.5 || faceNormal.y < -0.5 || faceNormal.z < -0.5) {
        faceU = worldPos.x;
    } else if (faceNormal.z > 0.5) {
        faceU = -worldPos.x;
    } else if (faceNormal.x > 0.5) {
        faceU = worldPos.z;
    } else {
        faceU = -worldPos.z;
    }

    float faceV;
    if (faceNormal.y > 0.5) {
        faceV = -worldPos.z;
    } else if (faceNormal.y < -0.5) {
        faceV = worldPos.z;
    } else {
        faceV = -worldPos.y;
    }

    float sheetPixels = 256.0;
    float baseU = mod(minU * 16.0, sheetPixels);
    if (baseU < 0.0) {
        baseU += sheetPixels;
    }
    float baseV = mod(minV * 16.0, sheetPixels);
    if (baseV < 0.0) {
        baseV += sheetPixels;
    }

    return vec2(
        (baseU + (faceU - minU) * 16.0) / sheetPixels,
        (baseV + (faceV - minV) * 16.0) / sheetPixels
    );
}

float decodePackedNibble(int packedValue, int nibbleIndex) {
    int divisor = 1;
    if (nibbleIndex == 1) {
        divisor = 16;
    } else if (nibbleIndex == 2) {
        divisor = 256;
    } else if (nibbleIndex == 3) {
        divisor = 4096;
    }
    return float((packedValue / divisor) % 16);
}

int decodePackedMaterial6(int packedValue, int fieldIndex) {
    int divisor = 1;
    if (fieldIndex == 1) {
        divisor = 64;
    } else if (fieldIndex == 2) {
        divisor = 4096;
    } else if (fieldIndex == 3) {
        divisor = 262144;
    }
    return (packedValue / divisor) % 64;
}

vec2 alignToFaceTexelGrid(vec2 faceUv) {
    return clamp((floor(faceUv * 16.0) + 0.5) / 16.0, vec2(0.0), vec2(1.0));
}

float edgeBlendWeight(float distanceToEdge, int neighborMaterialId, int currentMaterialId) {
    if (neighborMaterialId == 0 || neighborMaterialId == currentMaterialId || !isTexturallMaterial(neighborMaterialId)) {
        return 0.0;
    }
    const float bandWidth = 8.0 / 16.0;
    return 1.0 - smoothstep(0.0, bandWidth, distanceToEdge);
}

vec2 bilerpGradient(float bottomLeft, float bottomRight, float topLeft, float topRight, vec2 faceUv) {
    float du = mix(bottomRight - bottomLeft, topRight - topLeft, faceUv.y);
    float dv = mix(topLeft - bottomLeft, topRight - bottomRight, faceUv.x);
    return vec2(du, dv);
}

void main() {
    bool isCustomMaterial = isTexturallMaterial(v_materialId);

#ifdef ALPHA_CUTOUT
    if (!isCustomMaterial) {
        vec2 pixelSize = 1.0 / vec2(TextureSize);
        vec4 texColor = UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, pixelSize)
                                     : sampleNearest(Sampler0, texCoord0, pixelSize);
        if (texColor.a < ALPHA_CUTOUT) {
            discard;
        }
    }
#endif

    vec4 color;

    if (isCustomMaterial) {
        vec3 faceNormal = axisAlignedNormal(normalize(v_faceNormal));
        mat3 tbn = faceTbn(faceNormal);
        mat3 lightBasis = faceLightBasis(faceNormal);
        vec2 atlasPixelSize = 1.0 / vec2(TextureSize);
        vec4 normalSample = sampleNearest(Sampler0, texCoord0, atlasPixelSize);
        vec3 tsNormal = decodeNormal(normalSample);
        vec3 worldNormal = normalize(tbn * tsNormal);

        vec3 sunDirection = normalizeOr(Light0_Direction, vec3(0.0, 1.0, 0.0));
        vec3 moonDirection = normalizeOr(Light1_Direction, -sunDirection);
        float surfaceVariation = normalSample.a;
        vec3 paletteStartColor = materialPaletteStartColor(v_materialId);
        vec3 paletteEndColor = materialPaletteEndColor(v_materialId);
        vec3 localPos = v_worldPos - blockOrigin(v_worldPos, faceNormal);
        vec2 smoothFaceUv = faceLightUv(localPos, faceNormal);
        vec2 blendFaceUv = alignToFaceTexelGrid(smoothFaceUv);
        int uLowMaterial = decodePackedMaterial6(v_neighborPayload, 0);
        int uHighMaterial = decodePackedMaterial6(v_neighborPayload, 1);
        int vLowMaterial = decodePackedMaterial6(v_neighborPayload, 2);
        int vHighMaterial = decodePackedMaterial6(v_neighborPayload, 3);
        vec4 edgeWeights = vec4(
            edgeBlendWeight(blendFaceUv.x, uLowMaterial, v_materialId),
            edgeBlendWeight(1.0 - blendFaceUv.x, uHighMaterial, v_materialId),
            edgeBlendWeight(blendFaceUv.y, vLowMaterial, v_materialId),
            edgeBlendWeight(1.0 - blendFaceUv.y, vHighMaterial, v_materialId)
        );
        float totalBlendWeight = 1.0 + edgeWeights.x + edgeWeights.y + edgeWeights.z + edgeWeights.w;
        vec3 surfaceColor = materialPaletteColor(v_materialId, surfaceVariation);
        surfaceColor += materialPaletteColor(uLowMaterial, surfaceVariation) * edgeWeights.x;
        surfaceColor += materialPaletteColor(uHighMaterial, surfaceVariation) * edgeWeights.y;
        surfaceColor += materialPaletteColor(vLowMaterial, surfaceVariation) * edgeWeights.z;
        surfaceColor += materialPaletteColor(vHighMaterial, surfaceVariation) * edgeWeights.w;
        surfaceColor /= totalBlendWeight;
        paletteStartColor += materialPaletteStartColor(uLowMaterial) * edgeWeights.x;
        paletteStartColor += materialPaletteStartColor(uHighMaterial) * edgeWeights.y;
        paletteStartColor += materialPaletteStartColor(vLowMaterial) * edgeWeights.z;
        paletteStartColor += materialPaletteStartColor(vHighMaterial) * edgeWeights.w;
        paletteStartColor /= totalBlendWeight;
        paletteEndColor += materialPaletteEndColor(uLowMaterial) * edgeWeights.x;
        paletteEndColor += materialPaletteEndColor(uHighMaterial) * edgeWeights.y;
        paletteEndColor += materialPaletteEndColor(vLowMaterial) * edgeWeights.z;
        paletteEndColor += materialPaletteEndColor(vHighMaterial) * edgeWeights.w;
        paletteEndColor /= totalBlendWeight;

        float blockBottomLeft = decodePackedNibble(v_blockPayload, 0);
        float blockBottomRight = decodePackedNibble(v_blockPayload, 1);
        float blockTopLeft = decodePackedNibble(v_blockPayload, 2);
        float blockTopRight = decodePackedNibble(v_blockPayload, 3);
        vec2 blockGradient = bilerpGradient(blockBottomLeft, blockBottomRight, blockTopLeft, blockTopRight, smoothFaceUv) / 15.0;
        vec3 blockDirection = normalizeOr(
            lightBasis * vec3(blockGradient, 0.35),
            faceNormal
        );

        float sunVisibility = horizonFade(sunDirection);
        float moonVisibility = horizonFade(moonDirection);
        float sunDirect = lambert(worldNormal, sunDirection) * sunVisibility;
        float moonDirect = lambert(worldNormal, moonDirection) * moonVisibility;
        float skyDirectional = clamp(sunDirect + moonDirect * 0.55, 0.0, 1.0);
        vec3 skyLight = max(sampleLightmapAxis(vec2(LIGHTMAP_MIN, v_lightUv.y)), vec3(0.0));
        vec3 blockLight = max(sampleLightmapAxis(vec2(v_lightUv.x, LIGHTMAP_MIN)), vec3(0.0));
        vec3 vanillaLight = max(texture(Sampler2, v_lightUv).rgb, vec3(0.0));

        float blockDirectional = lambert(worldNormal, blockDirection);
        vec3 lighting = surfaceColor * ((skyLight * skyDirectional) + (blockLight * blockDirectional));
        color = vec4(max(lighting, vec3(0.0)), 1.0);
    } else {
        // --- Vanilla block: unchanged ---
        vec2 pixelSize = 1.0 / vec2(TextureSize);
        vec4 texColor = UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, pixelSize)
                                     : sampleNearest(Sampler0, texCoord0, pixelSize);
        vec3 vanillaLight = max(texture(Sampler2, v_lightUv).rgb, vec3(0.0));
        color = vec4(texColor.rgb * v_baseColor.rgb * vanillaLight, texColor.a * v_baseColor.a);
    }

    color = mix(FogColor * vec4(1.0, 1.0, 1.0, color.a), color, ChunkVisibility);
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
                          FogEnvironmentalStart, FogEnvironmentalEnd,
                          FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
