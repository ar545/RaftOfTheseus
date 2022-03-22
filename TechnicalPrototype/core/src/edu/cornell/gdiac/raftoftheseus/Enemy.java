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
     * How fast the enemy moves towards its target, in units per second
     */
    public static final float ENEMY_CHASE_SPEED = 2.0f;
    /**
     * How much health will enemy take from player upon collision
     */
    protected static final float ENEMY_DAMAGE = -25.0f;

    private Vector2 moveVector = new Vector2();

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

    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }

    public Enemy(Vector2 position, Raft targetRaft) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        this.targetRaft = targetRaft;
        fixture.filter.categoryBits = CATEGORY_ENEMY;
        fixture.filter.maskBits = MASK_ENEMY;
    }

//    // TODO: this will change depending on implementation of AIController
    public void update(float dt) {
        super.update(dt);
        if (moveVector != null && targetRaft != null) {
            body.applyLinearImpulse(moveVector, getPosition(), true);
        }
    }

    /**
     * call for AI controller
     */
    public void resolveAction(enemyState controlSignal, Raft player, long ticks) {
        if (isDestroyed())
            return;

//        System.out.println(controlSignal);
        switch (controlSignal) {
            case SPAWN:
                break;
            case WANDER:
                // every once in a while pick a new random direction

                if (ticks % 60 == 0) {
                    int p = rand.nextInt(4);
                    // move randomly in one of the four directions
                    if (p == 0) {
                        moveVector.set(0, 1);
                    } else if (p == 1) {
                        moveVector.set(0, -1);
                    } else if (p == 2) {
                        moveVector.set(-1, 0);
                    } else {
                        moveVector.set(1, 0);
                    }
                    calculateImpulse(ENEMY_WANDER_SPEED, 0.9f);
                }
                break;
            case CHASE:
                // find a normal vector pointing to the target player
                moveVector.set(player.getPosition()).sub(getPosition()).nor();
                // apply a linear impulse to accelerate towards the player, up to a max speed of ENEMY_CHASE_SPEED
                calculateImpulse(ENEMY_CHASE_SPEED, 0);
                break;
            default:
                // illegal state
                assert (false);
                break;
        }
    }

    /**
     * Sets moveVector so that applying it as a linear impulse brings this object's velocity closer to
     * moveVector*topSpeed.
     * Precondition: moveVector.len() == 1.
     * @param topSpeed Won't apply an impulse that takes us above this speed
     * @param smoothing Impulse is scaled by (1-smoothing). Higher smoothing means wider turns, slower responses.
     */
    private void calculateImpulse(float topSpeed, float smoothing) {
        float currentSpeed = getLinearVelocity().dot(moveVector); // current speed in that direction
        float impulseMagnitude = (topSpeed - currentSpeed)*body.getMass()*(1-smoothing);
        moveVector.scl(impulseMagnitude);
    }

    @Override
    public float getCrossSectionalArea() {
        return super.getCrossSectionalArea()*0.2f; // sharks are less affected by drag
    }
}
