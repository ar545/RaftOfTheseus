package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
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
    }

    // TODO: fix
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
//        // Make a body, if possible
//        bodyinfo.active = true;
//        body = world.createBody(bodyinfo);
//        body.setUserData(this);
//
//        // Only initialize if a body was created.
//        if (body != null) {
//            createFixtures();
//            return true;
//        }
//
//        bodyinfo.active = false;
//        return false;
        return false;
    }

    // TODO: fix
    /**
     * Destroys the physics Body(s) of this object if applicable,
     * removing them from the world.
     *
     * @param world Box2D world that stores body
     */
    public void deactivatePhysics(World world) {
        // Should be good for most (simple) applications.
//        if (body != null) {
//            // Snapshot the values
//            setBodyState(body);
//            world.destroyBody(body);
//            body = null;
//            bodyinfo.active = false;
//
    }
}
