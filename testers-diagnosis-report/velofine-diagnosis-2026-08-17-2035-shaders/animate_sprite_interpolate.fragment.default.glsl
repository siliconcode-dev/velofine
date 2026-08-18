#version 330


layout(std140) uniform SpriteAnimationInfo {
    mat4 ProjectionMatrix;
    mat4 SpriteMatrix;
    float UPadding;
    float VPadding;
    int MipMapLevel;
};


uniform sampler2D CurrentSprite;
uniform sampler2D NextSprite;

in float fAnimationProgress;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 currentColor = textureLod(CurrentSprite, texCoord0, MipMapLevel);
    vec4 nextColor = textureLod(NextSprite, texCoord0, MipMapLevel);
    fragColor = mix(currentColor, nextColor, fAnimationProgress);
}
