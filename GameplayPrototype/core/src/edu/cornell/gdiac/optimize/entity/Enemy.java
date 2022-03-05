package edu.cornell.gdiac.optimize.entity;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.optimize.GameObject;

public class Enemy extends GameObject {
    private Ship targetShip;

    public Enemy() {
        radius = 50;
    }

    public Enemy(Ship targetShip) {
        radius = 50;
        this.targetShip = targetShip;
    }

    /** get the type of wood objects
     * @return object type wood */
    @Override
    public ObjectType getType() {
        return ObjectType.ENEMY;
    }

    public void update(float delta) {
        if (targetShip != null) {
            Vector2 temp = targetShip.getPosition().cpy();
            temp.sub(position);
            velocity = temp.nor();
        }
        position.add(velocity);
    }
}
