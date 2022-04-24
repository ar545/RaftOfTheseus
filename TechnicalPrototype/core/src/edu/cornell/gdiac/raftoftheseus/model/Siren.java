package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class Siren extends GameObject {

    /**
     * Method to fill in all constants for the Siren
     * @param objParams the JsonValue with heading "siren".
     */
    public static void setConstants(JsonValue objParams){
        IDLE_TIME = objParams.getLong("idle time");
        SINGING_TIME = objParams.getLong("singing time");
        ATTACK_RANGE = objParams.getFloat("attack range");
        PROXIMITY = objParams.getFloat("proximity");
        FLY_SPEED = objParams.getFloat("fly speed");
        TAKE_OFF_SPEED = objParams.getFloat("take off speed");
        COOL_DOWN = objParams.getLong("cool down");
        STUN_TIME = objParams.getLong("stun time");
        TEXTURE_SCALE = objParams.getFloat("texture scale");
        IDLE_AS = objParams.getFloat("idle animation speed");
        SINGING_AS = objParams.getFloat("singing animation speed");
        TAKE_OFF_AS = objParams.getFloat("take off animation speed");
        LANDING_AS = objParams.getFloat("landing animation speed");
        FLYING_AS = objParams.getFloat("flying animation speed");
        IDLE_FRAMES = objParams.getInt("idle frames");
        SINGING_FRAMES = objParams.getInt("singing frames");
        TAKE_OFF_FRAMES = objParams.getInt("take off frames");
        LANDING_FRAMES = objParams.getInt("landing frames");
        FLYING_FRAMES = objParams.getInt("flying frames");
        IDLE_SF = objParams.getInt("idle start frame");
        SINGING_SF = objParams.getInt("singing start frame");
        TAKE_OFF_SF = objParams.getInt("take off start frame");
        LANDING_SF = objParams.getInt("landing start frame");
        FLYING_SF = objParams.getInt("flying start frame");
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
    private boolean animationDone;
    /** Constants that determine time in each state for range of attack. */
    private static float PROXIMITY;
    private static long IDLE_TIME;
    private static long SINGING_TIME;
    private static float ATTACK_RANGE;
    private static float TAKE_OFF_SPEED;
    private static float FLY_SPEED;
    private static long COOL_DOWN;
    private static long STUN_TIME;
    private static float TEXTURE_SCALE;
    private static float IDLE_AS;
    private static float SINGING_AS;
    private static float TAKE_OFF_AS;
    private static float LANDING_AS;
    private static float FLYING_AS;
    private static int IDLE_FRAMES;
    private static int SINGING_FRAMES;
    private static int TAKE_OFF_FRAMES;
    private static int LANDING_FRAMES;
    private static int FLYING_FRAMES;
    private static int IDLE_SF;
    private static int SINGING_SF;
    private static int TAKE_OFF_SF;
    private static int LANDING_SF;
    private static int FLYING_SF;
    /**
     * Constructor for the Siren.
     * @param position1 The starting position of the Siren.
     * @param position2 The secondary position the Siren will fly to.
     * @param targetRaft The player to target.
     */
    public Siren(Vector2 position1, Vector2 position2, Raft targetRaft) {
        physicsObject = new WheelObstacle(1.2f);
        setPosition(position1);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY;
        physicsObject.getFilterData().maskBits = MASK_SIREN;

        location1.set(position1);
        location2.set(position2);
        direction1.set(position2.cpy().sub(position1).nor());
        direction2.set(position1.cpy().sub(position2).nor());
        moveVector.set(0.0f, 0.0f);
        this.targetRaft = targetRaft;
        stateMachine = new DefaultStateMachine<>(this, SirenState.IDLE);
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

    /**
     * Method to change the size of the moveVector after first normalizing it.
     * @param isFlying
     */
    public void scaleMoveVector(boolean isFlying){
        if(isFlying) moveVector.nor().scl(FLY_SPEED);
        else moveVector.nor().scl(TAKE_OFF_SPEED);
    }


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
            dist = getPosition().cpy().sub(location2).len();
        } else {
            dist = getPosition().cpy().sub(location1).len();
        }
//        System.out.println(dist);
//        System.out.println(dist < PROXIMITY);
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
        hasAttacked = stateMachine.getCurrentState() == SirenState.SINGING && inAttackRange() && cooldownElapsed();
        if(hasAttacked) {
            resetAttackStamp();
            setAttackStamp();
        }
        return hasAttacked;
    }

    /** Get how much damage is done to the player. */
    public Vector2 getTargetDirection() { return targetRaft.getPosition().cpy().sub(getPosition()).nor(); }

    // Stunned
    public boolean isHit(){ return isHit; }
    public void setHit(boolean h){
        if (!(stateMachine.getCurrentState() == SirenState.STUNNED)){
            isHit = h;
        }
    }

    // Animation
    public void setAnimationDone(boolean done){ animationDone = done;}
    public boolean isAnimationDone(){
        if(animationDone) {
            setAnimationDone(false);
            return true;
        }
        return false;
    }

    /**
     * Method to set animation based on the time elapsed in the game.
     * @param dt the current time in the game.
     */
    public void setAnimationFrame(float dt) {
        // Get frame number
        timeElapsed += dt;
        System.out.println(stateMachine.getCurrentState());
        switch(stateMachine.getCurrentState()){
            case IDLE:
                setFrame(IDLE_AS, IDLE_FRAMES, IDLE_SF, false);
                break;
            case SINGING:
                setFrame(SINGING_AS, SINGING_FRAMES, SINGING_SF, false);
                break;
            case LANDING:
                setAnimationDone(setFrame(LANDING_AS, LANDING_FRAMES, LANDING_SF, true));
                break;
            case TAKEOFF:
                setAnimationDone(setFrame(TAKE_OFF_AS, TAKE_OFF_FRAMES, TAKE_OFF_SF, false));
                break;
            case FLYING:
                setFrame(FLYING_AS, FLYING_FRAMES, FLYING_SF, true);
                break;
            case STUNNED:
                break;
        }
    }

    /**
     * Sets the frame of the animation based on the FSM and time given.
     * @param animationSpeed how many seconds should pass between each frame.
     * @param frames the number of frames this animation has.
     * @param start which frame in the FilmStrip the animation starts on.
     * @param reverse whether the animation should be drawn backwards.
     * @return whether it has reached the last animation image.
     */
    private boolean setFrame(float animationSpeed, int frames, int start, boolean reverse){
        if (timeElapsed > animationSpeed){
            timeElapsed = 0;
            frameCount += 1;
        }
        frame = start + (reverse ? (frames - 1) - frameCount % frames : frameCount % frames);
        return reverse ? frame == start : frame == frames - 1 + start;
    }

    int frameCount = 0;
    float timeElapsed = 0;
    int frame = 0;

    /**
     * Method to reset the frameCount to 0 to ensure the next animation
     */
    public void resetFrame(){
        frameCount = 0;
    }

    /**
     * Set the appropriate image frame first before drawing the Siren.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas){
        ((FilmStrip) texture).setFrame(frame);
        super.draw(canvas);
    }
}
