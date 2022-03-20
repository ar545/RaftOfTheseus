package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Goal extends WheelObstacle {
    public ObjectType getType() {
        return ObjectType.GOAL;
    }

    public Goal(Vector2 position) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.StaticBody);
    }
}
