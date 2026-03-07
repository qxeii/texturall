#include frex:shaders/api/fragment.glsl
#include frex:shaders/api/sampler.glsl

in vec3 v_worldPos;

// Controls how pronounced the surface detail looks.
// Higher = more bumpy. Hot-reloadable with F3+T.
const float NORMAL_STRENGTH = 4.0;

void frx_materialFragment() {
    vec4 base = texture(frxs_baseColor, frx_texcoord);
    frx_fragColor = vec4(base.rgb, 1.0);

    // Derive normals from height stored in alpha using central differences.
    vec2 ts = 1.0 / vec2(textureSize(frxs_baseColor, 0));
    float hL = texture(frxs_baseColor, frx_texcoord + vec2(-ts.x,  0.0  )).a;
    float hR = texture(frxs_baseColor, frx_texcoord + vec2( ts.x,  0.0  )).a;
    float hD = texture(frxs_baseColor, frx_texcoord + vec2( 0.0,  -ts.y )).a;
    float hU = texture(frxs_baseColor, frx_texcoord + vec2( 0.0,   ts.y )).a;

    vec3 tsNormal = normalize(vec3((hL - hR) * NORMAL_STRENGTH, (hD - hU) * NORMAL_STRENGTH, 1.0));

    // Build TBN from position and UV derivatives — works for any face orientation.
    vec3 N = normalize(frx_fragNormal);
    vec3 dp1  = dFdx(v_worldPos);
    vec3 dp2  = dFdy(v_worldPos);
    vec2 duv1 = dFdx(frx_texcoord);
    vec2 duv2 = dFdy(frx_texcoord);

    float det = duv1.x * duv2.y - duv1.y * duv2.x;
    vec3 T = normalize((dp1 * duv2.y - dp2 * duv1.y) / det);
    vec3 B = normalize(cross(N, T));

    frx_fragNormal = normalize(mat3(T, B, N) * tsNormal);
}
