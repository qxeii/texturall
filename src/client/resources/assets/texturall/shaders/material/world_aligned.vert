#include frex:shaders/api/vertex.glsl

out vec3 v_worldPos;

void frx_startVertex(inout frx_VertexData data) {
    v_worldPos = data.vertex.xyz;
}
