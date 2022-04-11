attribute vec4 a_position; // projected position of a vertex of the mesh
attribute vec4 a_color; // tint color
attribute vec2 a_texCoord0; // UV coordinates of the tile in the tile atlas.
varying vec2 v_flowPos;//flowPos //Worldspace coordinates, used to look up into the flow map.

uniform sampler2D u_texture;
uniform mat4 u_projTrans;
uniform mat4 u_objToWorldMat;// transforms box2d coordinates to tile coordinates (i.e., 0,0 is the bottom left tile, 1,0 is to the right of that one, etc)

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    // what we do in BW shader:
    v_color = a_color;// OUT.color = IN.color;
    v_texCoords = a_texCoord0;// OUT.texcoord = IN.texcoord;
    gl_Position = u_projTrans * a_position;// OUT.vertex = mul(MVPMatrix, IN.vertex);// Conventional projection & pass-throughs...

    // Save xy world position into flow UV channel.
    v_flowPos = (u_objToWorldMat * a_position).xy;// OUT.flowPos = mul(ObjectToWorldMatrix, IN.vertex).xy;
}