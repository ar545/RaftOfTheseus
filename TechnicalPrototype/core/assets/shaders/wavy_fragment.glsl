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

uniform vec2 u_inverseFlowmapSize;// = vec2(1.0f/10.0f, 1.0f/8.0f);

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
float waveSpeed  = 0.5f;

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
    vec2 sampleShifted = (sampleSite + vec2(0.5, 0.5)) * u_inverseFlowmapSize;// add 0.5 to move to center of a pixel to avoid interpolation
//    sampleShifted.y = 1.0 - sampleShifted.y;// flip y because textures have their origin in the etc etc corner
    vec2 flowVector = texture2D(u_flowMap, sampleShifted).xy * 2.0f - 1.0f;
    float flowSpeed = dot(flowVector, flowVector);

    // I displace the UVs a little for each sample, so that adjacent
    // tiles flowing the same direction don't repeat exactly.
    vec2 waveUV = uv * waveDensity + 0.1*sin((3.3f * sampleSite.xy + sampleSite.yx) * 1.0f + u_time*0.50);

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
//    vec2 offset = uv - sampleSite;
//    float fade = 1.0 - clamp(dot(offset, offset), 0, 1);

//    return vec2(wave * fade, fade);
//    return wave;
    return vec2(wave, flowSpeed);

//    waveUV -= floor(waveUV-0.5) + 0.5;
//    return vec3(waveUV, flowSpeed);
}

void main() {
    vec4 c = texture2D(u_texture, v_texCoords).rgba;//vec4 c = tex2D(_MainTex, IN.texcoord);// Sample the tilemap texture.
//    vec4 c = v_color;

    // In my case, I just select the water areas based on
    // how blue they are. A more robust method would be
    // to encode this into an alpha mask or similar.
//    float waveBlend = clamp(3.0f * ((2*c.b-0.1)/(c.r+c.g+0.1)), 0, 1);
//    float waveBlend = 1;

    // Skip the water effect if we're not in water.
//    if(waveBlend == 0.0f) {
//        gl_FragColor = c * v_color;
//        return;
//    }

    vec2 flowUV = v_flowPos;// + vec2(0.5, 0.5);
    // Clamp to the bottom-left flowmap pixel
    // that influences this location.
    vec2 bottomLeft = floor(flowUV - vec2(0.5,0.5)); // pixel coordinates of pixel in flowmap to the bottom-left of flowUV

    // Sum up the wave contributions from the four
    // closest flow map pixels.
//    vec2 wave = WaveAmount(flowUV, bottomLeft);
//    wave += WaveAmount(flowUV, bottomLeft + vec2(1, 0));
//    wave += WaveAmount(flowUV, bottomLeft + vec2(1, 1));
//    wave += WaveAmount(flowUV, bottomLeft + vec2(0, 1));
    vec2 wave00 = WaveAmount(flowUV, bottomLeft);
    vec2 wave10 = WaveAmount(flowUV, bottomLeft + vec2(1, 0));
    vec2 wave01 = WaveAmount(flowUV, bottomLeft + vec2(0, 1));
    vec2 wave11 = WaveAmount(flowUV, bottomLeft + vec2(1, 1));

    vec2 smerp = flowUV - (bottomLeft + vec2(0.5, 0.5));
    smerp = smerp*smerp*(3.0 - 2.0*smerp);
    vec2 wave_0 = mix(wave00, wave10, smerp.x);
    vec2 wave_1 = mix(wave01, wave11, smerp.x);
    vec2 wave = mix(wave_0, wave_1, smerp.y);

//    float waveTex = texture2D(u_waveTexture, wave.xy).r;
    float waterSpeed = wave.y;

//    vec2 waveUV = flowUV * waveDensity + 0.1*sin(u_time*0.50) - wave.xy * u_time * waveSpeed;
//    float waveTex = texture2D(u_waveTexture, waveUV).r;
    float waveTex = wave.x;

    // Here I tint the "low" parts a darker blue.
//    c = mix(c, c*c + vec4(0, 0, 0.05, 0), waveBlend * 0.75f * clamp(1.2f - 4.0f * wave.x, 0, 1));
    c = mix(c, c*c + vec4(0, 0, 0.05, 0), 0.75f * clamp(1.2f - 4.0f * waveTex, 0, 1));

    // Then brighten the peaks. (more brightness for higher flow speed)
//    c += waveBlend * clamp((wave.x - 0.4f) * 2.0f, 0, 1) * (0.2 + wave.y*0.5);
//    c += waveBlend * clamp((wave - 0.4f) * 2.0f, 0, 1) * 0.3f;
    c += clamp((waveTex - 0.4f) * 2.0f, 0, 1) * (0.2 + waterSpeed*0.5);

    // And finally return the tinted colour.
    gl_FragColor = c*v_color;
}
