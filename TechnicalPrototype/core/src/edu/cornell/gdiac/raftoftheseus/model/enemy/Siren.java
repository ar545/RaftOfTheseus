package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.util.Animated;
import edu.cornell.gdiac.raftoftheseus.model.util.FrameCalculator;
import edu.cornell.gdiac.raftoftheseus.model.GameObject;
import edu.cornell.gdiac.raftoftheseus.model.Raft;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class Siren extends Enemy<Siren, SirenState> implements Animated {

    /**
     * Method to fill in all constants for the Siren
     * @param objParams the JsonValue with heading "siren".
     */
    public static void setConstants(JsonValue objParams){
        IDLE_TIME = objParams.getFloat("idle time");
        SINGING_TIME = objParams.getFloat("singing time");
        ATTACK_RANGE = objParams.getFloat("attack range");
        HEAR_RANGE = objParams.getFloat("hearing range");
        PROXIMITY = objParams.getFloat("proximity");
        FLY_SPEED = objParams.getFloat("fly speed");
        TAKE_OFF_SPEED = objParams.getFloat("take off speed");
        COOL_DOWN = objParams.getFloat("cool down");
        STUN_TIME = objParams.getFloat("stun time");
        TEXTURE_SCALE = objParams.getFloat("texture scale");
        RADIUS = objParams.getFloat("radius");
        IDLE_AS = objParams.getFloat("idle animation speed");
        SINGING_AS = objParams.getFloat("singing animation speed");
        TAKE_OFF_AS = objParams.getFloat("take off animation speed");
        LANDING_AS = objParams.getFloat("landing animation speed");
        FLYING_AS = objParams.getFloat("flying animation speed");
        FLASHING_AS = objParams.getFloat("flashing speed");
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

    /** 2 Vector caches to store where the siren is and where it will need to go. */
    private Vector2 start = new Vector2();
    private Vector2 finish = new Vector2();
    /** The index in waypoints of the next waypoint to fly to. */
    private int waypoint;
    private Array<Vector2> waypoints;
    /** Vector applied as a force to the Siren. */
    private Vector2 moveVector = new Vector2();
    /** FSM to control Siren AI */
    private StateMachine<Siren, SirenState> stateMachine;
    /** Animation Calculator */
    private FrameCalculator fc = new FrameCalculator();
    /** To keep track how much time has passed. */
    private long timeStamp = 0L;
    private boolean timeStamped = false;

    /** Whether this Siren has just attacked the player. */
    private boolean isHit = false;
    private boolean hasAttacked;
    private boolean animationDone;
    /** Constants that determine time in each state for range of attack. */
    private static float PROXIMITY;
    protected static float IDLE_TIME;
    protected static float SINGING_TIME;
    private static float ATTACK_RANGE;
    private static float HEAR_RANGE;
    private static float TAKE_OFF_SPEED;
    private static float FLY_SPEED;
    protected static float COOL_DOWN;
    protected static float STUN_TIME;
    private static float TEXTURE_SCALE;
    private static float RADIUS;
    private static float IDLE_AS;
    private static float SINGING_AS;
    private static float TAKE_OFF_AS;
    private static float LANDING_AS;
    private static float FLYING_AS;
    private static float FLASHING_AS;
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
     * General constructor for Siren with 3 or more positions.
     * @param positions 3 or more positions
     * @param raft the player
     */
    public Siren(Array<Vector2> positions, Raft raft){
        super(raft);
        waypoints = new Array<>();
        for(Vector2 pos : positions){
            waypoints.add(new Vector2(pos));
        }
        setParameters();
    }

    /**
     * Helper constructor when the Siren is given only one or two positions
     * @param positions 1 or 2 positions
     * @param raft the player
     */
    private Siren(Raft raft, Array<Vector2> positions){
        super(raft);
        waypoints = positions;
        setParameters();
    }

    /**
     * Constructor for the Siren.
     * @param position1 The starting position of the Siren.
     * @param position2 The secondary position the Siren will fly to.
     * @param raft The player to target.
     */
    public Siren(Vector2 position1, Vector2 position2, Raft raft) {
        this(raft, new Array<Vector2>(){{add(new Vector2(position1)); add(new Vector2(position2));}});
    }

    /**
     * Constructor for a stationary Siren
     * @param position the location of the siren
     * @param raft the player
     */
    public Siren(Vector2 position, Raft raft){
        this(raft, new Array<Vector2>(){{add(new Vector2(position));}});
    }

    /**
     * Constructor helper method to define common parameters.
     * Must be called at the end of a constructor.
     */
    private void setParameters(){
        physicsObject = new WheelObstacle(RADIUS);
        setPosition(waypoints.get(0));
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY;
        physicsObject.getFilterData().maskBits = MASK_SIREN;
        physicsObject.setSensor(true);
        moveVector.set(0.0f, 0.0f);
        stateMachine = new DefaultStateMachine<>(this, SirenState.IDLE);
        waypoint = 1;
    }

    /** Set the texture location relative to the physics body. */
    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2();
    }

    /** @param dt the time increment, to switch the state of the FSM when applicable. */
    @Override
    public void updateAI(float dt) {
        physicsObject.setLinearVelocity(moveVector);
        stateMachine.update();
    }
    /** @return this Siren's FSM */
    @Override
    public StateMachine<Siren, SirenState> getStateMachine(){ return this.stateMachine; }
    /** @return this Siren's ObjectType for collision. */
    @Override
    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.SIREN;
    }

    // Setting movement

    /**
     * Changes the force vector of this Siren.
     */
    public void setMoveVector() {
        start.set(getPosition());
        finish.set(waypoints.get(waypoint));
        // Defensive code for 2 of the same position
        if (finish.cpy().sub(start).len() < PROXIMITY) return;
        moveVector.set(finish.cpy().sub(start));
    }
    /** Set the move vector to zero so the Siren comes to a rest. */
    public void stopMove(){ this.moveVector.setZero(); }

    /** Method to change the indicator of the next flying location. */
    public void incrementWaypoint(){
        if(waypoint == waypoints.size - 1) waypoint = 0;
        else waypoint++;
    }

    /** @return whether this Siren stays in place or not. */
    public boolean isStationary(){
        return waypoints.size == 1;
    }

    /**
     * Method to change the size of the moveVector after first normalizing it.
     * @param isFlying
     */
    public void scaleMoveVector(boolean isFlying){
        if(isFlying) moveVector.nor().scl(FLY_SPEED);
        else moveVector.nor().scl(TAKE_OFF_SPEED);
    }

    // Changing location
    /** @return when the Siren has reached its destination. */
    public boolean nearLanding(){
        float dist = getPosition().cpy().sub(waypoints.get(waypoint)).len();
        return dist < PROXIMITY;
    }

    // Attacking player
    public boolean canHear(){ return inRange(HEAR_RANGE) && stateMachine.isInState(SirenState.SINGING);}
    /** @return whether the player is in attack range of this Siren. */
    public boolean inAttackRange(){ return inRange(ATTACK_RANGE); }
    /** @return whether the player is in range and the Siren is attack mode. */
    public boolean willAttack(){
        hasAttacked = stateMachine.getCurrentState() == SirenState.SINGING && inAttackRange() && attackTimer.hasTimeElapsed(COOL_DOWN, false);
        if(hasAttacked) {
            attackTimer.resetTimeStamp();
            attackTimer.setTimeStamp();
        }
        return hasAttacked;
    }

    /** Get how much damage is done to the player. */
    @Override
    public Vector2 getTargetDirection(Vector2 playerCurrentVelocity) {
        start.set(getPosition());
        finish.set(player.getPosition().add(player.getLinearVelocity()).add(playerCurrentVelocity));
        return finish.cpy().sub(start).nor();
    }

    // Stunned
    public boolean setHit(){
        if (stateMachine.isInState(SirenState.SINGING) || stateMachine.isInState(SirenState.IDLE)){
            stateMachine.changeState(SirenState.STUNNED);
            fc.setFlash(true);
            return true;
        }
        return false;
    }

    @Override
    public void setStunTexture(TextureRegion value) {

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

    @Override
    public FrameCalculator getFrameCalculator() { return fc; }
    @Override
    public void setAnimationFrame(float dt) {
        // Get frame number
        fc.addTime(dt);
//        System.out.println(stateMachine.getCurrentState());
        switch(stateMachine.getCurrentState()){
            case IDLE:
                fc.setFrame(IDLE_AS, IDLE_SF, IDLE_FRAMES, false);
                break;
            case SINGING:
                fc.setFrame(SINGING_AS, SINGING_SF, SINGING_FRAMES, false);
                break;
            case LANDING:
                setAnimationDone(fc.setFrame(LANDING_AS, LANDING_SF, LANDING_FRAMES, true));
                break;
            case TAKEOFF:
                setAnimationDone(fc.setFrame(TAKE_OFF_AS, TAKE_OFF_SF, TAKE_OFF_FRAMES, false));
                break;
            case FLYING:
                fc.setFrame(FLYING_AS, FLYING_SF, FLYING_FRAMES, true);
                break;
            case STUNNED:
                fc.setFrame(SINGING_SF);
                fc.checkFlash(FLASHING_AS);
                break;
        }
        setTextureXOrientation(true);
    }

    /**
     * Set the appropriate image frame first before drawing the Siren.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas){
        ((FilmStrip) texture).setFrame(fc.getFrame());
        if(fc.getFlash()) super.draw(canvas, Color.RED);
        else super.draw(canvas);
    }
}
