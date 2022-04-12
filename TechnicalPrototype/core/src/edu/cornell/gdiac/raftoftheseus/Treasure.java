package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Treasure extends WheelObstacle {

    // ATTRIBUTES
    /** Has the treasure been collected yet? */
    protected boolean collected;

    public Treasure(Vector2 position) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.StaticBody);
        setSensor(true);
        collected = false;
        fixture.filter.categoryBits = CATEGORY_NON_PUSHABLE;
        fixture.filter.maskBits = MASK_TREASURE;
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
            // TODO: update player score
            this.setDestroyed(true);
        }
    }
}
