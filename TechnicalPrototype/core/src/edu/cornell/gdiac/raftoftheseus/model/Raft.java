package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.projectile.Spear;
import edu.cornell.gdiac.raftoftheseus.model.util.Animated;
import edu.cornell.gdiac.raftoftheseus.model.util.FrameCalculator;
import edu.cornell.gdiac.raftoftheseus.model.util.TextureHolder;
import edu.cornell.gdiac.raftoftheseus.model.util.Timer;
import edu.cornell.gdiac.raftoftheseus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.SimpleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

import static edu.cornell.gdiac.raftoftheseus.model.LevelModel.BOB_AMP;
import static edu.cornell.gdiac.raftoftheseus.model.LevelModel.BOB_TIME;

/**
 * Model class for the player raft.
 */
public class Raft extends GameObject implements Animated {

    /**
     * @param objParams the "raft" child of object settings
     * Method to set Raft parameters
     * */
    public static void setConstants(JsonValue objParams){
        MOVE_COST = objParams.getFloat("move cost");
        WC_MOVE_BUFF = objParams.getFloat("with current movement buff");
        AC_MOVE_BUFF = objParams.getFloat("against current movement buff");
        CURRENT_BUFF_SCOPE = objParams.getFloat("current buff scope");
        MAXIMUM_PLAYER_HEALTH = objParams.getFloat("max health");
        INITIAL_PLAYER_HEALTH = objParams.getFloat("initial health");
        DAMPING = objParams.getFloat("damping");
        THRUST = objParams.getFloat("thrust");
        MIN_SPEED = objParams.getFloat("min speed");
        MAX_SPEED = objParams.getFloat("max speed");
        MAX_SPEED_AGAINST_CURRENT = objParams.getFloat("max speed against current");
        OBJ_WIDTH = objParams.getFloat("width");
        OBJ_HEIGHT = objParams.getFloat("height");
        SENSOR_RADIUS = objParams.getFloat("sensor size");
        TEXTURE_SCALE = objParams.getFloat("texture scale");
        HORIZONTAL_OFFSET = objParams.getFloat("horizontal offset");
        FORCE_DURATION = objParams.getFloat("enemy bullet duration");
        IDLE_AS = objParams.getFloat("idle animation speed");
        IDLE_SF = objParams.getInt("idle starting frame");
        IDLE_FC = objParams.getInt("idle frames");
        SHOOTING_AS = objParams.getFloat("shooting animation speed");
        SHOOTING_SF = objParams.getInt("shooting starting frame");
        SHOOTING_FC = objParams.getInt("shooting frames");
        AURA_AS = objParams.getFloat("aura animation speed");
        AURA_SF = objParams.getInt("aura start frame");
        AURA_FC = objParams.getInt("aura frame count");
    }

    // CONSTANTS
    /** Movement cost for a unit distance **/
    private static float MOVE_COST;
    /** Movement cost multiplier for moving with the current */
    private static float AC_MOVE_BUFF;
    private static float CURRENT_BUFF_SCOPE;
    private static float WC_MOVE_BUFF;

    // ATTRIBUTES
    /** A physics object used for collisions with interactable objects which shouldn't push the player (wood, goal, etc) */
    private SimpleObstacle interactionSensor;
    /** The player's movement input */
    private Vector2 movementInput = new Vector2(0f,0f);
    /** The health of the raft. This must be >=0. */
    private float health;
    /** Maximum player health */
    public static float MAXIMUM_PLAYER_HEALTH;
    /** Initial player health */
    public static float INITIAL_PLAYER_HEALTH;
//    /** Whether the raft is actively firing */
//    private boolean isCharging;
    private boolean canFire;
    /** Whether the raft is currently being damaged (feedback purposes) */
    private boolean isDamaged;
    /** The amount to slow the character down, while they aren't moving */
    private static float DAMPING;
    /** The amount to accelerate the character */
    private static float THRUST;
    /** The maximum character speed allowed */
    private static float MAX_SPEED_AGAINST_CURRENT;
    private static float MAX_SPEED;
    private static float MIN_SPEED;
    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();
    private final Vector2 externalForce = new Vector2();
    /** Size constants */
    private static float OBJ_WIDTH;
    private static float OBJ_HEIGHT;
    private static float SENSOR_RADIUS;
    /** How long to keep applying */
    private static float FORCE_DURATION;

