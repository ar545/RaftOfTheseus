#version 120

/* We shift the vertices to make items appear as if they're bobbing up and down */

attribute vec4 a_position;
attribute vec2 a_texCoord0;
uniform mat4 u_projTrans;
varying vec2 v_texCoords;

uniform float bobbing_amplitude;
uniform float bobbing_timescale;
uniform float time;

void main() {
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * (a_position + bobbing_amplitude*sin(bobbing_timescale*time)*vec4(0.0,1.0,0.0,0.0));
}