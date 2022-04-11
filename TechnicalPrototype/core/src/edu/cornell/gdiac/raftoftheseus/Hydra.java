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
    private boolean isHit = false;
    private boolean canSee = false;
    private boolean justFired = true;

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
    private EnemyState state;

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

    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }
    public boolean canFire(){ return canSee && inRange() && cooldownElapsed(); }
    public boolean isHit(){ return isHit; }
    public void setHit(boolean h){ isHit = h; timeSet = false;}
    public boolean canSee(){ return canSee; }
    public void setSee(boolean h){ canSee = h; }
    public boolean inRange(){
        return getPosition().dst(targetRaft.getPosition()) <= RANGE;
    }
    public boolean cooldownElapsed(){
        return TimeUtils.timeSinceMillis(time1) > COOLDOWN;
    }
    public boolean stunElapsed(){
        return TimeUtils.timeSinceMillis(time1) > STUNTIME;
    }
    public boolean isSplashing(){ return state == EnemyState.SPLASHING; }

    /** Call from AI controller */
    public void resolveAction(Hydra.EnemyState controlSignal, long ticks) {
        if (isDestroyed())
            return;

        state = controlSignal;
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
                }
                // find a normal vector pointing to the target player and start timer
                targetDirection.set(targetRaft.getPosition()).sub(getPosition()).nor();
                break;
            case SPLASHING:
                timeSet = false;
                break;
            case STUNNED:
                if(!timeSet) {
                    time1 = TimeUtils.millis();
                    timeSet = true;
                }
            default:
                // illegal state
                assert (false);
                break;
        }
    }
}
