#version 120

// ========================================================..======================================================== //
uniform sampler2D u_texture;
varying vec2 v_texCoords;

/*
A generic moving wave with speed s and wavelength w is sin(2*PI*(s*t-x)/w).
*/

// =================================================== CONSTANTS  =================================================== //

// mathematical constants
#define PI 3.1415926538

// camera positioning (for better "perspective")
const float camera_angle = 0.5*(PI/2.0); // camera angle to vertical axis. a=0 -> view from above, a=pi/2 -> view from side.
// (derived constants)
const vec3 camera_ray = vec3(0.0, sin(camera_angle), -cos(camera_angle)); // points from camera to scene
const float camera_slope = -tan(camera_angle);
const float aspect_ratio = 1.0/cos(camera_angle);

// color display thresholds
const float thresh_high = 0.16;
const float thresh_mid = 0.03;
const float thresh_low = 0.0225;

// big rolling wave parameters
const float big_wave_amplitude = 0.3;
const float big_wave_speed = 1.0; // in tile widths per second
const float big_wave_length = 12.0; // as a multiple of 1 tile width
const float big_wave_angle = -0.5*PI/2.0; // angle of direction in which the wave moves: 0 is east, pi/2 is north, etc.
// (derived constants)
const vec2 big_wave_direction = vec2(cos(big_wave_angle), sin(big_wave_angle));
const float big_wave_freq = 2.0*PI*big_wave_speed/big_wave_length;
const vec2 big_wave_velocity = -2.0*PI*big_wave_direction/big_wave_length; // not really "velocity" per se but this makes the math simpler

// textured wave parameters
const float texture_wave_amplitude = 0.3;
const float tile_factor = 0.25; // the wave texture is displayed 1/tile_factor level tiles wide

// current animation parameters
const float current_speed = 2.0; // scroll speed multiplier
const float current_blend = 0.2; // radius in which current animation is blended. Must be in [0,0.5].

// texture-based displacement parameters
const float texture_offset_height = 0.02;
const vec2 texture_offset_scale = vec2(2.0, 2.0);
const float texture_offset_time_scale = 0.03;

// surf parameters
const float surf_amplitude = 1.75;
const float surf_speed = 0.08; // in tile widths per second
const float surf_wavelength = 0.10; // as a multiple of 1 tile width
const float surf_range = 0.25; // surf which is more than this distance away from a shore won't show up

// wake parameters
const float wake_amplitude = 0.025;
const int wake_samples_number = 8; // number of samples which are tracked (older samples are discarded). TODO: make sure this matches WorldController
const float wake_sample_period = 0.15; // interval between sample times, in seconds. TODO: make sure this matches WorldController
const float wake_wavelet_wavelength = 0.75; // wavelength of circle wavelets emanating from samples, in tile units
// (derived constants)
const float wake_wavelet_freq = 2.0/wake_wavelet_wavelength;

// =============================================== IMPORTED UNIFORMS  =============================================== //

uniform float time; // time in seconds
uniform vec2 level_size; // dimensions of the level, in tiles
uniform sampler2D normal_texture; // represents 3D contours of texture wave
uniform sampler2D surf_map; // a map of how close the shoreline is at any point
uniform sampler2D flow_map; // a map of the current direction and magnitude at any point
uniform sampler2D texture_offset_uv; // displacement map for texture-based displacements
uniform float surf_map_res = 2.0; // ratio of surf_map dimensions to level dimensions in tiles

// color palette
uniform vec3 colors[4];

// raft position and speed samples
uniform vec2 wake_samples_pos[wake_samples_number]; // raft position, in tile units, at each sample time
uniform float wake_samples_speed[wake_samples_number]; // raft speed, in tile units per second, at each sample time
uniform float wake_sample_latency; // time since latest sample, in seconds

// ================================================ HELPER FUNCTIONS ================================================ //

/* Converts a value (corresponding roughly to "proportion of reflected sunlight") into a color to be displayed. */
vec3 color_from_value(float value) {
    float th1 = step(thresh_low, value); // using step() instead of smoothstep() to display discrete color bands
    float th2 = step(thresh_mid, value);
    float th3 = step(thresh_high, value);
    return colors[3]*(1.0-th1) + colors[2]*(th1-th2) + colors[1]*(th2-th3) + colors[0]*(th3);
}

/* Combines water height and water reflection amount into one value. */
float value_from_h_and_r(float height, float reflection) {
    return reflection + height*0.01;
}

/* Finds an approximate solution x to the equation x-a=b*sin(x) using Newton's Method. Assumes |b|<1.
 Note that if b is close to 1 and a is close to 0 mod 2*PI, then Newton's Method will fail, creating visual artifacts. */
