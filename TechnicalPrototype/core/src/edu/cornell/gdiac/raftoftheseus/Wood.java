package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.World;

/**
 * Model class for driftwood.
 */
public class Wood extends GameObject  {
    // ATTRIBUTES
    /** How many logs is in this pile of wood. player health will add correspondingly */
    private final float wood;
    /** Wood's current position. */
    public Vector2 position;

    // CONSTANTS
    /** the maximum log generated for each pile of wood */
    private final static float MAXIMUM_WOOD_GENERATION = 10f;
    /** the minimum log generated for each pile of wood */
    private final static float MINIMUM_WOOD_GENERATION = 5f;

    public ObjectType getType() {
        return ObjectType.WOOD;
    }

    /** Constructor for Wood object
     * @param pos: position of wood
     * @param value: amount of wood
     */
    public Wood(Vector2 pos, int value) {
        super();
        radius = 40;
        wood = value;
        position = pos;
    }

    /** return the number of logs in this pile of wood
     * @return float representing player health replenish */
    public float getWood() {
        return wood;
    }

    /** set this pile of wood to be destroyed
     * @param value whether to set the wood as destroyed */
    public void setDestroyed(boolean value) {
        super.setDestroyed(value);
    }

    // TODO: should the wood update? (i.e. does it move on currents?)
    public void update(float dt) {
        // nothing for now
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
