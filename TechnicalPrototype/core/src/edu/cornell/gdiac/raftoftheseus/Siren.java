package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Siren extends WheelObstacle implements Telegraph {
    /** Parameters */
    private Raft targetRaft;
    private Vector2 location1 = new Vector2();
    private Vector2 location2 = new Vector2();
    private Vector2 direction1 = new Vector2();
    private Vector2 direction2 = new Vector2();
    private Vector2 moveVector = new Vector2();
    private boolean fromLocation1 = true;
    private static float PROXIMITY = 1f;

    private StateMachine<Siren, SirenController> stateMachine;

    private long timeStamp = 0L;
    private boolean timeStamped = false;
    private static long IDLE_TIME;
    private static long SINGING_TIME;
    private static float ATTACK_RANGE;
    private boolean hasAttacked;
    private static void setConstants(JsonValue objParams){

    }


    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.ENEMY;
    }
    @Override
    public boolean handleMessage(Telegram msg) {
        return false;
    }

    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }

    public Siren(Vector2 position, Raft targetRaft) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        this.targetRaft = targetRaft;
        fixture.filter.maskBits = MASK_SIREN;
        stateMachine = new DefaultStateMachine<Siren, SirenController>(this, SirenController.SPAWN);
    }

    public void update(float dt) {
        body.setLinearVelocity(moveVector);
        stateMachine.update();
    }

    public StateMachine<Siren, SirenController> getStateMachine(){ return this.stateMachine; }
    public Vector2 getDirection1(){ return direction1; }
    public Vector2 getDirection2(){ return direction2; }
    public void setMoveVector(boolean direction1) {
        if(direction1) this.moveVector = this.direction1;
        else this.moveVector = this.direction2;
    }
    public void stopMove(){ this.moveVector.setZero(); }
    public void setTimeStamp(){
        if(!timeStamped) {
            timeStamp = TimeUtils.millis();
            timeStamped = true;
        }
    }

    // Time
    public void resetTimeStamp(){ timeStamped = false; }
    public boolean isPastIdleCooldown(){ return TimeUtils.timeSinceMillis(timeStamp) > IDLE_TIME; }
    public boolean isDoneSinging(){ return TimeUtils.timeSinceMillis(timeStamp) > SINGING_TIME; }

    // Changing location
    public boolean fromFirstLocation(){ return fromLocation1; }
    public boolean fromSecondLocation(){ return !fromLocation1; }
    public void setFromFirstLocation(){ fromLocation1 = true; }
    public void setFromSecondLocation(){ fromLocation1 = false; }
    public boolean nearLanding(){
        float dist;
        if(fromLocation1){
            dist = getPosition().sub(location2).len();
        } else {
            dist = getPosition().sub(location1).len();
        }
        return dist < PROXIMITY;
    }

    // Attacking player
    public boolean inAttackRange(){
        return targetRaft.getPosition().sub(getPosition()).len() < ATTACK_RANGE;
    }
    public boolean canAttack(){
        hasAttacked = stateMachine.isInState(SirenController.ATTACKING) && inAttackRange();
        return hasAttacked;
    }
    public boolean hasAttacked(){ return hasAttacked; }
    public void resetHasAttacked(){ hasAttacked = false; }
}
