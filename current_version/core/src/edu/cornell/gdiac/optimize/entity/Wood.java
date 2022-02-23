package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.GameObject;

/**
 * Model class for driftwood.
 */
public class Wood extends GameObject {
    @Override
    public ObjectType getType() {
        return ObjectType.WOOD;
    }
}
