#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:light.glsl>
#moj_import <minecraft:chunksection.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 v_baseColor;
in vec2 v_lightUv;
in vec3 v_worldPos;
in vec3 v_faceNormal;

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

void main() {
    vec2 pixelSize = 1.0 / vec2(TextureSize);
    vec4 texColor = UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, pixelSize)
                                 : sampleNearest(Sampler0, texCoord0, pixelSize);

#ifdef ALPHA_CUTOUT
    if (texColor.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

    vec4 color;
    int materialId = int(round(v_baseColor.a * 255.0));

    if (materialId > 0 && materialId < 5) {
        vec3 faceNormal = axisAlignedNormal(normalize(v_faceNormal));
        mat3 tbn = faceTbn(faceNormal);
        ivec2 texSize = textureSize(Sampler0, 0);
        ivec2 normalTexel = snappedTexelCoord(texCoord0, texSize);
        vec3 tsNormal = decodeNormal(texelFetch(Sampler0, normalTexel, 0));
        vec3 worldNormal = normalize(tbn * tsNormal);

        vec3 sunDirection = normalizeOr(Light0_Direction, vec3(0.0, 1.0, 0.0));
        vec3 moonDirection = normalizeOr(Light1_Direction, -sunDirection);

        float sunShade = lambert(worldNormal, sunDirection) * horizonFade(sunDirection);
        float moonShade = lambert(worldNormal, moonDirection) * horizonFade(moonDirection);

        vec3 lightFloor = sampleLightmapAxis(vec2(LIGHTMAP_MIN));
        vec3 skyLight = max(sampleLightmapAxis(vec2(LIGHTMAP_MIN, v_lightUv.y)) - lightFloor, vec3(0.0));
        vec3 blockLight = max(sampleLightmapAxis(vec2(v_lightUv.x, LIGHTMAP_MIN)) - lightFloor, vec3(0.0));

        // The lightmap does not expose local block-light vectors, so use a stable face-local key direction.
        vec3 blockDirection = normalize(tbn * normalize(vec3(0.35, 0.45, 0.82)));
        float blockShade = clamp(dot(worldNormal, blockDirection) * 0.35 + 0.65, 0.0, 1.0);

        vec3 lighting = skyLight * max(sunShade, moonShade) + blockLight * blockShade;
        color = vec4(v_baseColor.rgb * lighting, 1.0);
    } else {
        // --- Vanilla block: unchanged ---
        color = texColor * vertexColor;
    }

    color = mix(FogColor * vec4(1.0, 1.0, 1.0, color.a), color, ChunkVisibility);
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
                          FogEnvironmentalStart, FogEnvironmentalEnd,
                          FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
