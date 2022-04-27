package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.projectile.Spear;
import edu.cornell.gdiac.raftoftheseus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.SimpleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

/**
 * Model class for the player raft.
 */
public class Raft extends GameObject {

    /**
     * Method to set Raft parameters
     */
    public static void setConstants(JsonValue objParams){
        MOVE_COST = objParams.getFloat("move cost");
        WITH_CURRENT = objParams.getFloat("with current");
        AGAINST_CURRENT = objParams.getFloat("against current");
        MAXIMUM_PLAYER_HEALTH = objParams.getFloat("max health");
        INITIAL_PLAYER_HEALTH = objParams.getFloat("initial health");
        DAMPING = objParams.getFloat("damping");
        THRUST = objParams.getFloat("thrust");
        MAX_SPEED = objParams.getFloat("max speed");
        OBJ_WIDTH = objParams.getFloat("width");
        OBJ_HEIGHT = objParams.getFloat("height");
        SENSOR_RADIUS = objParams.getFloat("sensor size");
        TEXTURE_SCALE = objParams.getFloat("texture scale");
        IDLE_AS = objParams.getFloat("idle animation speed");
        SHOOTING_AS = objParams.getFloat("shooting animation speed");
        IDLE_F = objParams.getInt("idle frames");
        SHOOTING_F = objParams.getInt("shooting frames");
        IDLE_SF = objParams.getInt("idle starting frame");
        SHOOTING_SF = objParams.getInt("shooting starting frame");
        HORIZONTAL_OFFSET = objParams.getFloat("horizontal offset");
        FORCE_DURATION = objParams.getLong("enemy bullet duration");
    }

    // CONSTANTS
    /** Movement cost for a unit distance **/
    private static float MOVE_COST;
    /** Movement cost multiplier for moving with the current */
    private static float WITH_CURRENT;
    /** Movement cost increase for moving with the current */
    private static float AGAINST_CURRENT;

    // ATTRIBUTES
    /** A physics object used for collisions with interactable objects which shouldn't push the player (wood, goal, etc) */
    private SimpleObstacle interactionSensor;
    /** The player's movement input */
    private Vector2 movementInput = new Vector2(0f,0f);
    /** The health of the raft. This must be >=0. */
    private float health;
    /** Maximum player health */
    public static float MAXIMUM_PLAYER_HEALTH;
    /** Initial player health */
    public static float INITIAL_PLAYER_HEALTH;
    /** Whether the raft is actively firing */
    private boolean isCharging;
    private boolean canFire;
    /** Whether or not the raft is currently being damaged (feedback purposes) */
    private boolean isDamaged;
    /** The amount to slow the character down, while they aren't moving */
    private static float DAMPING;
    /** The amount to accelerate the character */
    private static float THRUST;
    /** The maximum character speed allowed */
    private static float MAX_SPEED;
    private static float MIN_SPEED = 0.01f;
    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();
    private final Vector2 externalForce = new Vector2();
    /** Size constants */
    private static float OBJ_WIDTH;
    private static float OBJ_HEIGHT;
    private static float SENSOR_RADIUS;
    /** How long to keep applying */
    private static float FORCE_DURATION;

    // ANIMATION
    private static float HORIZONTAL_OFFSET;
    /** How much to enlarge the raft. */
    private static float TEXTURE_SCALE;
    /** The animation speed for the raft. */
    private static float IDLE_AS;
    private static float SHOOTING_AS;
    /** The number of frames for this animation. */
    private static int IDLE_F;
    private static int SHOOTING_F;
    /** Which frame to start on the filmstrip with this animation. */
    private static int IDLE_SF;
    private static int SHOOTING_SF;

    private Spear spear;

