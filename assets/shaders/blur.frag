#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2  u_resolution;
uniform float u_blurRadius;
uniform float u_darkness;

void main() {
    vec2 texel = vec2(1.0) / u_resolution;
    vec4 sum = vec4(0.0);
    float samples = 0.0;

    for (float x = -4.0; x <= 4.0; x += 1.0) {
        for (float y = -4.0; y <= 4.0; y += 1.0) {
            vec2 offset = vec2(x, y) * texel * u_blurRadius;
            sum += texture2D(u_texture, v_texCoords + offset);
            samples += 1.0;
        }
    }

    vec4 color = sum / samples;
    color.rgb *= (1.0 - u_darkness);

    gl_FragColor = vec4(color.rgb, 1.0) * v_color;
}
