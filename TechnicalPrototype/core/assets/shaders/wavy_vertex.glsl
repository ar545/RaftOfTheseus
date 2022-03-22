// Colour texture / atlas for my tileset.
sampler2D _Tile;
// Flowmap texture.
sampler2D _Flow;
// Wave surface texture.
sampler2D _Wave;

// Tiling of the wave pattern texture.
float _WaveDensity = 0.5f;
// Scrolling speed for the wave flow.
float _WaveSpeed  = 5.0f;

// Scaling from my world size of 8x8 tiles
// to the 0...1
//vec2 inverseFlowmapSize = vec2(1.0f/8.0f);
vec2 inverseFlowmapSize = vec2(1.0f/10.0f, 1.0f/8.0f);


// vertex shader 2 fragment shader "connector" struct, I think
struct v2f
{
// Projected position of tile vertex.
    vec4 vertex;//   : SV_POSITION;
// Tint colour (not used in this effect, but handy to have.
    vec4 color;//    : COLOR;
// UV coordinates of the tile in the tile atlas.
    vec2 texcoord;// : TEXCOORD0;
// Worldspace coordinates, used to look up into the flow map.
    vec2 flowPos;//  : TEXCOORD1;
};

attribute vec4 a_position;//vertex //Projected position of tile vertex.
attribute vec4 a_color;//color //Tint colour (not used in this effect, but handy to have.
attribute vec2 a_texCoord0;//texcoord //UV coordinates of the tile in the tile atlas.
attribute vec2 a_texCoord1;//flowPos //Worldspace coordinates, used to look up into the flow map.

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    // Save xy world position into flow UV channel.
    OUT.flowPos = mul(ObjectToWorldMatrix, IN.vertex).xy;

    // Conventional projection & pass-throughs...
    OUT.vertex = mul(MVPMatrix, IN.vertex);



    // what we do in BW shader:
    v_color = a_color;// OUT.color = IN.color;
    v_texCoords = a_texCoord0;// OUT.texcoord = IN.texcoord; // is texcoord0
    gl_Position = u_projTrans * a_position;
}

// I use this function to sample the wave contribution
// from each of the 4 closest flow map pixels.
// uv = my uv in world space
// sample site = world space
vec2 WaveAmount(vec2 uv, vec2 sampleSite) {
    // Sample from the flow map texture without any mipmapping/filtering.
    // Convert to a vector in the -1...1 range.
    vec2 flowVector = tex2Dgrad(_Flow, sampleSite * inverseFlowmapSize, 0, 0).xy
    * 2.0f - 1.0f;
    // Optionally, you can skip this step, and actually encode
    // a flow speed into the flow map texture too.
    // I just enforce a 1.0 length for consistency without getting fussy.
    flowVector = normalize(flowVector);

    // I displace the UVs a little for each sample, so that adjacent
    // tiles flowing the same direction don't repeat exactly.
    vec2 waveUV = uv * _WaveDensity + sin((3.3f * sampleSite.xy + sampleSite.yx) * 1.0f);

    // Subtract the flow direction scaled by time
    // to make the wave pattern scroll this way.
    waveUV -= flowVector * _Time * _WaveSpeed;

    // I use tex2DGrad here to avoid mipping down
    // undesireably near tile boundaries.
    float wave = tex2Dgrad(_Wave, waveUV,
    ddx(uv) * _WaveDensity, ddy(uv) * _WaveDensity);

    // Calculate the squared distance of this flowmap pixel center
    // from our drawn position, and use it to fade the flow
    // influence smoothly toward 0 as we get further away.
    vec2 offset = uv - sampleSite;
    float fade = 1.0 - saturate(dot(offset, offset));

    return vec2(wave * fade, fade);
}

vec4 Frag(v2f IN) : SV_Target
{
// Sample the tilemap texture.
    vec4 c = tex2D(_MainTex, IN.texcoord);

// In my case, I just select the water areas based on
// how blue they are. A more robust method would be
// to encode this into an alpha mask or similar.
    float waveBlend = saturate(3.0f * (c.b - 0.4f));

// Skip the water effect if we're not in water.
    if(waveBlend == 0.0f)
    return c * IN.color;

    vec2 flowUV = IN.flowPos;
// Clamp to the bottom-left flowmap pixel
// that influences this location.
    vec2 bottomLeft = floor(flowUV);

// Sum up the wave contributions from the four
// closest flow map pixels.
    vec2 wave = WaveAmount(flowUV, bottomLeft);
    wave += WaveAmount(flowUV, bottomLeft + vec2(1, 0));
    wave += WaveAmount(flowUV, bottomLeft + vec2(1, 1));
    wave += WaveAmount(flowUV, bottomLeft + vec2(0, 1));

// We store total influence in the y channel,
// so we can divide it out for a weighted average.
    wave.x /= wave.y;

// Here I tint the "low" parts a darker blue.
    c = lerp(c, c*c + vec4(0, 0, 0.05, 0), waveBlend * 0.5f * saturate(1.2f - 4.0f * wave.x));

// Then brighten the peaks.
    c += waveBlend * saturate((wave.x - 0.4f) * 20.0f) * 0.1f;

// And finally return the tinted colour.
    return c * IN.color;
}