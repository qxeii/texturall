#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:light.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:texturall_materials.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
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

const float TEXTURALL_TILE_TEXELS = 16.0;
const float TEXTURALL_MERGE_TEXELS = 8.0;
const float TEXTURALL_MERGE_WIDTH = TEXTURALL_MERGE_TEXELS / TEXTURALL_TILE_TEXELS;
const int TEXTURALL_EDGE_MATERIAL_MASK = 63;
const float TEXTURALL_NORMAL_ATLAS_GRID = 8.0;
const float TEXTURALL_NORMAL_SHEET_SIZE = 256.0;
const vec2 TEXTURALL_NORMAL_ATLAS_PIXEL_SIZE = vec2(1.0 / (TEXTURALL_NORMAL_ATLAS_GRID * TEXTURALL_NORMAL_SHEET_SIZE));

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

int decodeEdgeMaterial(int payload, int edgeIndex) {
    return (payload >> (edgeIndex * 6)) & TEXTURALL_EDGE_MATERIAL_MASK;
}

float mergeWeightToMinEdge(float coord) {
    return 1.0 - smoothstep(0.0, TEXTURALL_MERGE_WIDTH, coord);
}

float mergeWeightToMaxEdge(float coord) {
    return smoothstep(1.0 - TEXTURALL_MERGE_WIDTH, 1.0, coord);
}

vec2 clampSheetUv(vec2 sheetUv) {
    vec2 halfTexel = vec2(0.5 / TEXTURALL_NORMAL_SHEET_SIZE);
    return clamp(sheetUv, halfTexel, vec2(1.0) - halfTexel);
}

vec2 materialNormalAtlasUv(int materialId, vec2 sheetUv) {
    int slot = max(materialId - 1, 0);
    float column = float(slot % int(TEXTURALL_NORMAL_ATLAS_GRID));
    float row = floor(float(slot) / TEXTURALL_NORMAL_ATLAS_GRID);
    return (vec2(column, row) + clampSheetUv(sheetUv)) / TEXTURALL_NORMAL_ATLAS_GRID;
}

vec4 sampleMaterialNormal(int materialId, vec2 sheetUv) {
    return sampleNearest(Sampler1, materialNormalAtlasUv(materialId, sheetUv), TEXTURALL_NORMAL_ATLAS_PIXEL_SIZE);
}

void accumulateMergedMaterial(
    int materialId,
    float weight,
    vec2 sheetUv,
    inout vec3 tsNormalSum,
    inout vec3 surfaceColorSum,
    inout vec3 paletteStartSum,
    inout vec3 paletteEndSum,
    inout float totalWeight
) {
    if (materialId <= 0 || weight <= 0.0) {
        return;
    }

    vec4 normalSample = sampleMaterialNormal(materialId, sheetUv);
    float variation = normalSample.a;
    tsNormalSum += decodeNormal(normalSample) * weight;
    surfaceColorSum += materialPaletteColor(materialId, variation) * weight;
    paletteStartSum += materialPaletteStartColor(materialId) * weight;
    paletteEndSum += materialPaletteEndColor(materialId) * weight;
    totalWeight += weight;
}