    // METHODS
    /** Constructor for Raft object
     * @param position: position of raft
     */
    public Raft(Vector2 position) {
        physicsObject = new CapsuleObstacle(OBJ_WIDTH, OBJ_HEIGHT);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER;
        physicsObject.getFilterData().maskBits = MASK_PLAYER;
        this.health = INITIAL_PLAYER_HEALTH;

        interactionSensor = new WheelObstacle(SENSOR_RADIUS);
        interactionSensor.setSensor(true);
        interactionSensor.setBodyType(BodyDef.BodyType.DynamicBody);
        interactionSensor.getFilterData().categoryBits = CATEGORY_PLAYER_SENSOR;
        interactionSensor.getFilterData().maskBits = MASK_PLAYER_SENSOR;
        isCharging = false;
        canFire = false;
        /** Whether or not the player was very recently damaged. */
        isDamaged = false;
    }

    /**
     * Returns the type of this object.
     *
     * We use this instead of runtime-typing for performance reasons.
     *
     * @return the type of this object.
     */
    public ObjectType getType() {
        return ObjectType.RAFT;
    }

    @Override
    public void deactivatePhysics(World world) {
        super.deactivatePhysics(world);
        interactionSensor.deactivatePhysics(world);
    }

    @Override
    public void activatePhysics(World world) {
        super.activatePhysics(world);
        interactionSensor.activatePhysics(world);
        interactionSensor.getBody().setUserData(this);
    }

