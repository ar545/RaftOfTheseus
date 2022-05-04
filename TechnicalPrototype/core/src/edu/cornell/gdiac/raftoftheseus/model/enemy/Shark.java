package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.model.*;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.util.Animated;
import edu.cornell.gdiac.raftoftheseus.model.util.FrameCalculator;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class Shark extends Enemy<Shark, SharkState> implements Animated {
    /**
     * Method to fill in all constants for the Shark
     * @param objParams the JsonValue with heading "shark".
     */
    public static void setConstants(JsonValue objParams){
        CONTACT_DAMAGE = objParams.getFloat("damage");
        HEAR_RANGE = objParams.getFloat("hearing range");
        APPROACH_SPEED = objParams.getFloat("approach speed");
        APPROACH_RANGE = objParams.getFloat("approach range");
        ATTACK_WINDUP_TIME = objParams.getFloat("warm up");
        ATTACK_SPEED = objParams.getFloat("attack speed");
        ATTACK_RANGE = objParams.getFloat("attack range");
        ATTACK_COOLDOWN_TIME = objParams.getFloat("cool down");
        STUN_TIME = objParams.getLong("stun time");

        TEXTURE_SCALE = objParams.getFloat("texture scale");
        RADIUS = objParams.getFloat("radius");

        SWIM_AS = objParams.getFloat("swim animation speed");
        BITE_AS = objParams.getFloat("bite animation speed");
        SWIM_FRAMES = objParams.getInt("swim frames");
        BITE_FRAMES = objParams.getInt("bite frames");
        SWIM_SF = objParams.getInt("swim start frame");
        BITE_SF = objParams.getInt("bite start frame");
    }

    /** The player for targeting. */
    private Vector2 aimDirection = new Vector2(0, 0);
    /** How the Shark wants to move. */
    private Vector2 desiredVelocity = new Vector2();
    /** FSM to control Shark AI */
    private StateMachine<Shark, SharkState> stateMachine;
    /** FrameController for animation. */
    private FrameCalculator fc = new FrameCalculator(SWIM_SF);
    /** To keep track how much time has passed. */
    private long timeStamp = 0L;
    private boolean timeStamped = false;
    /** Whether the Shark can see the player. (must be set by WorldController) */
    public boolean canSee;
    /** Constants that determine time in each state for range of attack. */
    public static float CONTACT_DAMAGE;
    public static float HEAR_RANGE;
    public static float APPROACH_SPEED;
    public static float APPROACH_RANGE;
    public static float ATTACK_WINDUP_TIME;
    public static float ATTACK_SPEED;
    public static float ATTACK_RANGE;
    public static float ATTACK_COOLDOWN_TIME;
    public static float STUN_TIME;

    private static float TEXTURE_SCALE;
    private static float RADIUS;

    private static float SWIM_AS;
    private static float BITE_AS;
    private static int SWIM_FRAMES;
    private static int BITE_FRAMES;
    private static int SWIM_SF;
    private static int BITE_SF;

    /**
     * Constructor for Shark
     * @param position start position
     * @param raft the player
     */
    public Shark(Vector2 position, Raft raft){
        super(raft);
        physicsObject = new WheelObstacle(RADIUS);
        setPosition(position);
        physicsObject.setRestitution(.75f);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY;
        physicsObject.getFilterData().maskBits = MASK_ENEMY;
        desiredVelocity.set(0.0f, 0.0f);
        stateMachine = new DefaultStateMachine<>(this, SharkState.IDLE);
        canSee = false;
    }

    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(0, 0);
    }

    /**
     * Method to switch the state of the FSM when applicable.
     * @param dt the time increment
     */
    public void updateAI(float dt) {
        if(!isDestroyed()) {
            Vector2 currentVelocity = physicsObject.getLinearVelocity().cpy();
            Vector2 f = currentVelocity.sub(desiredVelocity).scl(-2.0f * physicsObject.getMass());
            physicsObject.getBody().applyForce(f, getPosition(), true);
//            physicsObject.setLinearVelocity(desiredVelocity);
            stateMachine.update();
        }
    }
    /** @return this Shark's FSM */
    public StateMachine<Shark, SharkState> getStateMachine(){ return this.stateMachine; }
    /** @return this Shark's ObjectType for collision. */
    public ObjectType getType() {
        return ObjectType.SHARK;
    }

    // Attacking player
    public boolean isAggressive() {
        return stateMachine.isInState(SharkState.APPROACH) || stateMachine.isInState(SharkState.PAUSE_BEFORE_ATTACK)
                || stateMachine.isInState(SharkState.ATTACK) || stateMachine.isInState(SharkState.PAUSE_AFTER_ATTACK);
    }

    // Can collide with the player and damage them
    public boolean canHurtPlayer() {
        return stateMachine.isInState(SharkState.ATTACK) || stateMachine.isInState(SharkState.PAUSE_AFTER_ATTACK);
    }

    /** Whether the player can hear danger music because of this Shark */
    public boolean canHear(){
        return inRange(HEAR_RANGE) && isAggressive();
    }

    /** @return whether the player is in line-of-sight of this Shark. */
    public boolean canSee(){
        return canSee;
    }

    /** @return whether the player is in a given range of this Shark. */
    public boolean inRange(float dist){
        return getTargetDistance() < dist;
    }

    /**  */
    @Override
    public Vector2 getTargetDirection(Vector2 playerCurrentVelocity) {
        return player.getPosition().cpy().add(player.getLinearVelocity().cpy().scl(0.25f)).sub(getPosition()).nor();
    }

    public void setDesiredVelocity(float speed, boolean aimingAtPlayer) {
        if (speed == 0.0f)
            desiredVelocity.set(0,0);
        else if(aimingAtPlayer) {
            aimDirection = getTargetDirection(null);
            desiredVelocity.set(aimDirection).scl(speed);
        } else {
            // aim using last-used direction
            desiredVelocity.set(aimDirection).scl(speed);
        }
    }

    // Stunned
    public boolean setHit(){
        if (!(stateMachine.isInState(SharkState.STUNNED) || stateMachine.isInState(SharkState.DYING))){
            stateMachine.changeState(SharkState.STUNNED);
            fc.setFlash(false);
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
        switch(stateMachine.getCurrentState()){
            case STUNNED:
                fc.setFrame(SWIM_SF);
                fc.checkFlash(SWIM_AS);
                break;
            case IDLE:
            case APPROACH:
            case PAUSE_BEFORE_ATTACK:
            case PAUSE_AFTER_ATTACK:
            case DYING:
                fc.setFlash(false);
                fc.setFrame(SWIM_AS, SWIM_SF, SWIM_FRAMES, false);
                break;
            case ATTACK:
                fc.setFlash(false);
                fc.setFrame(BITE_AS, BITE_SF, BITE_FRAMES, false);
                break;
        }
        setTextureXOrientation(true);
    }

    public boolean isDoneWithAttackAnimation() {
        return stateMachine.isInState(SharkState.ATTACK) && fc.isFrame(BITE_SF, BITE_FRAMES, false);
    }

    /**
     * Set the appropriate image frame first before drawing the Shark.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas){
        ((FilmStrip) texture).setFrame(fc.getFrame());
        if(fc.getFlash()) super.draw(canvas, Color.RED);
        else super.draw(canvas);
    }

    public void takeDamage() {
        // nothing happens (for now)
    }
}
