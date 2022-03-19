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

    // CONSTANTS
    /** the maximum log generated for each pile of wood */
    private final static float MAXIMUM_WOOD_GENERATION = 10f;
    /** the minimum log generated for each pile of wood */
    private final static float MINIMUM_WOOD_GENERATION = 5f;

    public ObjectType getType() {
        return ObjectType.WOOD;
    }

    /** Constructor for Wood object
     * @param position: position of wood
     * @param value: amount of wood
     */
    public Wood(Vector2 position, int value) {
        super();
        setRadius(1.25f);
        wood = value;
        setPosition(position);
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
}
