package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

import java.sql.Time;
import java.util.Random;

public class Hydra extends WheelObstacle {
    private Random rand = new Random();
    public GameObject.ObjectType getType() {
        return ObjectType.HYDRA;
    }

    private static float RANGE;
    private static long COOLDOWN;
    private static long STUNTIME;
    public static void setConstants(JsonValue objParams){
        COOLDOWN = objParams.getLong("cooldown", 1500L);
        STUNTIME = objParams.getLong("stun time", 500L);
        RANGE = objParams.getFloat("range", 1500f);
    }

    private Vector2 targetDirection = new Vector2();
    private long time1;
    private boolean timeSet = false;
    private boolean canFire = true;
    private boolean isHit = false;
    private boolean canSee = false;

    public static enum EnemyState {
        /* The enemy just spawned */
        SPAWN,
        /* The enemy is idle without a target. */
        IDLE,
        /** The enemy has a target, but must get closer. */
        ACTIVE,
        HITTING,
        SPLASHING,
        STUNNED
    }

    /**
     * This is the player, if this enemy is targeting the player.
     */
    private Raft targetRaft;

    public Hydra(Vector2 position, Raft targetRaft) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.StaticBody);
        this.targetRaft = targetRaft;
        fixture.filter.categoryBits = CATEGORY_ENEMY;
        fixture.filter.maskBits = MASK_ENEMY;
    }

    public void update(float dt) {
        super.update(dt);
    }

    public boolean canFire(){ return canFire; }
    public boolean isHit(){ return isHit; }
    public void setHit(boolean h){ isHit = h; }
    public boolean canSee(){ return canSee; }
    public void setSee(boolean h){ canSee = h; }
    public float dist(){
        return getPosition().dst(targetRaft.getPosition());
    }

    /**
     * call for AI controller
     */
    public void resolveAction(Hydra.EnemyState controlSignal, Raft player, long ticks) {
        if (isDestroyed())
            return;

        switch (controlSignal) {
            case SPAWN:
                // Intermittent state
                break;
            case IDLE:
                // Do nothing but play animation
                break;
            case ACTIVE:
                if(!timeSet) {
                    time1 = TimeUtils.millis();
                    timeSet = true;
                } else if(TimeUtils.timeSinceMillis(time1) > COOLDOWN) {
                    canFire;
                }
                // find a normal vector pointing to the target player and start timer
                targetDirection.set(player.getPosition()).sub(getPosition()).nor();

                break;
            case SPLASHING:

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
