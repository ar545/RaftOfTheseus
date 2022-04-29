package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

import java.util.HashSet;


public class OldShark extends GameObject {

//    private Random rand = new Random();

    public OldShark(Vector2 position, Raft targetRaft, LevelModel level) {
        physicsObject = new WheelObstacle(1.45f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY;
        physicsObject.getFilterData().maskBits = MASK_ENEMY;

        this.targetRaft = targetRaft;
        this.level = level;

        health = 2;
    }

    public ObjectType getType() {
        return ObjectType.SHARK;
    }

    public static void setConstants(JsonValue objParams){
        ENEMY_WANDER_SPEED = objParams.getFloat("wander speed");
        ENEMY_CHASE_SPEED = objParams.getFloat("chase speed");
        ENEMY_DAMAGE = objParams.getFloat("damage");
        ENEMY_ENRAGE_CHASE_SPEED = objParams.getFloat("enrage speed", 8f);
        PROTECT_RANGE = objParams.getInt("protect range", 5);
        MAX_DEPTH = objParams.getInt("search range", 30);
    }

    /**
     * How much damage an enemy deals to the player upon collision, per animation frame
     */
    public static final float DAMAGE_PER_FRAME = 0.5f;
    /**
     * How fast enemy wanders around w/o target
     **/
    public static float ENEMY_WANDER_SPEED = 2.5f;
    /**
     * How fast the enemy moves towards its target, in units per second
     */
    public static float ENEMY_CHASE_SPEED = 4.0f;
    /**
     * How fast the enemy moves towards its target while enraged , in units per second
     */
    public static float ENEMY_ENRAGE_CHASE_SPEED = 8.0f;
    /**
     * How much health will enemy take from player upon collision
     */
    public static float ENEMY_DAMAGE;

    /** how far will the enemy go from a nearby treasure in tiles **/
    private static int PROTECT_RANGE = 5;

    /** max depth for djikstra pathfind **/
    public static int MAX_DEPTH = 30;

    public Vector2 moveVector = new Vector2();

    public static enum enemyState {
        /**
         * The enemy just spawned
         */
        SPAWN,
        /**
         * The enemy is patrolling around without a target
         */
        WANDER,
        /**
         * The enemy has a target, but must get closer
         */
        CHASE,
        /**
         * Like chase, but increased range and moves at greater speed
         */
        ENRAGE,
    }

    /**
     * This is the player, if this enemy is targeting the player.
     */
    private Raft targetRaft;

    private Treasure closestTreasure;

    /** if the enemy is enraged **/
    private boolean enraged;

    /** LevelModel used to convert screen coords **/
    private LevelModel level;

    private HashSet<String> visited;

    /** How much health this shark has remaining. */
    private int health;

    public void takeDamage() {
        health -= 1;
        if (health <= 0)
            setDestroyed(true);
        setEnraged(false);
    }


    public boolean isEnraged(){
        return enraged;
    }

    public void setEnraged(boolean b){
        this.enraged = b;
    }

    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }

    //    // TODO: this will change depending on implementation of AIController
    public void update(float dt) {
        super.update(dt);
        if (moveVector != null && targetRaft != null) {
            physicsObject.getBody().applyLinearImpulse(moveVector, getPosition(), true);
        }
    }
    /**
     * Sets moveVector so that applying it as a linear impulse brings this object's velocity closer to
     * moveVector*topSpeed.
     * Precondition: moveVector.len() == 1.
     * @param topSpeed Won't apply an impulse that takes us above this speed
     * @param smoothing Impulse is scaled by (1-smoothing). Higher smoothing means wider turns, slower responses.
     */
    public void calculateImpulse(float topSpeed, float smoothing) {
        float currentSpeed = physicsObject.getBody().getLinearVelocity().dot(moveVector); // current speed in that direction
        float impulseMagnitude = (topSpeed - currentSpeed)*physicsObject.getBody().getMass()*(1-smoothing);
        moveVector.scl(impulseMagnitude);
    }

    /* DISPLAY AND ANIMATION */

    // ANIMATION
    private static float HORIZONTAL_OFFSET = 0.0f;
    /** How much to enlarge the shark. */
    private static float TEXTURE_SCALE = 1.50f;
    /** The animation speed for the shark. */
    private static float IDLE_AS = 0.05f;
    private static float ATTACK_AS = 0.05f;
    /** The number of frames for this animation. */
    private static int IDLE_F = 9;
    private static int ATTACK_F = 7;
    /** Which frame to start on the filmstrip with this animation. */
    private static int IDLE_SF = 8;
    private static int ATTACK_SF = 0;

    // The value to increment once the animation time has passed. Used to calculate which frame should be used.
    private int frameCount = 0;
    // The amount of time elapsed, used for checking whether to increment frameCount.
    private float timeElapsed = 0;
    // Which frame should be set for drawing this game cycle.
    private int frame = IDLE_SF;
    // whether the shark was enraged in the last animation frame
    private boolean wasEnraged = false;

    /**
     * Realign the shark sprite so that the bottom of it is at the bottom of the physics object.
     */
    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(HORIZONTAL_OFFSET,(texture.getRegionHeight()*textureScale.y - getHeight())/2f - 1.0f);
    }

    /**
     * Set the filmstrip frame before call the super draw method.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas, Color color){
        ((FilmStrip) texture).setFrame(frame);
        super.draw(canvas, color);
    }

    /**
     * Method to set animation based on the time elapsed in the game.
     * @param dt the current time in the game.
     */
    public void setAnimationFrame(float dt) {
        timeElapsed += dt;
        if (isEnraged() != wasEnraged) {
            // animation changed, set frame to 0
            frameCount = 0;
            timeElapsed = 0;
            wasEnraged = isEnraged();
        }
        // update animation frame
        if(!isEnraged()) { // not attacking
            setFrame(IDLE_AS, IDLE_F, IDLE_SF, false);
        } else {
            setFrame(ATTACK_AS, ATTACK_F, ATTACK_SF, false);
        }
        // flip texture based on movement
        float flip = getLinearVelocity().x > 0 ? -1 : 1;
        textureScale.x = flip * Math.abs(textureScale.x);
    }

    /**
     * Sets the frame of the animation based on the FSM and time given.
     * @param animationSpeed how many seconds should pass between each frame.
     * @param frames the number of frames this animation has.
     * @param start which frame in the FilmStrip the animation starts on.
     * @param reverse whether the animation should be drawn backwards.
     * @return whether it has reached the last animation image.
     */
    private void setFrame(float animationSpeed, int frames, int start, boolean reverse){
        if (timeElapsed > animationSpeed){
            timeElapsed = 0;
            frameCount += 1;
            frame = start + (reverse ? (frames - 1) - frameCount % frames : frameCount % frames);
        }
    }

    /**
     * Checks whether the current frame is the starting or ending frame.
     * @param frames the amount of frames for the given animation.
     * @param start the starting index.
     * @param begin whether to check for the starting or ending index.
     * @return whether the current frame is the start or end frame.
     */
//    private boolean isFrame(int frames, int start, boolean begin){
//        return begin ? frame == start : frame == frames - 1 + start;
//    }

}