package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Siren extends GameObject {

    /**
     * Method to fill in all constants for the Siren
     * @param objParams the JsonValue with heading "siren".
     */
    public static void setConstants(JsonValue objParams){
        IDLE_TIME = objParams.getLong("idle time", 500L);
        SINGING_TIME = objParams.getLong("singing time", 3000L);
        SINGING_RANGE = objParams.getFloat("attack range", 1000f);
        ATTACK_RANGE = objParams.getFloat("attack range", 200f);
        ATTACK_DAMAGE = objParams.getFloat("attack damage", 40f);
        PROXIMITY = objParams.getFloat("proximity", 1f);
        FLY_SPEED = objParams.getFloat("fly speed", 0.0001f);
    }

    /** The player for targeting. */
    private Raft targetRaft;
    /** The two locations the Siren will be located. */
    private Vector2 location1 = new Vector2();
    private Vector2 location2 = new Vector2();
    /** The two directions the Siren will be need to move in. */
    private Vector2 direction1 = new Vector2();
    private Vector2 direction2 = new Vector2();
    /** Vector applied as a force to the Siren. */
    private Vector2 moveVector = new Vector2();
    private boolean fromLocation1 = true;
    /** FSM to control Siren AI */
    private StateMachine<Siren, SirenController> stateMachine;
    /** To keep track how much time has passed. */
    private long timeStamp = 0L;
    private boolean timeStamped = false;
    /** Whether this Siren has just attacked the player. */
    private boolean hasAttacked;
    /** Constants that determine time in each state for range of attack. */
    private static float PROXIMITY = 1f;
    private static long IDLE_TIME;
    private static long SINGING_TIME;
    private static float SINGING_RANGE;
    private static Vector2 SINGING_FORCE;
    private static float ATTACK_RANGE;
    private static float ATTACK_DAMAGE;
    private static float FLY_SPEED;

    /**
     * Constructor for the Siren.
     * @param position1 The starting position of the Siren.
     * @param position2 The secondary position the Siren will fly to.
     * @param targetRaft The player to target.
     */
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

    /**
     * Method to switch the state of the FSM when applicable.
     * @param dt the time increment
     */
    public void update(float dt) {
        physicsObject.getBody().setLinearVelocity(moveVector);
        stateMachine.update();
    }
    /** @return this Siren's FSM */
    public StateMachine<Siren, SirenController> getStateMachine(){ return this.stateMachine; }
    /** @return this Siren's ObjectType for collision. */
    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.SIREN;
    }
    /**
     * Set this Siren's target
     * @param targetRaft the player
     */
    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }

    // Setting movement

    /**
     * Changes the force vector of this Siren.
     * @param direction1 whether to go with the direction1 vector.
     */
    public void setMoveVector(boolean direction1) {
        if(direction1) this.moveVector = this.direction1;
        else this.moveVector = this.direction2;
    }
    /** Set the move vector to zero so the Siren comes to a rest. */
    public void stopMove(){ this.moveVector.setZero(); }

    // Time

    /** Method to start recording time. */
    public void setTimeStamp(){
        if(!timeStamped) {
            timeStamp = TimeUtils.millis();
            timeStamped = true;
        }
    }
    /** Method to allow a new time stamp. */
    public void resetTimeStamp(){ timeStamped = false; }
    /** Whether this Siren has elapsed its idling time. */
    public boolean isPastIdleCooldown(){ return TimeUtils.timeSinceMillis(timeStamp) > IDLE_TIME; }
    /** Whether this Siren has elapsed its singing time. */
    public boolean isDoneSinging(){ return TimeUtils.timeSinceMillis(timeStamp) > SINGING_TIME; }

    // Changing location
    /** @return whether Siren is flying from its spawn location. */
    public boolean fromFirstLocation(){ return fromLocation1; }
    /** @return whether the Siren is flying from its secondary location. */
    public boolean fromSecondLocation(){ return !fromLocation1; }
    /** Set that the Siren is flying from its spawn location. */
    public void setFromFirstLocation(){ fromLocation1 = true; }
    /** Set that the Siren is flying from its secondary location. */
    public void setFromSecondLocation(){ fromLocation1 = false; }
    /** @return when the Siren has reached its destination. */
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

    /** @return whether the player is in attack range of this Siren. */
    public boolean inAttackRange(){
        return targetRaft.getPosition().sub(getPosition()).len() < ATTACK_RANGE;
    }
    /** @return whether the player is in range and the Siren is attack mode.
     *  Resets
     */
    public boolean willAttack(){
        hasAttacked = stateMachine.isInState(SirenController.ATTACKING) && inAttackRange();
        return hasAttacked;
    }
    /** @return whether the Siren has just attacked. */
    public boolean hasAttacked(){ return hasAttacked; }
    /** Reset hasAttacked. */
    public void resetHasAttacked(){ hasAttacked = false; }
    /** Get how much damage is done to the player. */
    public static float getAttackDamage(){ return ATTACK_DAMAGE; }

}
