#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform mat4 u_projTrans;
uniform float f_test;

void main() {
    vec4 color = texture2D(u_texture, v_texCoords).rgba;
    float gray = (color.r + color.g + color.b) / 3.0;
    vec3 grayscale = vec3(gray);

    gl_FragColor = color.b > f_test ? color : vec4(grayscale, color.a);
}