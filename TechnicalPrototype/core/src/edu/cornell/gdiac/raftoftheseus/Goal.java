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
        radius = 50;
    }

    // goal shouldn't update
    public void update(float dt) {
        // nothing for now
    }

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     *
     * Implementations of this method should NOT retain a reference to World.
     * That is a tight coupling that we should avoid.
     *
     * @param world Box2D world to store body
     *
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        // do nothing
        return false;
    }

    /**
     * Destroys the physics Body(s) of this object if applicable,
     * removing them from the world.
     *
     * @param world Box2D world that stores body
     */
    public void deactivatePhysics(World world) {
        // do nothing
    }
}
