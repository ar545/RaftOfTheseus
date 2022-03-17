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
    private Vector2 movement = new Vector2(0f,0f); // keep?
    /** The most recent non-zero movement of the player this turn */
    private Vector2 last_movement = new Vector2(0f,0f); // keep?


    /** The health of the raft. This must be >=0. */
    private float health;
    /** Maximum player health */
    public static final float MAXIMUM_PLAYER_HEALTH = 120.0f;
    /** Initial player health */
    public static final float INITIAL_PLAYER_HEALTH = 40.0f;

    /** Raft's current position. */
    public Vector2 position;
    /** Raft's current speed. */
    public float speed;

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

    /** Getter and setters for position */
    public Vector2 getPosition() { return position; }

    public void setPosition(Vector2 pos) {
        position = pos;
    }

    /**
     * Returns the current player (left/right) movement input.
     *
     * @return the current player movement input.
     */
    public Vector2 getMovement() {
        return movement;
    }

    /**
     * Sets the current player (left/right) movement input.
     *
     * @param value the current player movement input.
     */
    public void setMovement(Vector2 value) {
        movement.set(value);
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
        health += last_movement.len()*speed*MOVE_COST;
    }

    /** If the player collides with a rock, this is called to undo that movement's change to the raft position */
    public void cancelLastMovement() {
        position.add(last_movement.cpy().scl(-speed));
    }

    /** Constructor for Raft object
     * @param position: position of raft
     * @param speed: speed of raft
     */
    public Raft(Vector2 position, float speed) {
        super();
        this.position = position;
        this.speed = speed;
    }

    public void update(float dt) {
        // Call superclasses' update
        position.add(velocity);

        // Apply movement
        Vector2 temp = movement.cpy();
        position.add(temp.scl(speed));
        health -= movement.len() * speed * MOVE_COST; // scale health by distance traveled
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
//        // Make a body, if possible
//        bodyinfo.active = true;
//        body = world.createBody(bodyinfo);
//        body.setUserData(this);
//
//        // Only initialize if a body was created.
//        if (body != null) {
//            createFixtures();
//            return true;
//        }
//
//        bodyinfo.active = false;
//        return false;
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
}
