#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_flowPos;

uniform sampler2D u_texture;
uniform sampler2D u_flowMap;
uniform mat4 u_projTrans;
uniform mat4 u_objToWorldMat;// transforms box2d coordinates to tile coordinates (i.e., 0,0 is the bottom left tile, 1,0 is to the right of that one, etc)

// Scaling from my world size of 10x8 tiles
// to the 0...1
vec2 inverseFlowmapSize = vec2(1.0f/10.0f, 1.0f/8.0f);

//// Colour texture / atlas for my tileset.
//sampler2D _Tile;
//uniform sampler2D u_tileTexture;
//// Flowmap texture.
//sampler2D _Flow;
// Wave surface texture.
uniform sampler2D u_waveTexture;

// Tiling of the wave pattern texture.
float waveDensity = 0.5f;
// Scrolling speed for the wave flow.
float waveSpeed  = 5.0f;

uniform float u_time;

// I use this function to sample the wave contribution
// from each of the 4 closest flow map pixels.
// uv = my uv in world space
// sample site = world space
vec2 WaveAmount(vec2 uv, vec2 sampleSite) {
    // Sample from the flow map texture without any mipmapping/filtering.
    // Convert to a vector in the -1...1 range.
//    vec2 flowVector = tex2Dgrad(_Flow, sampleSite * inverseFlowmapSize, 0, 0).xy
//    * 2.0f - 1.0f;

    vec2 flowVector = texture2D(u_flowMap, sampleSite * inverseFlowmapSize).xy * 2.0f - 1.0f;


    // Optionally, you can skip this step, and actually encode
    // a flow speed into the flow map texture too.
    // I just enforce a 1.0 length for consistency without getting fussy.
    flowVector = normalize(flowVector);

    // I displace the UVs a little for each sample, so that adjacent
    // tiles flowing the same direction don't repeat exactly.
    vec2 waveUV = uv * waveDensity + sin((3.3f * sampleSite.xy + sampleSite.yx) * 1.0f);

    // Subtract the flow direction scaled by time
    // to make the wave pattern scroll this way.
    waveUV -= flowVector * u_time * waveSpeed;

    // I use tex2DGrad here to avoid mipping down
    // undesireably near tile boundaries.
//    float wave = tex2Dgrad(u_waveTexture, waveUV, ddx(uv) * waveDensity, ddy(uv) * waveDensity);
    float wave = texture2D(u_waveTexture, waveUV).r;

    // Calculate the squared distance of this flowmap pixel center
    // from our drawn position, and use it to fade the flow
    // influence smoothly toward 0 as we get further away.
    vec2 offset = uv - sampleSite;
    float fade = 1.0 - clamp(dot(offset, offset), 0, 1);

    return vec2(wave * fade, fade);
}

void main() {
    vec4 c = texture2D(u_texture, v_texCoords).rgba;//vec4 c = tex2D(_MainTex, IN.texcoord);// Sample the tilemap texture.
//    vec4 c = v_color;

    // In my case, I just select the water areas based on
    // how blue they are. A more robust method would be
    // to encode this into an alpha mask or similar.
    float waveBlend = clamp(3.0f * (c.b - 0.4f), 0, 1);

    // Skip the water effect if we're not in water.
    if(waveBlend == 0.0f) {
        gl_FragColor = c * v_color;
        return;
    }

    vec2 flowUV = v_flowPos;
    // Clamp to the bottom-left flowmap pixel
    // that influences this location.
    vec2 bottomLeft = floor(flowUV);



    if(true) {
        //        gl_FragColor = vec4(1,0,0,1);
        gl_FragColor = vec4(flowUV.x/100,flowUV.y/100,0.0f,c.a);
        return;
    }

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
    c = mix(c, c*c + vec4(0, 0, 0.05, 0), waveBlend * 0.5f * clamp(1.2f - 4.0f * wave.x, 0, 1));

    // Then brighten the peaks.
    c += waveBlend * clamp((wave.x - 0.4f) * 20.0f, 0, 1) * 0.1f;

    // And finally return the tinted colour.
    gl_FragColor = c*v_color;//return c * v_color;
}
