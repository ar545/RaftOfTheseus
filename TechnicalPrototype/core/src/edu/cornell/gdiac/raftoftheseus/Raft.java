package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.World;


/**
 * Model class for the player raft.
 */
public class Raft extends GameObject {

    // CONSTANTS
    /** Movement cost for a pixel distance **/
    private static final float MOVE_COST = 0.05f; // keep?

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
    private final float damping = 1f;
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
     * Sets the current player (left/right) movement input.
     *
     * @param value the current player movement input.
     */
    private void setMovement(Vector2 value) {
        movement.set(value);
    }

    /**
     * Sets the current player movement input on the x-axis
     * @param value the current player movement input on x-axis
     */
    public void setMovementX(float value) {
        movement.x = value;
    }

    /**
     * Sets the current player movement input on the y-axis
     * @param value the current player movement input on y-axis
     */
    public void setMovementY(float value) {
        movement.y = value;
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
        if (getMovement().x == 0f) {
            forceCache.set(-getDamping()*getVX(),0);
            body.applyForce(forceCache,getPosition(),true);
        }
        if (getMovement().y == 0f) {
            forceCache.set(-getDamping()*getVY(),0);
            body.applyForce(forceCache,getPosition(),true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getVX())*getMaxSpeed());
        } else if (Math.abs(getVY()) >= getMaxSpeed()) {
            setVX(Math.signum(getVY())*getMaxSpeed());
        } else {
            forceCache.set(getMovement());
            body.applyForce(forceCache,getPosition(),true);
        }

    }

    /** Getter and setters for health */
    public float getHealth() { return health; }

    public void setHealth(float newHealth) {
        health = Math.max(0, newHealth);
    }

    public void addHealth(float wood) {
        health = Math.min(health + wood, MAXIMUM_PLAYER_HEALTH);
    }

    /** If the player collides with a border/rock, this is called to prevent that movement from costing health */
    public void cancelLastMovementCost() {
        health += last_movement.len()* force *MOVE_COST;
    }

    /** If the player collides with a rock, this is called to undo that movement's change to the raft position */
    public void cancelLastMovement() {
        setPosition(getPosition().add(last_movement.cpy().scl(-force)));
    }

    /** Constructor for Raft object
     * @param position: position of raft
     * @param force: speed of raft
     */
    public Raft(Vector2 position, float force) {
        super();
        setPosition(position);
        this.force = force;
        this.health = INITIAL_PLAYER_HEALTH;
        this.star = INITIAL_PLAYER_STAR;
    }

    public void update(float dt) {
        // Apply movement
        Vector2 temp = movement.cpy();
        setPosition(getPosition().add(temp.scl(force)));
        health -= movement.len() * force * MOVE_COST; // scale health by distance traveled
        if (health < 0) health = 0;
        if(!movement.isZero()){
            last_movement.set(movement);
        }
    }

    // TODO: fix
    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     *
     * Implementations of this method should NOT retain a reference to World.
     * That is a tight coupling that we should avoid.
     *
     * @param world Box2D world to store body
     *
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        // Make a body, if possible
        bodyinfo.active = true;
        body = world.createBody(bodyinfo);
        body.setUserData(this);

        // Only initialize if a body was created.
//        if (body != null) {
//            createFixtures();
//            return true;
//        }

//        bodyinfo.active = false;
        return false;
    }

    // TODO: fix
    /**
     * Destroys the physics Body(s) of this object if applicable,
     * removing them from the world.
     *
     * @param world Box2D world that stores body
     */
    public void deactivatePhysics(World world) {
        // Should be good for most (simple) applications.
//        if (body != null) {
//            // Snapshot the values
//            setBodyState(body);
//            world.destroyBody(body);
//            body = null;
//            bodyinfo.active = false;
//
    }

    /** Add one star to the player star count */
    protected void addStar() { star++; }

    /** Get the star of the level */
    protected int getStar() { return star; }
}
