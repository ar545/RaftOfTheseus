package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.GameObject;

public class Obstacle extends GameObject {
    /** get the type of rock objects
     * @return object type obstacle */
    @Override
    public GameObject.ObjectType getType() {
        return ObjectType.OBSTACLE;
    }
}
