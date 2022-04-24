package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.SimpleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

/**
 * Model class for the player raft.
 */
public class Raft extends GameObject implements Steerable<Vector2> {

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
    /** The star of the level. This must be >=0. */
    private int star;
    /** Whether the raft is actively firing */
    private boolean fire;
    /** The amount to slow the character down, while they aren't moving */
    private static float DAMPING;
    /** The amount to accelerate the character */
    private static float THRUST;
    /** The maximum character speed allowed */
    private static float MAX_SPEED;
    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();
    /** Raft sail sound id */
    private long sailSound;
    private static float OBJ_WIDTH;
    private static float OBJ_HEIGHT;
    private static float SENSOR_RADIUS;

    // ANIMATION
    private static TextureRegion shooting;
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
        this.star = 0;

        interactionSensor = new WheelObstacle(SENSOR_RADIUS);
        interactionSensor.setSensor(true);
        interactionSensor.setBodyType(BodyDef.BodyType.DynamicBody);
        interactionSensor.getFilterData().categoryBits = CATEGORY_PLAYER_SENSOR;
        interactionSensor.getFilterData().maskBits = MASK_PLAYER_SENSOR;
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
        // TODO: save level star
        star = 0; // Clear level star
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

    public boolean isMoving() { return !physicsObject.getLinearVelocity().isZero() || !movementInput.isZero(); }

    /** Sets the player movement input. */
    public void setMovementInput(Vector2 value) { movementInput.set(value); }

    /** @return whether the player is actively firing */
    public boolean isFire() { return fire; }

    /** @param fire whether to set the player to actively firing */
    public void setFire(boolean fire) { this.fire = fire; }

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


    /** Add one star to the player star count */
    public void addStar() { star++; }

    /** Get the star of the level */
    public int getStar() { return star; }

    /** Reduce player health based on distance traveled and movement cost. */
    public void applyMoveCost(float dt) {
        float L = physicsObject.getLinearVelocity().len();
        health -= MOVE_COST * L * dt; // base movement cost (no current)
    }

    /**
     * Apply projectile force
     * @param force the force applied
     */
    public void applyProjectileForce(Vector2 force){
        forceCache.set(force);
        physicsObject.getBody().applyForce(forceCache, getPosition(), true);
    }

    /** @return whether the player health is below zero */
    public boolean isDead() { return health < 0f; }

    /** How far the raft could travel with its current health, in game units, assuming they don't use currents or drift */
    public float getPotentialDistance() {
        return health / MOVE_COST;
    }

    /**
     * Realign the raft so that the bottom of it is at the bottom of the capsule object.
     */
    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(0,(texture.getRegionHeight()*textureScale.y - getHeight())/2f);
    }

    /**
     * Method to set animation based on the time elapsed in the game.
     * @param time the current time in the game.
     */
    public void setAnimationFrame(float time) {
        // Get frame number
        int frame = (int)(time * IDLE_AS);
        // Find frame
        ((FilmStrip)texture).setFrame(frame % IDLE_F);
    }

    // MUSIC

    /** @param id the id of the sail moving sound. */
    public void setSailSound(long id){
        sailSound = id;
    }

    /** @return the current sail sound and reset the id. */
    public long getSailSound(){
        long temp = sailSound;
        sailSound = -1;
        return temp;
    }

    /* STEERING */

    @Override
    public float getAngularVelocity() {
        return 0;
    }

    @Override
    public float getBoundingRadius() {
        return 0;
    }

    @Override
    public boolean isTagged() {
        return false;
    }

    @Override
    public void setTagged(boolean tagged) {

    }

    @Override
    public float getZeroLinearSpeedThreshold() {
        return 0;
    }

    @Override
    public void setZeroLinearSpeedThreshold(float value) {

    }

    @Override
    public float getMaxLinearSpeed() {
        return 0;
    }

    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed) {

    }

    @Override
    public float getMaxLinearAcceleration() {
        return 0;
    }

    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration) {

    }

    @Override
    public float getMaxAngularSpeed() {
        return 0;
    }

    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed) {

    }

    @Override
    public float getMaxAngularAcceleration() {
        return 0;
    }

    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration) {

    }

    @Override
    public float getOrientation() {
        return 0;
    }

    @Override
    public void setOrientation(float orientation) {

    }

    @Override
    public float vectorToAngle(Vector2 vector) {
        return 0;
    }

    @Override
    public Vector2 angleToVector(Vector2 outVector, float angle) {
        return null;
    }

    @Override
    public Location<Vector2> newLocation() {
        return null;
    }

    /* ACTOR **/

    SteeringBehavior<Vector2> steeringBehavior;
    public SteeringBehavior<Vector2> getSteeringBehavior () {return steeringBehavior;}
    public void setSteeringBehavior (SteeringBehavior<Vector2> steeringBehavior) {
        this.steeringBehavior = steeringBehavior;
    }

    private static final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<Vector2>(new Vector2());

    public void act (float delta) {
        if (steeringBehavior != null) {
            // Calculate steering acceleration
            steeringBehavior.calculateSteering(steeringOutput);

            /*
             * Here you might want to add a motor control layer filtering steering accelerations.
             *
             * For instance, a car in a driving game has physical constraints on its movement: it cannot turn while stationary; the
             * faster it moves, the slower it can turn (without going into a skid); it can brake much more quickly than it can
             * accelerate; and it only moves in the direction it is facing (ignoring power slides).
             */

            // Apply steering acceleration
            applySteering(steeringOutput, delta);
//            setPosition(bodyinfo.position.x, bodyinfo.position.y);
        }
    }

    private void applySteering (SteeringAcceleration<Vector2> steering, float time) {
        // Update position and linear velocity. Velocity is trimmed to maximum speed
        getPosition().mulAdd(getLinearVelocity(), time);
        getLinearVelocity().mulAdd(steering.linear, time).limit(getMaxLinearSpeed());


        // Update orientation and angular velocity
//        if (independentFacing) {
//            setRotation(getRotation() + (bodyinfo.angularVelocity * time) * MathUtils.radiansToDegrees);
//            bodyinfo.angularVelocity += steering.angular * time;
//        } else {
            // If we haven't got any velocity, then we can do nothing.
            if (!getLinearVelocity().isZero(getZeroLinearSpeedThreshold())) {
                float newOrientation = vectorToAngle(getLinearVelocity());
                physicsObject.getBody().setAngularVelocity((newOrientation - getAngle() * MathUtils.degreesToRadians) * time); // this is superfluous if independentFacing is always true
                setAngle(newOrientation * MathUtils.radiansToDegrees);
            }
//        }
    }
}
