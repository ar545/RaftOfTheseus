package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Treasure extends GameObject {

    // ATTRIBUTES
    /** Has the treasure been collected yet? */
    protected boolean collected;

    public Treasure(Vector2 position) {
        physicsObject = new WheelObstacle(0.9f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_NON_PUSHABLE;
        physicsObject.getFilterData().maskBits = MASK_TREASURE;

        collected = false;
    }

    public ObjectType getType() {
        return ObjectType.TREASURE;
    }

    public boolean getCollected() { return collected; }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public void update(float dt) {
        super.update(dt);
        if (collected) {
            this.setDestroyed(true);
        }
    }
}
