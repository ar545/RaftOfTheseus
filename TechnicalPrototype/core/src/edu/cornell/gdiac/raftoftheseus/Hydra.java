package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Hydra extends WheelObstacle {
    public GameObject.ObjectType getType() {
        return ObjectType.HYDRA;
    }
    public static void setConstants(JsonValue objParams){
        COOL_DOWN = objParams.getLong("cool down", 1500L);
        STUN_TIME = objParams.getLong("stun time", 500L);
        FIRING_RANGE = objParams.getFloat("firing range", 1500f);
        HITTING_RANGE = objParams.getFloat("hitting range", 200f);
    }

    // Timing
    private long timeStamp = 0L;
    private boolean timeStamped = false;
    private static long COOL_DOWN;
    private static long STUN_TIME;

    // Ranges
    private static float HITTING_RANGE;
    private static float FIRING_RANGE;

    // Booleans
    private boolean isHit = false;
    private boolean canSee = false;
    private boolean hasFired;
    private boolean hasAttacked;


    private Raft targetRaft;
    private StateMachine<Hydra, HydraController> stateMachine;

    public Hydra(Vector2 position, Raft targetRaft) {
        setPosition(position);
        setBodyType(BodyDef.BodyType.StaticBody);
        this.targetRaft = targetRaft;
        fixture.filter.categoryBits = CATEGORY_ENEMY;
        fixture.filter.maskBits = MASK_ENEMY;
        stateMachine = new DefaultStateMachine<>(this, HydraController.IDLE);
    }

    public void update(float dt) {
        stateMachine.update();
    }

    public StateMachine<Hydra, HydraController> getStateMachine(){ return this.stateMachine; }

    // Timing
    public void setTimeStamp(){
        if(!timeStamped) {
            timeStamp = TimeUtils.millis();
            timeStamped = true;
        }
    }
    public void resetTimeStamp(){ timeStamped = false; }
    public boolean cooldownElapsed(){
        return TimeUtils.timeSinceMillis(timeStamp) > COOL_DOWN;
    }
    public boolean stunElapsed(){
        return TimeUtils.timeSinceMillis(timeStamp) > STUN_TIME;
    }

    // Targeting
    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }
    public Vector2 getTargetDirection() { return getPosition().sub(targetRaft.getPosition()).nor(); }
    public boolean inRange(){
        return getPosition().dst(targetRaft.getPosition()) <= FIRING_RANGE;
    }

    public boolean canSee(){ return canSee; }
    public void setSee(boolean h){ canSee = h; }

    // Firing
    public boolean canFire(){
        return canSee && inRange() && cooldownElapsed();
    }
    public boolean willFire(){
        hasFired = canFire() && stateMachine.isInState(HydraController.SPLASHING);
        return hasFired;
    }
    public boolean hasFired(){ return hasFired; }
    public void resetHasFired(){ hasFired = false; }

    // Hitting
    public boolean inAttackRange(){
        return targetRaft.getPosition().sub(getPosition()).len() <= HITTING_RANGE;
    }
    public boolean willAttack(){
        hasAttacked = stateMachine.isInState(HydraController.HITTING) && inAttackRange();
        return hasAttacked;
    }
    public boolean hasAttacked(){ return hasAttacked; }
    public void resetHasAttacked(){ hasAttacked = false; }

    // Stunned
    public boolean isHit(){ return isHit; }
    public void setHit(boolean h){
        if (!stateMachine.isInState(HydraController.STUNNED)){
            isHit = h;
        }
    }




}