    // ANIMATION
    private static float HORIZONTAL_OFFSET;
    /** How much to enlarge the raft. */
    private static float TEXTURE_SCALE;
    /** The animation speed for the raft. */
    private static float IDLE_AS;
    private static float SHOOTING_AS;
    /** Which frame to start on the filmstrip with this animation. */
    private static int IDLE_SF;
    private static int SHOOTING_SF;
    /** The number of frames for this animation. */
    private static int IDLE_FC;
    private static int SHOOTING_FC;
    /** The player's spear when held in inventory. */
    private Spear spear;
    /** Frame calculator for animation. */
    private FrameCalculator fc = new FrameCalculator(IDLE_SF);
    // AURA
    private FrameCalculator aurafc = new FrameCalculator(AURA_SF);
    /** animation speed of the attack animation */
    private static float AURA_AS;
    /** start frame of the attack animation */
    private static int AURA_SF;
    /** frame count of the attack animation */
    private static int AURA_FC;
    private TextureHolder attackAura;

    /** restore the player health to half life */
    public void halfLife() { addHealth(MAXIMUM_PLAYER_HEALTH / 2); }

    /** raft state: idle represent not actively trying to create a spear, charging means in the process of creating spears */
    private enum RaftState{ IDLE,  CHARGING }
    /** default state should be idle */
    private RaftState raftState = RaftState.IDLE;

    // METHODS
    /** Constructor for Raft object
     * @param position: position of raft
     */
    public Raft(Vector2 position) {
        physicsObject = new CapsuleObstacle(OBJ_WIDTH, OBJ_HEIGHT);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER;
        physicsObject.getFilterData().maskBits = MASK_PLAYER;
        this.health = INITIAL_PLAYER_HEALTH;

        interactionSensor = new WheelObstacle(SENSOR_RADIUS);
        interactionSensor.setSensor(true);
        interactionSensor.setBodyType(BodyDef.BodyType.DynamicBody);
        interactionSensor.getFilterData().categoryBits = CATEGORY_PLAYER_SENSOR;
        interactionSensor.getFilterData().maskBits = MASK_PLAYER_SENSOR;
        canFire = false;
        isDamaged = false; /* Whether the player was very recently damaged. */
    }

    /**
     * Returns the type of this object.
     *
     * We use this instead of runtime-typing for performance reasons.
     *
     * @return the type of this object.
     */
    public ObjectType getType() {
        return ObjectType.RAFT;
    }

    @Override
    public void deactivatePhysics(World world) {
        super.deactivatePhysics(world);
        interactionSensor.deactivatePhysics(world);
    }

    @Override
    public void activatePhysics(World world) {
        super.activatePhysics(world);
        interactionSensor.activatePhysics(world);
        interactionSensor.getBody().setUserData(this);
    }

