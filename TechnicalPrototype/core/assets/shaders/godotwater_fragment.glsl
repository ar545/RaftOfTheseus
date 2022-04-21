#version 120

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform sampler2D u_flowMap;

uniform vec2 u_flowmapSize;
uniform vec2 u_inverseFlowmapSize;

// Scrolling speed for the currents flow.
float waveSpeed = 0.5;

uniform float u_time;

uniform vec3 color_foam = vec3(160.0/255.0, 215.0/255.0, 236.0/255.0);
uniform float thresh_high = 0.814;
uniform vec3 color_crest = vec3(98.0/255.0, 157.0/255.0, 227.0/255.0);
uniform float thresh_mid = 0.431;
uniform vec3 color_sky = vec3(55.0/255.0, 97.0/255.0, 197.0/255.0);
uniform float thresh_low = 0.118;
uniform vec3 color_deep = vec3(36.0/255.0, 58.0/255.0, 186.0/255.0);
uniform float step_smooth = 0.02;

uniform vec4 shadow_color = vec4( 0.038, 0.105, 0.648, 1.0 );
uniform float tile_factor = 0.33;//0.25;
uniform float aspect_ratio = 1.4;//1.6;

uniform sampler2D texture_offset_uv; // set externally
uniform vec2 texture_offset_scale = vec2(0.1, 0.2);
uniform float texture_offset_height = 0.04;
uniform float texture_offset_time_scale = 0.06;

uniform float sine_time_scale = 0.8;//3.0
uniform vec2 sine_offset_scale = vec2(0.8, 0.6);
uniform float sine_wave_size = 0.06;//0.06
uniform float water_height_scale = 0.15;//0.2

uniform sampler2D TEXTURE;
uniform sampler2D NORMAL_TEXTURE;

/*
Returns (x,y) where:
x is water "height", as sampled from the wave texture (offset using flowMap) information
y is water "speed", as sampled from flowMap texture
*/
vec2 waterInfo(vec2 uv, vec2 gridPoint) {
    // calculate UV in flowMap texture (between 0 and 1)
    vec2 flowMapUV = (gridPoint + vec2(0.5, 0.5)) * u_inverseFlowmapSize; // add 0.5 to move to center of a pixel to avoid interpolation
    flowMapUV.y = 1.0 - flowMapUV.y;
    vec2 flowVector = texture2D(u_flowMap, flowMapUV).xy * 2.0 - 1.0; // scale and shift into -1...1 range
    flowVector.y = -flowVector.y;

    // Subtract the flow direction scaled by time to make the wave pattern scroll this way.
    vec2 offsetUV = uv - flowVector * u_time * waveSpeed;

    offsetUV.y *= aspect_ratio;
    offsetUV *= tile_factor;
    float height = texture2D(u_texture, offsetUV).b;
    float speed = dot(flowVector, flowVector); // actually speed squared, but it doesn't matter
    return vec2(height, speed);
}

float foo(float x, float x0) {
    return clamp(((x-x0)/step_smooth + 1.0)*0.5, 0.0, 1.0);
}

vec3 colorFromHeight(float height) {
    float th1 = foo(height, thresh_low);
    float th2 = foo(height, thresh_mid);
    float th3 = foo(height, thresh_high);
    return color_deep*(1.0-th1) + color_sky*(th1-th2) + color_crest*(th2-th3) + color_foam*(th3);
}

vec3 combinedWaterColor(vec2 uv) {
    vec2 flowUV = uv;
    // Clamp to the bottom-left flowmap pixel that influences this location
    vec2 bottomLeft = floor(flowUV - vec2(0.5,0.5)); // pixel coordinates of pixel in flowmap to the bottom-left of flowUV

    // Calculate contributions from the four closest flow map pixels
    vec2 wave00 = waterInfo(flowUV, bottomLeft);
    vec2 wave10 = waterInfo(flowUV, bottomLeft + vec2(1, 0));
    vec2 wave01 = waterInfo(flowUV, bottomLeft + vec2(0, 1));
    vec2 wave11 = waterInfo(flowUV, bottomLeft + vec2(1, 1));

    vec2 smerp = flowUV - (bottomLeft + vec2(0.5, 0.5));
    smerp = smerp*smerp*(3.0 - 2.0*smerp);
    vec2 wave_0 = mix(wave00, wave10, smerp.x);
    vec2 wave_1 = mix(wave01, wave11, smerp.x);
    vec2 waterCombined = mix(wave_0, wave_1, smerp.y);

    float height = waterCombined.x;//0...1
    float speed = waterCombined.y;//0...1

    // increase "height" by "speed" to make currents more visible
    height = min(height/(1.01-speed*0.5), mix(height, 1.0, speed));

    return colorFromHeight(height);
}

void main() {
    vec2 UV = v_texCoords * u_flowmapSize; // UV coordinates of this pixel in the unscaled texture, between 0 and level size

    vec2 base_uv_offset = UV * texture_offset_scale;
    base_uv_offset += u_time * texture_offset_time_scale;

    vec2 offset_texture_uv = texture2D(texture_offset_uv, base_uv_offset).rg * 2.0 - 1.0;
    vec2 texture_based_offset = offset_texture_uv * texture_offset_height;

    vec2 adjusted_uv = UV + texture_based_offset;

    adjusted_uv.x += sin(u_time * sine_time_scale + (adjusted_uv.x + adjusted_uv.y) * sine_offset_scale.x) * sine_wave_size;
    adjusted_uv.y += cos(u_time * sine_time_scale + (adjusted_uv.x + adjusted_uv.y) * sine_offset_scale.y) * sine_wave_size;

    float sine_wave_height = sin(u_time * sine_time_scale + (adjusted_uv.x + adjusted_uv.y) * sine_offset_scale.y);
    float water_height = ( sine_wave_height + offset_texture_uv.g ) * water_height_scale;

    vec4 c = vec4(combinedWaterColor(adjusted_uv), 1.0);
//    gl_FragColor = c;
    //	gl_FragColor = vec4(vec2(offset_texture_uv), 0.0, 1.0);
    gl_FragColor = mix(c, shadow_color, water_height);
    //	gl_FragColor = vec4(vec2(adjusted_uv), 0.0, 1.0);
    // TODO figure out how to do normal mapping in GLSL
//    NORMALMAP = texture2D(NORMAL_TEXTURE, adjusted_uv / 5.0).rgb;
    //	gl_FragColor = texture2D(NORMAL_TEXTURE, adjusted_uv / 5.0);
}
