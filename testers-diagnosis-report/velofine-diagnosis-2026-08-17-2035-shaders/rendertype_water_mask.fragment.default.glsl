#version 330


layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};


out vec4 fragColor;

void main() {
    fragColor = ColorModulator;
}