    @Override
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        interactionSensor.drawDebug(canvas);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        interactionSensor.setPosition(physicsObject.getPosition());
        interactionSensor.update(dt);
    }

    /** Returns the player movement input. */
    protected Vector2 getMovementInput() { return movementInput; }

    public boolean isMoving() {
        boolean isDrifting = physicsObject.getLinearVelocity().len() > MIN_SPEED;
        boolean isRowing = !movementInput.isZero();
        return isDrifting || isRowing;
    }

    /** Getter and setters for whether or not the player was recently damaged. */
    public boolean isDamaged() { return isDamaged; }

    public void setDamaged(boolean damaged) { this.isDamaged = damaged; }

    /** Sets the player movement input. */
    public void setMovementInput(Vector2 value) { movementInput.set(value); }

    /**
     * Applies the force to the body
     *
     * This method should be called after the force attribute is set.
     */
    public void applyInputForce() {
        if (super.isDestroyed()) {
            return;
        }
        if (movementInput.isZero()) {
            // Damp out player motion
            forceCache.set(physicsObject.getLinearVelocity()).scl(-DAMPING);
            physicsObject.getBody().applyForce(forceCache,getPosition(),true);
        } else {
            // Accelerate player based on input
            forceCache.set(movementInput).scl(THRUST);
            physicsObject.getBody().applyLinearImpulse(forceCache,getPosition(),true);
        }
        // Velocity too high, clamp it
        float speedRatio = MAX_SPEED / physicsObject.getLinearVelocity().len();
        if (speedRatio < 1) {
            physicsObject.setLinearVelocity(physicsObject.getLinearVelocity().scl(speedRatio));
        }
    }

    private long timeStamp;
    private long forceDuration = 500L;

    /**
     * Apply projectile force
     * @param force the force applied
     */
    public void setProjectileForce(Vector2 force){
        externalForce.set(force);
        timeStamp = TimeUtils.millis();
    }

    public void applyProjectileForce(){
        if(TimeUtils.timeSinceMillis(timeStamp) < forceDuration) {
            physicsObject.getBody().applyForce(externalForce, getPosition(), true);
        }
    }


    /** Getter and setters for health */
    public float getHealth() { return health; }

    /** Getter and setters for health */
    public float getHealthRatio() { return health / MAXIMUM_PLAYER_HEALTH; }
    public void setHealth(float newHealth) {
        health = Math.max(0, newHealth);
    }
    public void addHealth(float dh) {
        health = Math.min(health + dh, MAXIMUM_PLAYER_HEALTH);
    }

    /** Reduce player health based on distance traveled and movement cost. */
    public void applyMoveCost(float dt) {
        float L = physicsObject.getLinearVelocity().len();
        health -= MOVE_COST * L * dt; // base movement cost (no current)
    }

    /** @return whether the player health is below zero */
    public boolean isDead() { return health < 0f; }

    /** How far the raft could travel with its current health, in game units, assuming they don't use currents or drift */
    public float getPotentialDistance() {
        return health / MOVE_COST;
    }

    // The value to increment once the animation time has passed. Used to calculate which frame should be used.
    private int frameCount = 0;
    // The amount of time elapsed, used for checking whether to increment frameCount.
    private float timeElapsed = 0;
    // Which frame should be set for drawing this game cycle.
    private int frame = IDLE_SF;
    // Which direction to player is facing.
    float flip;


    /** Set whether the playe rhas reached the end of the animation cycle. */
    public void resetCanFire() { canFire = false; }
    /** @return whether the player has reached the end of the firing animation cycle. */
    public boolean canFire() {
        return canFire;
    }

    /** @param charging whether to set the player to actively charging. Subtracts health to create the spear. */
    public void beginCharging(boolean charging) {
        if(!isCharging && !canFire && charging) {
            isCharging = true;
            addHealth(Spear.DAMAGE);
        }
    }
    /** @return whether player is actively charging. */
    public boolean isCharging() { return isCharging; }

    /**
     * Method to set animation based on the time elapsed in the game.
     * @param dt the current time in the game.
     */
    public void setAnimationFrame(float dt) {
        timeElapsed += dt;
        if(frame >= IDLE_SF && !isCharging){
            setFrame(IDLE_AS, IDLE_F, IDLE_SF, false);
        } else if (frame >= IDLE_SF && isCharging){
            frameCount = 0;
            timeElapsed = 0;
            frame = SHOOTING_SF;
        } else if (!isFrame(SHOOTING_F, SHOOTING_SF, false) && isCharging){
            setFrame(SHOOTING_AS, SHOOTING_F, SHOOTING_SF, false);
            canFire = isFrame(SHOOTING_F, SHOOTING_SF, false);
            if(canFire) isCharging = false;
        } else if (isFrame(SHOOTING_F, SHOOTING_SF, false)){
            frame += 20;
            timeElapsed = 0;
        }
        // flip texture based on movement
        flip = getLinearVelocity().x < 0 ? -1 : 1;
        textureScale.x = flip * Math.abs(textureScale.x);
    }

    /**
     * Sets the frame of the animation based on the FSM and time given.
     * @param animationSpeed how many seconds should pass between each frame.
     * @param frames the number of frames this animation has.
     * @param start which frame in the FilmStrip the animation starts on.
     * @param reverse whether the animation should be drawn backwards.
     * @return whether it has reached the last animation image.
     */
    private void setFrame(float animationSpeed, int frames, int start, boolean reverse){
        if (timeElapsed > animationSpeed){
            timeElapsed = 0;
            frameCount += 1;
            frame = start + (reverse ? (frames - 1) - frameCount % frames : frameCount % frames);
        }
    }

    /**
     * Checks whether the current frame is the starting or ending frame.
     * @param frames the amount of frames for the given animation.
     * @param start the starting index.
     * @param begin whether to check for the starting or ending index.
     * @return whether the current frame is the start or end frame.
     */
    private boolean isFrame(int frames, int start, boolean begin){
        return begin ? frame == start : frame == frames - 1 + start;
    }

    /**
     * Realign the raft so that the bottom of it is at the bottom of the capsule object.
     */
    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(HORIZONTAL_OFFSET,(texture.getRegionHeight()*textureScale.y - getHeight())/2f);
        // 0.25 offset because the texture is off-center horizontally
    }

    /**
     * Set the filmstrip frame before call the super draw method.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas){
        ((FilmStrip) texture).setFrame(frame);
        super.draw(canvas);
    }

    /** @param s */
    public void setSpear(Spear s){
        spear = s;
        floatTime = 0;
    }

    /** @return whether this raft has a spear or not. */
    public boolean hasSpear(){
        return spear != null && !spear.isDestroyed();
    }

    float floatTime;

    public void updateSpear(float dt){
        if(!hasSpear()) return;
        floatTime += dt;
        if(floatTime >= 360){
            floatTime = 0;
        }
        spear.setFloatPosition(getPosition(), floatTime, flip);
    }

    public Spear getSpear(){
        return spear;
    }
}