vec2 lightUvFromLevels(float blockLevel, float skyLevel) {
    return clamp((vec2(blockLevel, skyLevel) + 0.5) / 16.0, vec2(LIGHTMAP_MIN), vec2(15.5 / 16.0));
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

vec2 alignToFaceTexelGrid(vec2 faceUv) {
    return clamp((floor(faceUv * 16.0) + 0.5) / 16.0, vec2(0.0), vec2(1.0));
}

float bilerpLight(float bottomLeft, float bottomRight, float topLeft, float topRight, vec2 faceUv) {
    float bottom = mix(bottomLeft, bottomRight, faceUv.x);
    float top = mix(topLeft, topRight, faceUv.x);
    return mix(bottom, top, faceUv.y);
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
    vec3 lightFloor = sampleLightmapAxis(vec2(LIGHTMAP_MIN));

    if (isCustomMaterial) {
        vec3 faceNormal = axisAlignedNormal(normalize(v_faceNormal));
        mat3 tbn = faceTbn(faceNormal);
        mat3 lightBasis = faceLightBasis(faceNormal);
        vec2 atlasPixelSize = 1.0 / vec2(TextureSize);
        vec2 sheetUv = worldAlignedUv(v_worldPos, faceNormal);
        vec4 normalSample = sampleNearest(Sampler0, texCoord0, atlasPixelSize);
        vec2 faceUv = faceLocalUv(v_worldPos - blockOrigin(v_worldPos, faceNormal), faceNormal);
        float uMinWeight = mergeWeightToMinEdge(faceUv.x);
        float uMaxWeight = mergeWeightToMaxEdge(faceUv.x);
        float vMinWeight = mergeWeightToMinEdge(faceUv.y);
        float vMaxWeight = mergeWeightToMaxEdge(faceUv.y);
        int uMinMaterial = decodeEdgeMaterial(v_neighborPayload, 0);
        int uMaxMaterial = decodeEdgeMaterial(v_neighborPayload, 1);
        int vMinMaterial = decodeEdgeMaterial(v_neighborPayload, 2);
        int vMaxMaterial = decodeEdgeMaterial(v_neighborPayload, 3);
        vec3 tsNormalSum = decodeNormal(normalSample);
        vec3 surfaceColorSum = materialPaletteColor(v_materialId, normalSample.a);
        vec3 paletteStartSum = materialPaletteStartColor(v_materialId);
        vec3 paletteEndSum = materialPaletteEndColor(v_materialId);
        float materialWeight = 1.0;

        accumulateMergedMaterial(uMinMaterial, uMinWeight, sheetUv, tsNormalSum, surfaceColorSum, paletteStartSum, paletteEndSum, materialWeight);
        accumulateMergedMaterial(uMaxMaterial, uMaxWeight, sheetUv, tsNormalSum, surfaceColorSum, paletteStartSum, paletteEndSum, materialWeight);
        accumulateMergedMaterial(vMinMaterial, vMinWeight, sheetUv, tsNormalSum, surfaceColorSum, paletteStartSum, paletteEndSum, materialWeight);
        accumulateMergedMaterial(vMaxMaterial, vMaxWeight, sheetUv, tsNormalSum, surfaceColorSum, paletteStartSum, paletteEndSum, materialWeight);

        vec3 tsNormal = normalize(tsNormalSum);
        vec3 worldNormal = normalize(tbn * tsNormal);

        vec3 sunDirection = normalizeOr(Light0_Direction, vec3(0.0, 1.0, 0.0));
        vec3 moonDirection = normalizeOr(Light1_Direction, -sunDirection);
        vec3 localPos = v_worldPos - blockOrigin(v_worldPos, faceNormal);
        vec2 lightFaceUv = alignToFaceTexelGrid(faceLightUv(localPos, faceNormal));
        vec3 surfaceColor = surfaceColorSum / materialWeight;
        vec3 paletteStartColor = paletteStartSum / materialWeight;
        vec3 paletteEndColor = paletteEndSum / materialWeight;

        float blockBottomLeft = decodePackedNibble(v_blockPayload, 0);
        float blockBottomRight = decodePackedNibble(v_blockPayload, 1);
        float blockTopLeft = decodePackedNibble(v_blockPayload, 2);
        float blockTopRight = decodePackedNibble(v_blockPayload, 3);

        float blockLevel = bilerpLight(blockBottomLeft, blockBottomRight, blockTopLeft, blockTopRight, lightFaceUv);
        vec2 blockGradient = bilerpGradient(blockBottomLeft, blockBottomRight, blockTopLeft, blockTopRight, lightFaceUv);
        vec3 blockDirection = normalizeOr(
            lightBasis * normalize(vec3(blockGradient, max(1.0, blockLevel * 0.35))),
            faceNormal
        );

        float sunVisibility = horizonFade(sunDirection);
        float moonVisibility = horizonFade(moonDirection);
        float sunDirect = lambert(worldNormal, sunDirection) * sunVisibility;
        float moonDirect = lambert(worldNormal, moonDirection) * moonVisibility;
        float skyDirectional = clamp(sunDirect + moonDirect * 0.55, 0.0, 1.0);
        vec3 skyLight = max(sampleLightmapAxis(vec2(LIGHTMAP_MIN, v_lightUv.y)) - lightFloor, vec3(0.0));
        vec3 blockLight = max(sampleLightmapAxis(vec2(lightUvFromLevels(blockLevel, 0.0).x, LIGHTMAP_MIN)) - lightFloor, vec3(0.0));

        float blockShade = lambert(worldNormal, blockDirection);
        vec3 baseAlbedo = mix(surfaceColor, paletteStartColor, 0.18);
        vec3 skyTint = (paletteEndColor - surfaceColor) * 0.035;
        vec3 blockTint = (paletteEndColor - surfaceColor) * 0.025;
        float skyLightLevel = clamp(maxComponent(skyLight) * 1.75, 0.0, 1.0);
        float blockLightLevel = clamp(maxComponent(blockLight) * 2.1, 0.0, 1.0);
        float skyAmbient = mix(0.10, 0.60, skyLightLevel) * clamp(sunVisibility + moonVisibility * 0.35, 0.0, 1.0);
        float skyDirectionalWeight = mix(0.48, 0.16, skyLightLevel);
        float blockAmbient = mix(0.12, 0.42, blockLightLevel);
        float blockDirectionalWeight = mix(0.82, 0.40, blockLightLevel);
        vec3 skyEnergy = skyLight * min(vec3(skyAmbient + skyDirectional * skyDirectionalWeight), vec3(0.78));
        vec3 blockEnergy = blockLight * min(vec3(blockAmbient + blockShade * blockDirectionalWeight), vec3(0.88));
        vec3 combinedEnergy = min(skyEnergy + blockEnergy, vec3(0.92));

        vec3 lighting = baseAlbedo * combinedEnergy;
        lighting += skyLight * skyDirectional * skyTint * mix(0.85, 0.35, skyLightLevel);
        lighting += blockLight * blockShade * blockTint * mix(0.90, 0.45, blockLightLevel);
        color = vec4(max(lighting, vec3(0.0)), 1.0);
    } else {
        // --- Vanilla block: unchanged ---
        vec2 pixelSize = 1.0 / vec2(TextureSize);
        vec4 texColor = UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, pixelSize)
                                     : sampleNearest(Sampler0, texCoord0, pixelSize);
        vec3 vanillaLight = max(texture(Sampler2, v_lightUv).rgb - lightFloor, vec3(0.0));
        color = vec4(texColor.rgb * v_baseColor.rgb * vanillaLight, texColor.a * v_baseColor.a);
    }

    color = mix(FogColor * vec4(1.0, 1.0, 1.0, color.a), color, ChunkVisibility);
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
                          FogEnvironmentalStart, FogEnvironmentalEnd,
                          FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
