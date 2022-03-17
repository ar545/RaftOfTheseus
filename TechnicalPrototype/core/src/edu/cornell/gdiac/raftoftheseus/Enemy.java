package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Enemy extends GameObject {

    public ObjectType getType() {
        return ObjectType.ENEMY;
    }

    /** How much damage an enemy deals to the player upon collision, per animation frame */
    public static final float DAMAGE_PER_FRAME = 0.5f;
    /** How fast the enemy moves towards its target, in pixel per frame */
    public static final float ENEMY_SPEED = 1.5f;

    /** This is the player, if this enemy is targeting the player. */
    private Raft targetRaft;

    /** Enemy's current position. */
    public Vector2 position;

    public Enemy() {
        super();
        radius = 50;
    }

    public Enemy(Vector2 position, Raft targetRaft) {
        super();
        this.targetRaft = targetRaft;
        radius = 50;
        this.position = position;
    }

    // TODO: this will change depending on implementation of AIController
    public void update(float dt) {
        if (targetRaft != null) {
            Vector2 temp = targetRaft.getPosition().cpy();
            temp.sub(position);
            velocity = temp.nor().scl(ENEMY_SPEED);
        }
        position.add(velocity);
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
