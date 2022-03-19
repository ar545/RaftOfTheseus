package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Goal extends GameObject {
    public ObjectType getType() {
        return ObjectType.GOAL;
    }

    public Goal(Vector2 position) {
        super();
        setPosition(position);
    }

    // goal shouldn't update
    public void update(float dt) {
        // nothing for now
    }
}
