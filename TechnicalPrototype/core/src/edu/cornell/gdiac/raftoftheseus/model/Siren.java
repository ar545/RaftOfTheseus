package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.model.unused.HydraState;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Siren extends GameObject {

    /**
     * Method to fill in all constants for the Siren
     * @param objParams the JsonValue with heading "siren".
     */
    public static void setConstants(JsonValue objParams){
        IDLE_TIME = objParams.getLong("idle time");
        SINGING_TIME = objParams.getLong("singing time");
        ATTACK_RANGE = objParams.getFloat("attack range");
        ATTACK_DAMAGE = objParams.getFloat("attack damage");
        PROXIMITY = objParams.getFloat("proximity");
        FLY_SPEED = objParams.getFloat("fly speed");
        COOL_DOWN = objParams.getLong("cool down");
        STUN_TIME = objParams.getLong("stun time");
        TEXTURE_SCALE = objParams.getFloat("texture scale");
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
    private StateMachine<Siren, SirenState> stateMachine;
    /** To keep track how much time has passed. */
    private long timeStamp = 0L;
    private boolean timeStamped = false;
    private long attackStamp = 0L;
    private boolean attackStamped = false;
    /** Whether this Siren has just attacked the player. */
    private boolean isHit = false;
    private boolean hasAttacked;
    /** Constants that determine time in each state for range of attack. */
    private static float PROXIMITY = 1f;
    private static long IDLE_TIME;
    private static long SINGING_TIME;
    private static Vector2 SINGING_FORCE;
    private static float ATTACK_RANGE;
    private static float ATTACK_DAMAGE;
    private static float FLY_SPEED;
    private static long COOL_DOWN;
    private static long STUN_TIME;
    private static float TEXTURE_SCALE;

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
        direction1.set(position2.cpy().sub(position1).scl(FLY_SPEED));
        direction2.set(position1.cpy().sub(position2).scl(FLY_SPEED));
        moveVector.set(0.0f, 0.0f);
        this.targetRaft = targetRaft;
        stateMachine = new DefaultStateMachine<Siren, SirenState>(this, SirenState.IDLE);
    }

    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(0,(texture.getRegionHeight()*textureScale.y - getHeight())/2f);
    }

    /**
     * Method to switch the state of the FSM when applicable.
     * @param dt the time increment
     */
    public void update(float dt) {
        physicsObject.setLinearVelocity(moveVector);
        stateMachine.update();
    }
    /** @return this Siren's FSM */
    public StateMachine<Siren, SirenState> getStateMachine(){ return this.stateMachine; }
    /** @return this Siren's ObjectType for collision. */
    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.SIREN;
    }

    // Setting movement

    /**
     * Changes the force vector of this Siren.
     * @param direction1 whether to go with the direction1 vector.
     */
    public void setMoveVector(boolean direction1) {
        if(direction1) this.moveVector.set(this.direction1);
        else this.moveVector.set(this.direction2);
    }
    /** Set the move vector to zero so the Siren comes to a rest. */
    public void stopMove(){ this.moveVector.setZero(); }

    // Time

    /** Method to start recording time for transitioning between states. */
    public void setTimeStamp(){
        if(!timeStamped) {
            timeStamp = TimeUtils.millis();
            timeStamped = true;
        }
    }
    /** Method to allow a new time stamp. */
    public void resetTimeStamp(){ timeStamped = false; }
    /** Method to start recording time between firing */
    public void setAttackStamp(){
        if(!attackStamped){
            attackStamp = TimeUtils.millis();
            attackStamped = true;
        }
    }
    /** Method to allow a new time stamp. */
    public void resetAttackStamp(){ attackStamped = false; }

    // Cool downs

    /** @return Whether this Siren has elapsed its idling time. */
    public boolean isPastIdleCooldown(){ return TimeUtils.timeSinceMillis(timeStamp) > IDLE_TIME; }
    /** @return Whether this Siren has elapsed its singing time. */
    public boolean isDoneSinging(){ return TimeUtils.timeSinceMillis(timeStamp) > SINGING_TIME; }
    /** @return Whether this Siren can fire again. */
    public boolean cooldownElapsed(){
        return TimeUtils.timeSinceMillis(attackStamp) > COOL_DOWN;
    }
    /** @return Whether this Siren is not stunned anymore. */
    public boolean stunElapsed(){ return TimeUtils.timeSinceMillis(timeStamp) > STUN_TIME; }

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
        return targetRaft.getPosition().cpy().sub(getPosition()).len() < ATTACK_RANGE;
    }
    /** @return whether the player is in range and the Siren is attack mode.
     *  Resets
     */
    public boolean willAttack(){
        hasAttacked = stateMachine.isInState(SirenState.SINGING) && inAttackRange();
        return hasAttacked;
    }

    /** Get how much damage is done to the player. */
    public static float getAttackDamage(){ return ATTACK_DAMAGE; }
    public Vector2 getTargetDirection() { return getPosition().sub(targetRaft.getPosition()).nor(); }

    // Stunned
    public boolean isHit(){ return isHit; }
    public void setHit(boolean h){
        if (!stateMachine.isInState(SirenState.STUNNED)){
            isHit = h;
        }
    }

}
