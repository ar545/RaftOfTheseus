package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Bullet extends WheelObstacle {
    // TODO: don't believe bullet class is necessary - PhysicsLab creates a bullet using Box2d in WorldController
    public ObjectType getType() {
        return ObjectType.BULLET;
    }

    public Bullet(Vector2 position) {
        super();
        setRadius(0.75f);
        setPosition(position);
        setBodyType(BodyDef.BodyType.KinematicBody);
    }
}
