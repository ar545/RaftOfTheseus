package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.model.GameObject;
import edu.cornell.gdiac.raftoftheseus.model.Raft;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Hydra extends Enemy<Hydra, HydraState> {
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
    protected static long COOL_DOWN;
    protected static long STUN_TIME;

    // Ranges
    protected static float HITTING_RANGE;
    protected static float FIRING_RANGE;

    // Booleans
    private boolean canSee = false;
    private boolean hasFired;
    private boolean hasAttacked;

    private StateMachine<Hydra, HydraState> stateMachine;

    public Hydra(Vector2 position, Raft raft) {
        super(raft);
        physicsObject = new WheelObstacle(1.45f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY;
        physicsObject.getFilterData().maskBits = MASK_ENEMY;
        stateMachine = new DefaultStateMachine<>(this, HydraState.IDLE);
    }

    @Override
    public void updateAI(float dt) {
        stateMachine.update();
    }
    @Override
    public StateMachine<Hydra, HydraState> getStateMachine(){ return this.stateMachine; }

    // Targeting
    @Override
    public Vector2 getTargetDirection(Vector2 playerCurrentVelocity) { return getPosition().sub(player.getPosition()).nor(); }
    public boolean canSee(){ return canSee; }
    public void setSee(boolean h){ canSee = h; }

    // Firing
    public boolean canFire(){
        return canSee && inRange(FIRING_RANGE) && attackTimer.hasTimeElapsed(COOL_DOWN, false);
    }
    public boolean willFire(){
        hasFired = canFire() && stateMachine.isInState(HydraState.SPLASHING);
        attackTimer.setTimeStamp();
        return hasFired;
    }
    public boolean hasFired(){ return hasFired; }
    public void resetHasFired(){ hasFired = false; }

    // Hitting
    public boolean willAttack(){
        hasAttacked = stateMachine.isInState(HydraState.ACTIVE) && inRange(HITTING_RANGE);
        return hasAttacked;
    }
    public boolean hasAttacked(){ return hasAttacked; }
    public void resetHasAttacked(){ hasAttacked = false; }

    // Stunned
    public boolean setHit(){
        if (!stateMachine.isInState(HydraState.STUNNED)){
            stateMachine.changeState(HydraState.STUNNED);
        }
        return false;
    }

    @Override
    public void setStunTexture(TextureRegion value) {

    }
}
