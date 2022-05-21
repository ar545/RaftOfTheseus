#version 120

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D mask;

uniform float bobbing_amplitude;
uniform float bobbing_timescale = 1.5;
uniform float mask_sweep_timescale = -0.25;
uniform float time;

uniform vec2 film_strip_dims; // how many frames wide / high
uniform float height;

varying float pos_x;

void main() {
    vec4 color = texture2D(u_texture, v_texCoords).rgba;
    float a = color.a;
    vec2 uv = v_texCoords * film_strip_dims;
    uv.y -= int(uv.y);
    uv.y -= height + 0.3333 * bobbing_amplitude * sin(bobbing_timescale*time);
    uv.x = 0.3333 * pos_x + mask_sweep_timescale * sin(time);
    if (texture2D(mask, uv).r < 0.5)
        a *= 0.33;
    gl_FragColor = vec4(color.rgb, a);
}