float solve_equation(float a, float b) {
    float x = a; // initial guess x = a
    x -= (b*sin(x)-x+a)/(b*cos(x)-1.0); // first iteration of Newton's Method
    // x -= (b*sin(x)-x+a)/(b*cos(x)-1); // second iteration of Newton's Method (unnecessary if |b| <= 1/3)
    return x;
}

// ========================================= PART 0. COORDINATE CONVERSION  ========================================= //

/* Finds the world coordinates of this pixel. */
vec2 get_coords() {
    vec2 pos = v_texCoords; // uv coordinates of this pixel (aka fragment). (0,0) is the upper-left corner of the level, (1,1) is the lower-right corner of the level.
    pos.y = 1.0 - pos.y; // flip vertically
    pos *= level_size; // scale to level size
    pos.y *= aspect_ratio; // project onto the flat plane (z = water height = 0) using the camera angle.
    return pos;
}

// ============================================ PART 1. BIG ROLLING WAVE ============================================ //

/* Finds the approximate (x,y,z) world coordinates at which the view ray intersects the water, assuming that water
 height (z) is predominantly determined by the big rolling wave. */
vec3 find_water_intersect_point(vec2 coords) {
    // the point where the view ray intersects the plane z=0:
    vec2 root_uv = coords;
    /*
    We want to find the point (x, y) where the view ray intersects the big wave. This lengthy comment explains the math.

    The view ray can be described parametrically as (x,y,z) = (root_uv.x, root_uv.y, 0) + lambda*camera_ray, for any
    value lambda. This means that any point (x,y,z) along the ray must satisfy:
    x = root_uv.x
    (y-root_uv.y)/camera_ray.y = z/camera_ray.z = lambda
    We don't care about lambda, so the second equation just relates y and z. It can be rewritten as:
    y = (camera_ray.y/camera_ray.z)*z + root_uv.y = camera_slope*z + root_uv.y

    The big wave can be described as z = A*sin(F*t + (x,y) @ D). A is wave height, F is wave frequency, t is time, and
    D is the wave direction vector ("@" means dot product). We can plug in the expressions for x and y above to get:
    z = A*sin(F*t + (root_uv.x, root_uv.y + camera_slope*z) @ D) = A*sin(F*t + root_uv @ D + camera_slope*D.y*z).

    To simplify this equation, we define w as the expression inside the sin() and rewrite in terms of w. This means:
    w := F*t + root_uv @ D + camera_slope*D.y*z
    (w - (F*t + root_uv @ D))/(camera_slope*D.y) = A*sin(w)
    w - (F*t + root_uv @ D) = A*camera_slope*D.y*sin(w)

    We can define two more numbers to fully simplify the equation:
    a := F*t + root_uv @ D
    b := A*camera_slope*D.y
    This gives us a simple equation:
    w - a = b*sin(w)

    This equation is transcendental and thus can't be solved for w in terms of elementary functions. Instead we use
    Newton's Method, starting with an initial guess of w = a. This lets us approximately find w given a and b.

    Then, we work backwards to find y. We use what we derived above:
    z = A*sin(w)
    w - a = b*sin(w) --> sin(w) = (w - a)/b --> z = A*(w-a)/b
    y = camera_slope*z + root_uv.y
    */
    float a = -big_wave_freq * time + dot(big_wave_velocity, root_uv);
    const float b = big_wave_amplitude * camera_slope * big_wave_velocity.y;
    // call the equation solver
    float w = solve_equation(a, b);
    // calculate z
    float z = big_wave_amplitude * (w-a)/b; // (w-a)/b is faster than calling sin(w)
    if (b == 0.0) { // can't use the above shortcut because it's 0/0, so we have to use sin(w)
        z = big_wave_amplitude * sin(w);
    }
    float y = camera_slope * z + root_uv.y;
    float x = root_uv.x;
    return vec3(x, y, z);
}

/* Return the height and gradient of the big wave as (height, grad x, grad y) at the given location. */
vec3 get_big_wave(vec3 world_xyz) {
    vec2 world_xy = world_xyz.xy;
    float height = world_xyz.z; // = big_wave_amplitude * sin(big_wave_freq * time + dot(big_wave_velocity, world_xy));
    vec2 gradient = big_wave_amplitude * cos(big_wave_freq * time + dot(big_wave_velocity, world_xy)) * big_wave_velocity;
    return vec3(height, gradient);
}

// ============================================== PART 2. TEXTURE WAVE ============================================== //

/* Returns the texture wave height, gradient, and speed at the given uv coordinates. Speed and displacement information
 are sampled from flow_map at grid_point. Returns (height, grad.x, grad/y, speed). */
