package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.Environment;

public class Obstacle extends Environment {
    public Obstacle() {
        radius = 50;
    }

    /** get the type of rock objects
     * @return object type obstacle */
    @Override
    public Environment.ObjectType getType() {
        return Environment.ObjectType.OBSTACLE;
    }
}
