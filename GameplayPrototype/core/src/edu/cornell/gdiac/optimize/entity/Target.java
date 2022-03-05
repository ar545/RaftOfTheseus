package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.GameObject;

public class Target extends GameObject {
    public Target() {
        super();
        radius = 50;
    }

    /** get the type of wood objects
     * @return object type wood */
    @Override
    public GameObject.ObjectType getType() {
        return ObjectType.TARGET;
    }
}