vec4 sample_flow_map(vec2 uv, vec2 grid_point) {
    // calculate UV in flowMap texture (between 0 and 1)
    vec2 uv_in_flow_map = (grid_point + vec2(0.5, 0.5)) / level_size; // add 0.5 to move to center of a pixel to avoid interpolation
    // calculate flow vector
    vec2 flow_vector = texture2D(flow_map, uv_in_flow_map).rg * 2.0 - 1.0; // scale and shift into -1...1 range

    // offset uv
    uv -= time * current_speed * flow_vector; // offset with flow vector * time, to animate the texture
    uv -= time * big_wave_speed * big_wave_direction * 0.25; // offset with big wave, but at 0.25 speed
    uv.y = 1.0 - uv.y; // flip vertically
    uv.y *= aspect_ratio;
    uv *= tile_factor; // scale texture

    // texture-based UV displacement
    vec2 uv_in_TBO = uv * texture_offset_scale + time * texture_offset_time_scale;
    vec2 texture_based_offset = (texture2D(texture_offset_uv, uv_in_TBO).rg * 2.0 - 1.0) * texture_offset_height;
    uv += texture_based_offset;

    float height = texture2D(u_texture, uv).r; // only uses one channel
    float speed = dot(flow_vector, flow_vector); // actually speed squared, but it doesn't matter
    speed = min(1.0, 50.0*speed);
    vec3 normal = texture2D(normal_texture, uv).rgb * 2.0 - 1.0;
    vec2 gradient = normal.xy/normal.z + texture_based_offset;
    return vec4(height, gradient, speed);
}

/* Return the height and gradient of the texture wave as (height, grad x, grad y) at the given location. */
vec3 get_texture_wave(vec2 world_xy) {
    // convert world_xy to uv (in flow_map texture)
    vec2 uv = world_xy;
    uv.y /= aspect_ratio;

    // do bilinear interpolation on 4 nearby pixels in flow_map
    vec2 bottom_left = floor(uv - vec2(0.5,0.5)); // pixel coordinates of pixel in flow_map to the bottom-left of uv
    vec4 s00 = sample_flow_map(uv, bottom_left);
    vec4 s01 = sample_flow_map(uv, bottom_left + vec2(0.0,1.0));
    vec4 s10 = sample_flow_map(uv, bottom_left + vec2(1.0,0.0));
    vec4 s11 = sample_flow_map(uv, bottom_left + vec2(1.0,1.0));
    vec2 coeff = uv - (bottom_left + vec2(0.5, 0.5)); // interpolation coefficient (x and y components)
    coeff = smoothstep(0.5-current_blend, 0.5+current_blend, coeff); // smooth the blending
    vec4 s0 = mix(s00, s01, coeff.y);
    vec4 s1 = mix(s10, s11, coeff.y);
    vec4 combined = mix(s0, s1, coeff.x);

    float height = combined.x;
    vec2 gradient = combined.yz;
    float water_speed = combined.w;

    // increase height using water_speed to make currents more visible
    float speed_scaling = 1.0+2.0*water_speed;

    return vec3(height, gradient) * texture_wave_amplitude * speed_scaling;
}

// ================================================== PART 3. SURF ================================================== //

/* Returns the surf_map value at the given uv coordinates. */
float sample_surf_map(vec2 uv) {
    uv = (uv + vec2(0.5, 0.5)) / (level_size * surf_map_res); // offset uv to center of a pixel, and scale by surf_map size
    return texture2D(surf_map, uv).r; // we only care about the red channel
}

/* Returns the interpolated surf_map value at the given world coordinates. */
float get_surf_map_value(vec2 world_xy) {
    // convert world_xy to uv (in surf_map texture)
    vec2 uv = world_xy * surf_map_res;
    uv.y /= aspect_ratio;

    // do bilinear interpolation on 4 nearby pixels in surf_map
    vec2 bottom_left = floor(uv - vec2(0.5,0.5)); // pixel coordinates of pixel in surf_map to the bottom-left of uv
    float s00 = sample_surf_map(bottom_left);
    float s01 = sample_surf_map(bottom_left + vec2(0.0,1.0));
    float s10 = sample_surf_map(bottom_left + vec2(1.0,0.0));
    float s11 = sample_surf_map(bottom_left + vec2(1.0,1.0));
    vec2 coeff = uv - (bottom_left + vec2(0.5, 0.5)); // interpolation coefficient (x and y components)
    float s0 = mix(s00, s01, coeff.y);
    float s1 = mix(s10, s11, coeff.y);
    float combined = mix(s0, s1, coeff.x);
    return combined;
}

