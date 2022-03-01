package edu.cornell.gdiac.optimize.entity;

import edu.cornell.gdiac.optimize.GameObject;

public class Enemy extends GameObject {
    /** get the type of wood objects
     * @return object type wood */
    @Override
    public ObjectType getType() {
        return ObjectType.ENEMY;
    }

}
