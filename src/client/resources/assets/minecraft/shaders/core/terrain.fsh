#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec3 u_sunDirection;

const float AMBIENT       = 0.0;
const float NORMAL_STRENGTH = 5.0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 v_worldPos;
in vec2 v_lightUV;

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

float fetchHeight(ivec2 texelXY, ivec2 texSize, ivec2 offset) {
    ivec2 sampleXY = clamp(texelXY + offset, ivec2(0), texSize - 1);
    return texelFetch(Sampler0, sampleXY, 0).a;
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

    if (texColor.a < 0.9999) {
        // --- Texturall block: per-pixel normal-mapped lighting ---


        ivec2 texSize = textureSize(Sampler0, 0);
        ivec2 texelXY = snappedTexelCoord(texCoord0, texSize);
        float hL = fetchHeight(texelXY, texSize, ivec2(-1,  0));
        float hR = fetchHeight(texelXY, texSize, ivec2( 1,  0));
        float hU = fetchHeight(texelXY, texSize, ivec2( 0, -1));
        float hD = fetchHeight(texelXY, texSize, ivec2( 0,  1));

        vec3 tsNormal = normalize(vec3((hL - hR) * NORMAL_STRENGTH,
                                       (hU - hD) * NORMAL_STRENGTH,
                                       1.0));

        vec3 N   = normalize(cross(dFdx(v_worldPos), dFdy(v_worldPos)));
        vec3 dp1 = dFdx(v_worldPos);
        vec3 dp2 = dFdy(v_worldPos);
        vec2 duv1 = dFdx(texCoord0);
        vec2 duv2 = dFdy(texCoord0);
        float det = duv1.x * duv2.y - duv1.y * duv2.x;
        vec3 T = normalize((dp1 * duv2.y - dp2 * duv1.y) / det);
        vec3 B = normalize(cross(N, T));

        vec3 worldNormal = normalize(mat3(T, B, N) * tsNormal);

        vec2 uvBase = clamp(v_lightUV / 256.0 + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0));
        vec3 skyLight = texture(Sampler2, vec2(0.5 / 16.0, uvBase.y)).rgb;
        vec3 blockLight = texture(Sampler2, vec2(uvBase.x, 0.5 / 16.0)).rgb;

        vec3 skyContribution = skyLight * (sunDot * (1.0 - AMBIENT) + AMBIENT);
        vec3 totalLight = clamp(skyContribution + blockLight, 0.0, 1.0);
        color = vec4(totalLight, 1.0);
    } else {
        // --- Vanilla block: unchanged ---
        color = texColor * vertexColor;
    }

    color = mix(FogColor * vec4(1.0, 1.0, 1.0, color.a), color, ChunkVisibility);
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
                          FogEnvironmentalStart, FogEnvironmentalEnd,
                          FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
