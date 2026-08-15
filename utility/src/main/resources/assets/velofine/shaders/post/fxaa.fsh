#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

// Lightweight luma-edge-detection FXAA (the well-documented public FXAA technique, not a copy of
// any specific engine's implementation) - InSize/OutSize come from the standard SamplerInfo UBO
// vanilla's own post-chain passes already provide (confirmed via box_blur.fsh/invert.fsh using the
// same block), so no custom per-resolution uniform needs to be pushed from Java each frame/resize.
const float EDGE_THRESHOLD_MIN = 0.0312;
const float EDGE_THRESHOLD_MAX = 0.125;
const float SUBPIXEL_QUALITY = 0.75;

float luma(vec3 rgb) {
    return dot(rgb, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec2 texel = 1.0 / InSize;
    vec3 colorCenter = texture(InSampler, texCoord).rgb;

    float lumaCenter = luma(colorCenter);
    float lumaDown = luma(texture(InSampler, texCoord + vec2(0.0, -texel.y)).rgb);
    float lumaUp = luma(texture(InSampler, texCoord + vec2(0.0, texel.y)).rgb);
    float lumaLeft = luma(texture(InSampler, texCoord + vec2(-texel.x, 0.0)).rgb);
    float lumaRight = luma(texture(InSampler, texCoord + vec2(texel.x, 0.0)).rgb);

    float lumaMin = min(lumaCenter, min(min(lumaDown, lumaUp), min(lumaLeft, lumaRight)));
    float lumaMax = max(lumaCenter, max(max(lumaDown, lumaUp), max(lumaLeft, lumaRight)));
    float lumaRange = lumaMax - lumaMin;

    if (lumaRange < max(EDGE_THRESHOLD_MIN, lumaMax * EDGE_THRESHOLD_MAX)) {
        fragColor = vec4(colorCenter, 1.0);
        return;
    }

    float lumaDownLeft = luma(texture(InSampler, texCoord + vec2(-texel.x, -texel.y)).rgb);
    float lumaUpRight = luma(texture(InSampler, texCoord + vec2(texel.x, texel.y)).rgb);
    float lumaUpLeft = luma(texture(InSampler, texCoord + vec2(-texel.x, texel.y)).rgb);
    float lumaDownRight = luma(texture(InSampler, texCoord + vec2(texel.x, -texel.y)).rgb);

    float lumaDownUp = lumaDown + lumaUp;
    float lumaLeftRight = lumaLeft + lumaRight;

    float edgeHorizontal = abs(-2.0 * lumaLeft + lumaDownLeft + lumaUpLeft)
            + abs(-2.0 * lumaCenter + lumaDownUp) * 2.0
            + abs(-2.0 * lumaRight + lumaDownRight + lumaUpRight);
    float edgeVertical = abs(-2.0 * lumaUp + lumaUpLeft + lumaUpRight)
            + abs(-2.0 * lumaCenter + lumaLeftRight) * 2.0
            + abs(-2.0 * lumaDown + lumaDownLeft + lumaDownRight);
    bool isHorizontal = edgeHorizontal >= edgeVertical;

    float luma1 = isHorizontal ? lumaDown : lumaLeft;
    float luma2 = isHorizontal ? lumaUp : lumaRight;
    float gradient1 = luma1 - lumaCenter;
    float gradient2 = luma2 - lumaCenter;
    bool is1Steepest = abs(gradient1) >= abs(gradient2);
    float gradientScaled = 0.25 * max(abs(gradient1), abs(gradient2));

    float stepLength = isHorizontal ? texel.y : texel.x;
    float lumaLocalAverage;
    if (is1Steepest) {
        stepLength = -stepLength;
        lumaLocalAverage = 0.5 * (luma1 + lumaCenter);
    } else {
        lumaLocalAverage = 0.5 * (luma2 + lumaCenter);
    }

    vec2 currentUv = texCoord;
    if (isHorizontal) {
        currentUv.y += stepLength * 0.5;
    } else {
        currentUv.x += stepLength * 0.5;
    }

    // Subpixel-only blend (single-tap, no directional edge walk) - a deliberately cheap FXAA
    // variant appropriate for the low-end hardware class Velofine also targets, trading edge-walk
    // quality for a single extra texture fetch per pixel.
    float subpixelBlend = SUBPIXEL_QUALITY * clamp(abs(lumaCenter - lumaLocalAverage) / lumaRange, 0.0, 1.0);
    vec3 blended = texture(InSampler, currentUv).rgb;
    fragColor = vec4(mix(colorCenter, blended, subpixelBlend), 1.0);
}
