package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Bullet extends GameObject {

    /** Bullet's current position. */
    public Vector2 position;

    // TODO: don't believe bullet class is necessary - PhysicsLab creates a bullet using Box2d in WorldController
    public ObjectType getType() {
        return ObjectType.BULLET;
    }

    public Bullet(Vector2 position) {
        super();
        this.position = position;
    }

    public void update(float dt) {

    }

    public boolean activatePhysics(World world) {
        return false;
    }

    public void deactivatePhysics(World world) {

    }
}