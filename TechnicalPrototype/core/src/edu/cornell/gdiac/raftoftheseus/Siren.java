package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Siren extends GameObject {

    public static void setConstants(JsonValue objParams){
        IDLE_TIME = objParams.getLong("idle time", 500L);
        SINGING_TIME = objParams.getLong("singing time", 3000L);
        SINGING_RANGE = objParams.getFloat("attack range", 1000f);
        ATTACK_RANGE = objParams.getFloat("attack range", 200f);
        ATTACK_DAMAGE = objParams.getFloat("attack damage", 40f);
        PROXIMITY = objParams.getFloat("proximity", 1f);
        FLY_SPEED = objParams.getFloat("fly speed", 0.0001f);
    }

    // Parameters
    private Raft targetRaft;
    private Vector2 location1 = new Vector2();
    private Vector2 location2 = new Vector2();
    private Vector2 direction1 = new Vector2();
    private Vector2 direction2 = new Vector2();
    private Vector2 moveVector = new Vector2();
    private boolean fromLocation1 = true;

    private StateMachine<Siren, SirenController> stateMachine;

    private long timeStamp = 0L;
    private boolean timeStamped = false;
    private boolean hasAttacked;

    private static float PROXIMITY = 1f;
    private static long IDLE_TIME;
    private static long SINGING_TIME;
    private static float SINGING_RANGE;
    private static Vector2 SINGING_FORCE;
    private static float ATTACK_RANGE;
    private static float ATTACK_DAMAGE;
    private static float FLY_SPEED;

    public Siren(Vector2 position1, Vector2 position2, Raft targetRaft) {
        physicsObject = new WheelObstacle(1.45f);
        setPosition(position1);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY;
        physicsObject.getFilterData().maskBits = MASK_SIREN;

        location1.set(position1);
        location2.set(position2);
        direction1.set(position2.sub(position1).scl(FLY_SPEED));
        direction2.set(position1.sub(position2).scl(FLY_SPEED));
        this.targetRaft = targetRaft;
        stateMachine = new DefaultStateMachine<Siren, SirenController>(this, SirenController.IDLE);
    }

    public void update(float dt) {
        physicsObject.getBody().setLinearVelocity(moveVector);
        stateMachine.update();
    }

    public StateMachine<Siren, SirenController> getStateMachine(){ return this.stateMachine; }

    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.SHARK;
    }

    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }

    // Setting movement
    public void setMoveVector(boolean direction1) {
        if(direction1) this.moveVector = this.direction1;
        else this.moveVector = this.direction2;
    }
    public void stopMove(){ this.moveVector.setZero(); }

    // Time
    public void setTimeStamp(){
        if(!timeStamped) {
            timeStamp = TimeUtils.millis();
            timeStamped = true;
        }
    }
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
    public boolean willAttack(){
        hasAttacked = stateMachine.isInState(SirenController.ATTACKING) && inAttackRange();
        return hasAttacked;
    }
    public boolean hasAttacked(){ return hasAttacked; }
    public void resetHasAttacked(){ hasAttacked = false; }
    public static float getAttackDamage(){ return ATTACK_DAMAGE; }

    //
}
