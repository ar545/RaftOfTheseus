package edu.cornell.gdiac.optimize.entity;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.optimize.GameObject;

public class Enemy extends GameObject {
    /** How much damage an enemy deals to the player upon collision, per animation frame */
    public static final float DAMAGE_PER_FRAME = 0.5f;

    /** This is the player, if this enemy is targeting the player. */
    private Ship targetShip;

    public Enemy() {
        super();
        radius = 50;
    }

    public Enemy(Ship targetShip) {
        super();
        this.targetShip = targetShip;
        radius = 50;
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
