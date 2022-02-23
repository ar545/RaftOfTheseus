package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.GameObject;

/**
 * Model class for driftwood.
 */
public class Wood extends GameObject {
    // TODO: possibly, add a field specifying how much health this piece of driftwood restores
    @Override
    public ObjectType getType() {
        return ObjectType.WOOD;
    }
}
