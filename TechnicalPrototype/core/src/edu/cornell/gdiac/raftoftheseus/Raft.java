package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.Align;
import edu.cornell.gdiac.raftoftheseus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.util.FilmStrip;

/**
 * Model class for the player raft.
 */
public class Raft extends CapsuleObstacle implements Steerable<Vector2> {

    // CONSTANTS
    /** Movement cost for a unit distance **/
    private static final float MOVE_COST = 1.5f;
    /** Movement cost multiplier for moving with the current */
    private static final float WITH_CURRENT = 0.45f;
    /** Movement cost increase for moving with the current */
    private static final float AGAINST_CURRENT = 1.75f;

    // ATTRIBUTES
    /** The player's movement input */
    private Vector2 movementInput = new Vector2(0f,0f);

    /** The health of the raft. This must be >=0. */
    private float health;
    /** Maximum player health */
    public static final float MAXIMUM_PLAYER_HEALTH = 120.0f;
    /** Initial player health */
    public static final float INITIAL_PLAYER_HEALTH = 40.0f;
    /** The star of the level. This must be >=0. */
    private int star;
    /** Initial player health */
    private static final int INITIAL_PLAYER_STAR = 0; //TODO: potential bugs on stars upon level restart

    /** Whether the raft is actively firing */
    private boolean fire;
    /** The amount to slow the character down, while they aren't moving */
    private final float damping = 20f;
    /** The amount to accelerate the character */
    private final float thrust = 10f;
    /** The maximum character speed allowed */
    private final float maxSpeed = 20f;
    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();

    private FilmStrip texture;
    public int animationFrame = 0;

    // METHODS
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

    /** Returns the player movement input. */
    protected Vector2 getMovementInput() { return movementInput; }

    /** Sets the player movement input. */
    public void setMovementInput(Vector2 value) { movementInput.set(value); }

    /** @return whether the player is actively firing */
    public boolean isFire() { return fire; }

    /** @param fire whether to set the player to actively firing */
    public void setFire(boolean fire) { this.fire = fire; }

    /**
     * Applies the force to the body of this dude
     *
     * This method should be called after the force attribute is set.
     */
    public void applyInputForce() {
        if (super.isDestroyed()) {
            return;
        }

        if (movementInput.isZero()) {
            // Damp out player motion
            forceCache.set(getLinearVelocity()).scl(-damping);
            body.applyForce(forceCache,getPosition(),true);
        } else {
            // Accelerate player based on input
            forceCache.set(movementInput).scl(thrust);
            body.applyLinearImpulse(forceCache,getPosition(),true);
        }

        // Velocity too high, clamp it
        float speedRatio = maxSpeed / getLinearVelocity().len();
        if (speedRatio < 1) {
            setLinearVelocity(getLinearVelocity().scl(speedRatio));
        }
    }

    /** Getter and setters for health */
    public float getHealth() { return health; }

    /** Getter and setters for health */
    public float getHealthRatio() { return health / MAXIMUM_PLAYER_HEALTH; }

    public void setHealth(float newHealth) {
        health = Math.max(0, newHealth);
    }

    public void addHealth(float wood) {
        health = Math.min(health + wood, MAXIMUM_PLAYER_HEALTH);
    }

    /** Constructor for Raft object
     * @param position: position of raft
     */
    public Raft(Vector2 position) {
        super(2.8f, 1.3f);
//        setRadius(1.4f);
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        this.health = INITIAL_PLAYER_HEALTH;
        this.star = INITIAL_PLAYER_STAR;
        fixture.filter.categoryBits = CATEGORY_PLAYER;
        fixture.filter.maskBits = MASK_PLAYER;
//        Filter filter = new Filter();
//        filter.categoryBits = (short) 1;
//        filter.maskBits = (short) 0;
//        setFilterData(filter);
    }

    /** Add one star to the player star count */
    protected void addStar() { star++; }

    /** Get the star of the level */
    protected int getStar() { return star; }

    /** Reduce player health based on distance traveled and movement cost. */
    public void applyMoveCost(float dt) {
        if (!movementInput.isZero()) {
            float L = getLinearVelocity().len();
            if (L > 0.15) { // L < 0.15 could be from moving into a wall, so we ignore it
                float cost = MOVE_COST * L * dt; // base movement cost (no current)
                if (!waterVelocity.isZero()) { // reduced cost if moving with the current, increased if moving against the current
                    // TODO: Demian: I wrote this code, but looking at it again, I should probably rewrite it so it doesn't use all this math.
                    float c = (float)Math.cos(getLinearVelocity().angleRad(waterVelocity));// c = +1 with current, -1 against current
                    float b = 1 + (WITH_CURRENT-AGAINST_CURRENT)*0.5f*c + ((WITH_CURRENT+AGAINST_CURRENT)*0.5f-1)*c*c;// interpolate between modifiers based on c
                    cost *= b;
                }
                health -= cost;
            }
        }
    }

    /** @return whether the player health is below zero */
    public boolean isDead() { return health < 0f; }

    /** How far the raft could travel with its current health, in game units, assuming they don't use currents or drift */
    public float getPotentialDistance() {
        return health / MOVE_COST;
    }

    public void setTexture(FilmStrip value) {
        texture = value;
        origin.set(texture.getRegionWidth()/2.0f, texture.getRegionHeight()/2.0f);

        float w = getWidth()*drawScale.x / texture.getRegionWidth() * 1.50f;
        textureScale = new Vector2(w, w);
        origin.set(texture.getRegionWidth()/2.0f, texture.getRegionWidth()/2.0f * getHeight()/getWidth());
    }

    public void draw(GameCanvas canvas) {
        draw(canvas, Color.WHITE);
    }

    public void draw(GameCanvas canvas, Color color) {
        if (texture != null) {
            texture.setFrame(animationFrame);
            canvas.draw(texture, color, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y,
                    getAngle(), textureScale.x, textureScale.y);
        }
    }

    @Override
    public float getCrossSectionalArea() {
        return 1.5f*super.getCrossSectionalArea();
    }

    /* STEERING */

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
                setAngularVelocity((newOrientation - getAngle() * MathUtils.degreesToRadians) * time); // this is superfluous if independentFacing is always true
                setAngle(newOrientation * MathUtils.radiansToDegrees);
            }
//        }
    }
}
