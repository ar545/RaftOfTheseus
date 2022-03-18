package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import java.util.Random;

public class Enemy extends GameObject {

    private Random rand = new Random();

    public ObjectType getType() {
        return ObjectType.ENEMY;
    }

    /**
     * How much damage an enemy deals to the player upon collision, per animation frame
     */
    public static final float DAMAGE_PER_FRAME = 0.5f;
    /**
     * How fast enemy wanders around w/o target
     **/
    public static final float ENEMY_WANDER_SPEED = 1.5f;
    /**
     * How fast the enemy moves towards its target, in pixel per frame
     */
    public static final float ENEMY_CHASE_SPEED = 2.0f;
    /**
     * How much health will enemy take from player upon collision
     */
    protected static final float ENEMY_DAMAGE = 10.0f;

    private Vector2 moveVector;

    public static enum enemyState {
        /**
         * The enemy just spawned
         */
        SPAWN,
        /**
         * The enemy is patrolling around without a target
         */
        WANDER,
        /**
         * The enemy has a target, but must get closer
         */
        CHASE
    }


    /**
     * This is the player, if this enemy is targeting the player.
     */
    private Raft targetRaft;

    public Enemy() {
        super();
        radius = 50;
    }

    public Enemy(Vector2 position, Raft targetRaft) {
        super();
        this.targetRaft = targetRaft;
        radius = 50;
        setPosition(position);
    }

    // TODO: this will change depending on implementation of AIController
    public void update(float dt) {
        if (targetRaft != null) {
            setLinearVelocity(moveVector);
        }

    }

    // TODO: fix

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     * <p>
     * Implementations of this method should NOT retain a reference to World.
     * That is a tight coupling that we should avoid.
     *
     * @param world Box2D world to store body
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

    /**
     * call for AI controller
     */
    public void resolveAction(enemyState controlSignal, Raft player, long ticks) {
        switch (controlSignal) {
            case SPAWN:
                break;
            case WANDER:
                // every once in a while pick a new random direction
                if (ticks % 120 == 0) {
                    int p = rand.nextInt(4);

                    Vector2 temp = new Vector2();
                    // move randomly in one of the four directions
                    if (p == 0) {
                        temp.set(temp.x, 1);
                    } else if (p == 1) {
                        temp.set(temp.x, -1);

                    } else if (p == 2) {
                        temp.set(-1, temp.y);
                    } else {
                        temp.set(1, temp.y);

                    }
                    moveVector = temp;
                    moveVector.nor().scl(ENEMY_WANDER_SPEED);
                }
            case CHASE:
                Vector2 temp = targetRaft.getPosition().cpy();
                temp.sub(getPosition());
                moveVector = temp;
                moveVector.nor().scl(ENEMY_CHASE_SPEED);
                break;
            default:
                // illegal state
                assert (false);
                break;
        }
    }
}
