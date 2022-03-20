package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

import java.util.Random;

public class Enemy extends WheelObstacle {

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
    }

    public Enemy(Vector2 position, Raft targetRaft) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        this.targetRaft = targetRaft;
    }

//    // TODO: this will change depending on implementation of AIController
//    public void update(float dt) {
//        if (targetRaft != null) {
//            setLinearVelocity(moveVector);
//        }
//
//    }

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
