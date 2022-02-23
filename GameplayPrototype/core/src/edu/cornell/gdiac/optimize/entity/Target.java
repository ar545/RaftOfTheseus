package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.GameObject;

public class Target extends GameObject {
    /** get the type of wood objects
     * @return object type wood */
    @Override
    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.TARGET;
    }
}
