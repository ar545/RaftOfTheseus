#version 120

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D mask;

uniform float bobbing_amplitude = 0.25;
uniform float bobbing_timescale = 1.5;
uniform float mask_sweep_timescale = -0.25;
uniform float time;

void main() {
    vec4 color = texture2D(u_texture, v_texCoords).rgba;
    float a = color.a;
    vec2 shifted_uv = v_texCoords - 0.3333*bobbing_amplitude*sin(bobbing_timescale*time)*vec2(0.0, 1.0) + vec2(mask_sweep_timescale*time, 0.0);
    if (texture2D(mask, shifted_uv).r < 0.5)
        a *= 0.33;
    gl_FragColor = vec4(color.rgb, a);
}
