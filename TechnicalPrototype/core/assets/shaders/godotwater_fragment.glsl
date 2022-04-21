#version 120

#ifdef GL_ES
precision mediump float;
#endif

#define PI 3.1415926538

varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform sampler2D u_normalTexture;
uniform sampler2D u_flowMap;

uniform vec2 u_flowmapSize;
uniform vec2 u_inverseFlowmapSize;

// Scrolling speed for the currents flow.
float waveSpeed = 2.0;
float current_blend = 0.2;//0...0.5

uniform float u_time;

uniform vec3 color_foam = vec3(160.0/255.0, 215.0/255.0, 236.0/255.0);
uniform float thresh_high = 0.6;//0.814;
uniform vec3 color_crest = vec3(98.0/255.0, 157.0/255.0, 227.0/255.0);
uniform float thresh_mid = 0.15;//0.431;
uniform vec3 color_sky = vec3(55.0/255.0, 97.0/255.0, 197.0/255.0);
uniform float thresh_low = 0.07;//0.118;
uniform vec3 color_deep = vec3(36.0/255.0, 58.0/255.0, 186.0/255.0);
uniform float step_smooth = 0.00002;

//uniform vec4 shadow_color = vec4( 0.038, 0.105, 0.648, 1.0 );
uniform float tile_factor = 0.25;//0.25;
uniform float aspect_ratio = 1.4;//1.6;

uniform sampler2D texture_offset_uv; // set externally
uniform vec2 texture_offset_scale = vec2(0.1, 0.2);
uniform float texture_offset_height = 0.04;
uniform float texture_offset_time_scale = 0.03;

uniform vec2 sine_wave_velocity = vec2(-0.4, -0.5);
uniform float sine_time_scale = 0.6;//3.0
//uniform vec2 sine_offset_scale = vec2(0.7, 0.7);
uniform float sine_wave_size = 0.18;//0.06
//uniform float water_height_scale = 0.15;//0.2

// water reflection calculations:
const float camera_angle = 0.74*(PI*0.5); // angle to the vertical, i.e. 0 is looking straight down, pi/2 is looking totally horizontal
const vec3 incoming_ray = vec3(0.0, sin(camera_angle), -cos(camera_angle)); // a ray pointing from the "camera" towards the scene

/*
Returns (x,y) where:
x is water "height", as sampled from the wave texture (offset using flowMap) information, in 0...1
y is water "speed", as sampled from flowMap texture, in 0...1
(z, w) is the water normal vector, in the range -1...1
*/
vec4 waterInfo(vec2 uv, vec2 gridPoint) {
    // calculate UV in flowMap texture (between 0 and 1)
    vec2 flowMapUV = (gridPoint + vec2(0.5, 0.5)) * u_inverseFlowmapSize; // add 0.5 to move to center of a pixel to avoid interpolation
    flowMapUV.y = 1.0 - flowMapUV.y;
    vec2 flowVector = texture2D(u_flowMap, flowMapUV).xy * 2.0 - 1.0; // scale and shift into -1...1 range
    flowVector.y = -flowVector.y;

    // Subtract the flow direction scaled by time to make the wave pattern scroll this way.
    vec2 offsetUV = uv - flowVector * u_time * waveSpeed + vec2(-u_time*0.05);

    // displacement
    vec2 base_uv_offset = offsetUV * texture_offset_scale;
    base_uv_offset += u_time * texture_offset_time_scale;
    vec2 offset_texture_uv = texture2D(texture_offset_uv, base_uv_offset).rg * 2.0 - 1.0;
    vec2 texture_based_offset = offset_texture_uv * texture_offset_height;
    offsetUV += texture_based_offset;

    offsetUV.y *= aspect_ratio;
    offsetUV *= tile_factor;
    float height = texture2D(u_texture, offsetUV).b;
    float speed = dot(flowVector, flowVector); // actually speed squared, but it doesn't matter
    vec3 normal = texture2D(u_normalTexture, offsetUV).rgb * 2.0 - 1.0;
    vec2 normal_xy = normal.xy/normal.z + texture_based_offset;
    return vec4(height, speed, normal_xy);
}