    @Override
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        interactionSensor.drawDebug(canvas);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        interactionSensor.setPosition(physicsObject.getPosition());
        interactionSensor.update(dt);
    }

    /** @return the player movement input. */
    protected Vector2 getMovementInput() { return movementInput; }
    /** @return whether the player is moving or not. */
    public boolean isMoving() {
        boolean isDrifting = physicsObject.getLinearVelocity().len() > MIN_SPEED;
        boolean isRowing = !movementInput.isZero();
        return isDrifting || isRowing;
    }
    /** Getter and setters for whether the player was recently damaged. */
    public boolean isDamaged() { return isDamaged; }
    public void setDamaged(boolean damaged) { this.isDamaged = damaged; }
    /** Sets the player movement input. */
    public void setMovementInput(Vector2 value) { movementInput.set(value); }

    /**
     * Applies the force to the body
     * This method should be called after the force attribute is set.
     */
    public void applyInputForce(boolean onCurrent, Vector2 currentVelocity) {
        // Determine whether the give small buff to player
        boolean againstCurrent = false;
        if(onCurrent){
            float angle = Math.abs(movementInput.angleDeg() - currentVelocity.angleDeg());
//            System.out.println(angle);
            againstCurrent = (180 - CURRENT_BUFF_SCOPE) < angle && angle < (180 + CURRENT_BUFF_SCOPE);
        }
        if (super.isDestroyed()) return;
        if (movementInput.isZero()) {
            // Damp out player motion
            forceCache.set(physicsObject.getLinearVelocity()).scl(-DAMPING);
            physicsObject.getBody().applyForce(forceCache,getPosition(),true);
        } else {
            // Accelerate player based on input
            forceCache.set(movementInput).scl(THRUST);
            // Small buff going against currents
            if(againstCurrent) forceCache.add(movementInput.cpy().nor().scl(AC_MOVE_BUFF));
            else if(onCurrent) forceCache.add(movementInput.cpy().nor().scl(WC_MOVE_BUFF));
            physicsObject.getBody().applyLinearImpulse(forceCache,getPosition(),true);
        }
        // Velocity too high, clamp it
        float speedRatio = (againstCurrent ? MAX_SPEED_AGAINST_CURRENT : MAX_SPEED) / physicsObject.getLinearVelocity().len();
        if (speedRatio < 1) {
            physicsObject.setLinearVelocity(physicsObject.getLinearVelocity().scl(speedRatio));
        }
    }

    private Timer forceTime = new Timer();

    /** @param force the force applied to player over a period of time. */
    public void setProjectileForce(Vector2 force){
        externalForce.set(force);
        // Reset timer so the most recent forces takes precedence.
        forceTime.resetTimeStamp();
        forceTime.setTimeStamp();
    }

    /** */
    public void applyProjectileForce(){
        if(!forceTime.hasTimeElapsed(FORCE_DURATION, false)) {
            physicsObject.getBody().applyForce(externalForce, getPosition(), true);
        }
    }

    /** Getter and setters for health */
    public float getHealth() { return health; }
    /** Getter and setters for health */
    public float getHealthRatio() { return health / MAXIMUM_PLAYER_HEALTH; }
    /** @param newHealth the amount of health the player will have updated. */
    public void setHealth(float newHealth) {
        health = Math.max(0, newHealth);
    }
    /** @param dh amount to change player health by with collisons. */
    public void addHealth(float dh) {
        health = Math.min(health + dh, MAXIMUM_PLAYER_HEALTH);
    }
    /** Reduce player health based on distance traveled and movement cost. */
    public void applyMoveCost(float dt) {
        float L = physicsObject.getLinearVelocity().len();
        health -= MOVE_COST * L * dt; // base movement cost (no current)
    }
    /** @return whether the player health is below zero */
    public boolean isDead() { return health < 0f; }
    /** How far the raft could travel with its current health, in game units, assuming they don't use currents or drift */
    public float getPotentialDistance() {
        return health / MOVE_COST;
    }
    /** Reverse the firing animation and effect due to pre-mature release of mouse. */
    public void reverseFire() { addHealth(-Spear.DAMAGE); switchToIdle(); resetCanFire(); }
    /** called when fire is reversed or finished. reset two fcs and set state to idle */
    public void switchToIdle() { raftState = RaftState.IDLE; fc.resetAll(); }
    /** Set whether the player has reached the end of the animation cycle. */
    public void resetCanFire() { canFire = false; aurafc.resetAll(); }
    /** @return whether the player has reached the end of the firing animation cycle. */
    public boolean canFire() { return canFire; }
    /** @param charging whether to set the player to actively charging. Subtracts health to create the spear. */
    public void beginCharging(boolean charging) {
        if(!isCharging() && !canFire && charging) {
            addHealth(Spear.DAMAGE);
            raftState = RaftState.CHARGING;
            fc.resetAll();
        }
    }
    /** @return whether player is actively charging. */
    public boolean isCharging() { return raftState == RaftState.CHARGING; }

    // Which direction to player is facing.
    float flip;

    @Override
    public FrameCalculator getFrameCalculator(){ return fc; };

    @Override
    public void setAnimationFrame(float dt) {
        fc.addTime(dt);
        aurafc.addTime(dt);
        switch (raftState){
            case IDLE:
                fc.setFrame(IDLE_AS, IDLE_SF, IDLE_FC, false);
                if(canFire()) {aurafc.setFrame(AURA_AS, AURA_SF, AURA_FC, false);}
                break;
            case CHARGING:
                fc.setFrame(SHOOTING_AS, SHOOTING_SF, SHOOTING_FC, false);
                canFire = fc.isFrame(SHOOTING_SF, SHOOTING_FC, false);
                aurafc.setFrame(AURA_AS, AURA_SF, AURA_FC, false);
                if(canFire()) { switchToIdle(); } // charging process is finished
        }
        flip = setTextureXOrientation(false);
    }

    /** @return float representing the readiness of the weapon before firing. Guaranteed to be between 0 and 1. */
    public float getFiringStage(){
        if(canFire() || hasSpear()){ return 1; }
        else if(raftState == RaftState.IDLE) { return 0; }
        else{ return Math.max(0, Math.min(1, (float) fc.getFrame() / SHOOTING_FC)); }
    }

    /**
     * Set the texture for this raft and its aura.
     * @param raft texture
     * @param aura texture
     */
    public void setTexture(TextureRegion raft, TextureRegion aura){
        super.setTexture(raft);
        attackAura = new TextureHolder(aura);
        attackAura.setTextureScale(new Vector2(
                getWidth() * 2/ this.attackAura.getTexture().getRegionWidth(),
                getWidth() * 2/ this.attackAura.getTexture().getRegionHeight()));
        attackAura.setTextureOffset(new Vector2(
                HORIZONTAL_OFFSET,
                (attackAura.getTexture().getRegionHeight() * attackAura.getTextureScale().y - getHeight())/2f));
    }

    /** Realign the raft so thsat the bottom of it is at the bottom of the capsule object. */
    @Override
    public void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(HORIZONTAL_OFFSET,(texture.getRegionHeight()*textureScale.y - getHeight())/2f);
        // 0.25 offset because the texture is off-center horizontally
    }
    /**
     * Set the filmstrip frame before call the super draw method.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas){
        ((FilmStrip) texture).setFrame(fc.getFrame());
        super.draw(canvas);
        if(raftState == RaftState.CHARGING || canFire()){
            ((FilmStrip) attackAura.getTexture()).setFrame(aurafc.getFrame());
            attackAura.setColor(canFire() ? Color.YELLOW : Color.RED);
            super.draw(canvas, attackAura);
        }
    }

    public void draw(GameCanvas canvas, int ticks){
        ((FilmStrip) texture).setFrame(fc.getFrame());
        if (texture != null) {
            canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() + textureOffset.x,
                    getY() + textureOffset.y + BOB_AMP * (float) Math.sin((ticks % BOB_TIME)/BOB_TIME * 2 * Math.PI), getAngle(), textureScale.x, textureScale.y);
        }
        if(raftState == RaftState.CHARGING || canFire()){
            ((FilmStrip) attackAura.getTexture()).setFrame(aurafc.getFrame());
            attackAura.setColor(canFire() ? Color.YELLOW : Color.RED);
            super.draw(canvas, attackAura);
        }
    }

    /** @param s the newly created spear. */
    public void setSpear(Spear s){
        spear = s;
        floatTime = 0;
    }
    /** @return whether this raft has a spear or not. */
    public boolean hasSpear(){
        return spear != null && !spear.isDestroyed();
    }
    /** The amount of time that has elapsed for floating */
    float floatTime;
    /**
     * Change the spear location based on the raft and mouse location
     * @param dt the time elapsed.
     * @param dir the mouse location
     */
    public void updateSpear(float dt, Vector2 dir){
        if(!hasSpear()) return;
        floatTime += dt;
        spear.setFloatPosition(getPosition(), floatTime, flip, dir);
    }
    /** The Spear the raft owns */
    public Spear getSpear(){ return spear; }
}