/* Returns the height [and gradient] of the surf "waves" at the given location. */
vec3 get_surf(vec2 world_xy) {
    float dist = get_surf_map_value(world_xy); // 0...1, where 0 is close to terrain and 1 is far from terrain
    float dist_prop = min(1.0, dist/surf_range); // limit surf range
    float env = dist_prop*(1.0 - dist_prop) * 2.0; // wave envelope
    float wave = sin(2.0 * PI * (surf_speed * time - dist)/surf_wavelength);
    float height = ((1.0 - dist_prop) + env * wave);
    // optionally calculate gradient (we don't do this, but this is where we would)
    vec2 gradient = vec2(0.0);
    return vec3(height, gradient) * surf_amplitude;
}

// ================================================== PART 4. WAKE ================================================== //

/* Returns the height of the wavelet function at val, and its derivative (wrt. val), as (height, deriv).
 The wavelet function is W(x) = sin(pi*f*x)/(1+(f*x)^2), where f is wake_wavelet_freq. */
vec2 wake_wavelet(float val) {
    val *= wake_wavelet_freq;
    float denom = (1.0 + val*val);
    float height = sin(PI*val)/denom;
    float deriv = (PI*cos(PI*val) - 2.0*val*height)/denom * wake_wavelet_freq;
    return vec2(height, deriv);
}

/* Return the part of the wake which is contributed by the given sample point at the given location. */
vec3 wake_contributed_from_sample(int sample_id, vec2 world_xy) {
    // distance to sample point
    vec2 delta = world_xy - wake_samples_pos[sample_id] * vec2(1.0, aspect_ratio);
    float dist = sqrt(dot(delta, delta));

    // retrieve / calculate sample information
    float sample_age = wake_sample_latency + sample_id * wake_sample_period; // age of sample point (in seconds)
    float sample_speed = wake_samples_speed[sample_id]; // raft speed at sample point in time
    // radius of circle wave emanating from sample:
    float R = sample_age * sample_speed / 3.0; // this expression lets us approximate the Kelvin Wake Pattern when moving at a constant velocity

    // call wavelet function
    vec2 wav = wake_wavelet(dist - R);
    float height = wav.x;
    float deriv = wav.y;

    // calculate gradient
    vec2 gradient = delta * deriv / dist;

    // scale this sample's contribution
    float age_prop = sample_age / (wake_sample_period * wake_samples_number);
    float age_decay = clamp(5.0*(1.0 - age_prop), 0.0, 1.0); // older samples contribute less
    float slowness_factor = clamp(sample_speed, 0.0, 1.0); // slow-moving raft produces less wake

    return vec3(height, gradient) * age_decay * slowness_factor * slowness_factor;
}

/* Return the height and gradient of the wake as (height, grad x, grad y) at the given location. */
vec3 get_wake(vec2 world_xy) {
    vec3 tot = vec3(0.0);
    for(int i = 0; i < wake_samples_number; i++) {
        tot += wake_contributed_from_sample(i, world_xy);
    }
    return tot * wake_amplitude;
}

// ================================================== PART 5. MAIN ================================================== //

void main() {
    vec2 coords = get_coords();

    // get position in world (accounting for displacement caused by the big wave)
    vec3 world_xyz = find_water_intersect_point(coords);
    vec2 world_xy = world_xyz.xy;

    // calculate height and gradient of big wave
    vec3 big_wave = get_big_wave(world_xyz);
    big_wave.x = 0.0; // ignore big wave height for coloring

    // calculate height and gradient of texture wave
    vec3 texture_wave = get_texture_wave(world_xy);

    // calculate height and gradient of surf
    vec3 surf = get_surf(world_xy);
    surf.yz = vec2(0.0); // ignore surf gradient for coloring

    // calculate height and gradient of wake
    vec3 wake = get_wake(world_xy);

    // combine components
    vec3 sum = big_wave + texture_wave + surf + wake; // components are simply added together
    float sum_height = sum.x;
    vec2 sum_gradient = sum.yz;

    // calculate reflection amount using angle of incidence
    vec3 overall_normal = normalize(vec3(sum_gradient, 1.0)); // normal vector to the water surface
    float cos_ang = -dot(overall_normal, camera_ray);// cosine of angle between normal and incoming ray (angle of incidence)
    // proportion of light power reflected from sky into camera (using Schlick's Approximation)
    // this assumes lighting is diffuse (i.e. no sun, only sky) and that reflected rays always go into the sky (i.e., water isn't too choppy).
    float reflection = 0.02 + 0.98*pow(1.0-cos_ang, 5.0); // 0.02 = ((1-n)/(1+n))^2, where n=1.33 = index of refraction of water

    vec3 color = color_from_value(value_from_h_and_r(sum_height, reflection));
    gl_FragColor = vec4(color, 1.0);
}
