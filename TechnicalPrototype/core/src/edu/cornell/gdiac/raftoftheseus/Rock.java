package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;

public class Rock extends GameObject {
    public ObjectType getType() {
        return ObjectType.OBSTACLE;
    }

    // rocks don't update
    public void update(float dt) {
        // nothing
    }

    public Rock(Vector2 position) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.StaticBody);
    }
}