vec3 colorFromHeight(float height, vec2 normal_xy, vec2 normal_adjustment_xy) {
    vec3 normal = normalize(vec3(normal_xy, 1.0));
    vec3 normal_adjustment = normalize(vec3(normal_adjustment_xy, 1.0));
    normal = normalize(mix(normal, normal_adjustment, 0.5));
    float cos_ang = -dot(normal, incoming_ray);// cosine of angle between normal and incoming ray (angle of incidence)
    // proportion of light power reflected from sky into camera (using Schlick's Approximation), assuming diffuse lighting only (eg. cloudy sky, no sun):
    float reflection = 0.02 + 0.98*pow(1.0-cos_ang, 5.0);
    float value = mix(height, reflection, 1-height*height);
    float th1 = step(thresh_low, value);
    float th2 = step(thresh_mid, value);
    float th3 = step(thresh_high, value);
    return color_deep*(1.0-th1) + color_sky*(th1-th2) + color_crest*(th2-th3) + color_foam*(th3);
//    return (normal+1.0)*0.5;
//    return vec3(reflection);
}

vec3 combinedWaterColor(vec2 uv, vec2 uv_adjustment, vec2 normal_adjustment) {
    vec2 flowUV = uv;
    // Clamp to the bottom-left flowmap pixel that influences this location
    vec2 bottomLeft = floor(flowUV - vec2(0.5,0.5)); // pixel coordinates of pixel in flowmap to the bottom-left of flowUV

    // Calculate contributions from the four closest flow map pixels
    vec2 flowUV_adjusted = flowUV + uv_adjustment;
    vec4 wave00 = waterInfo(flowUV_adjusted, bottomLeft);
    vec4 wave10 = waterInfo(flowUV_adjusted, bottomLeft + vec2(1, 0));
    vec4 wave01 = waterInfo(flowUV_adjusted, bottomLeft + vec2(0, 1));
    vec4 wave11 = waterInfo(flowUV_adjusted, bottomLeft + vec2(1, 1));

    vec2 smerp = flowUV - (bottomLeft + vec2(0.5, 0.5));
//    smerp = smerp*smerp*(3.0 - 2.0*smerp);
//    smerp = smerp*smerp*(3.0 - 2.0*smerp);
    smerp = smoothstep(0.5-current_blend, 0.5+current_blend, smerp);
    vec4 wave_0 = mix(wave00, wave10, smerp.x);
    vec4 wave_1 = mix(wave01, wave11, smerp.x);
    vec4 waterCombined = mix(wave_0, wave_1, smerp.y);

    float height = waterCombined.x;//0...1
    float speed = waterCombined.y;//0...1
    vec2 normal = waterCombined.zw;//-1...1

    // increase "height" by "speed" to make currents more visible
    height = min(height/(1.01-speed*0.5), mix(height, 1.0, speed));
    normal *= speed + 0.5;

    return colorFromHeight(height, normal, normal_adjustment);
}

void main() {
    vec2 uv = v_texCoords * u_flowmapSize; // UV coordinates of this pixel in the unscaled texture, between 0 and level size

//    vec2 base_uv_offset = uv * texture_offset_scale;
//    base_uv_offset += u_time * texture_offset_time_scale;

//    vec2 offset_texture_uv = texture2D(texture_offset_uv, base_uv_offset).rg * 2.0 - 1.0;
//    vec2 texture_based_offset = offset_texture_uv * texture_offset_height;

    float arg = -u_time * sine_time_scale + dot(uv, sine_wave_velocity);
    float sine_wave = sin(arg) * sine_wave_size;
    vec2 sine_wave_gradient = cos(arg) * sine_wave_velocity * sine_wave_size;
    vec2 sine_wave_offset = vec2(0.0, sine_wave);
//    vec2 normal_adjustment = texture_based_offset + sine_wave_gradient;
//    vec2 uv_adjustment = texture_based_offset + sine_wave_offset;
    vec2 normal_adjustment = sine_wave_gradient;
    vec2 uv_adjustment = sine_wave_offset;

    gl_FragColor = vec4(combinedWaterColor(uv, uv_adjustment, normal_adjustment), 1.0);
}
