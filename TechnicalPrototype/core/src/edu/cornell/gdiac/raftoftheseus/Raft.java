package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;


/**
 * Model class for the player raft.
 */
public class Raft extends WheelObstacle {

    // CONSTANTS
    /** Movement cost for a pixel distance **/
    private static final float MOVE_COST = 0.001f; // keep?

    // ATTRIBUTES
    /** The movement of the player this turn */
    private Vector2 movement = new Vector2(0f,0f);
    /** The most recent non-zero movement of the player this turn */
    private Vector2 last_movement = new Vector2(0f,0f);

    /** The health of the raft. This must be >=0. */
    private float health;
    /** Maximum player health */
    public static final float MAXIMUM_PLAYER_HEALTH = 120.0f;
    /** Initial player health */
    public static final float INITIAL_PLAYER_HEALTH = 40.0f;
    /** The star of the level. This must be >=0. */
    private int star;
    /** Initial player health */
    private static final int INITIAL_PLAYER_STAR = 0;

    /** Raft's current speed. */
    private float force;
    /** Whether the raft is actively firing */
    private boolean fire;
    /** The amount to slow the character down */
    private final float damping = 2f;
    /** The maximum character speed */
    private final float maxSpeed = 10f;
    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();

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

    /**
     * Returns the current player (left/right) movement input.
     *
     * @return the current player movement input.
     */
    protected Vector2 getMovement() { return movement; }

    /** @return Last movement is the facing of the player */
    public Vector2 getFacing() { return last_movement; }

    /**
     * Sets the player movement input.
     */
    public void setMovement(Vector2 value) {
        movement.set(value);
    }

    /** @return whether the player is actively firing */
    public boolean isFire() { return fire; }

    /** @param fire whether to set the player to actively firing */
    public void setFire(boolean fire) { this.fire = fire; }

    /** Get the force float of the player */
    public float getForce() { return force; }

    /** @return how hard the brakes are applied to get a dude to stop moving */
    public float getDamping() { return damping; }

    /** @return the upper limit on dude vertical and horizontal movement.
     */
    public float getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Applies the force to the body of this dude
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (super.isDestroyed()) {
            return;
        }

        // Don't want to be moving. Damp out player motion
        forceCache.set(getLinearVelocity().scl(-getDamping()));
        body.applyForce(forceCache,getPosition(),true);

        forceCache.set(getMovement());
        body.applyLinearImpulse(forceCache,getPosition(),true);

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getVX())*getMaxSpeed());
        }
        if (Math.abs(getVY()) >= getMaxSpeed()) {
            setVY(Math.signum(getVY())*getMaxSpeed());
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
     * @param force: speed of raft
     */
    public Raft(Vector2 position, float force) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        this.force = force;
        this.health = INITIAL_PLAYER_HEALTH;
        this.star = INITIAL_PLAYER_STAR;
    }

    /** Add one star to the player star count */
    protected void addStar() { star++; }

    /** Get the star of the level */
    protected int getStar() { return star; }

    /** take the health cost for player. This the always scaled to 1 move_cost because player move are normalized,
     * i.e. movement.len() = 1 always hold */
    public void subtractHealth() { health = health - (MOVE_COST  * force); }

    /** @return whether the player health is below zero */
    public boolean isDead() { return health < 0f; }
}
