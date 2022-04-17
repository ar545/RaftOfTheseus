package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Goal extends GameObject {
    public ObjectType getType() {
        return ObjectType.GOAL;
    }

    public Goal(Vector2 position) {
        physicsObject = new WheelObstacle(1.45f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.setSensor(true);
        physicsObject.getFilterData().categoryBits = CATEGORY_NON_PUSHABLE;
        physicsObject.getFilterData().maskBits = MASK_GOAL;
    }
}
