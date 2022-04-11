package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;


/**
 * Model class for the player raft.
 */
public class Raft extends CapsuleObstacle {

    // CONSTANTS
    /** Movement cost for a unit distance **/
    private static final float MOVE_COST = 1.5f;
    /** Movement cost multiplier for moving with the current */
    private static final float WITH_CURRENT = 0.45f;
    /** Movement cost increase for moving with the current */
    private static final float AGAINST_CURRENT = 1.75f;

    // ATTRIBUTES
    /** The player's movement input */
    private Vector2 movementInput = new Vector2(0f,0f);

    /** The health of the raft. This must be >=0. */
    private float health;
    /** Maximum player health */
    public static final float MAXIMUM_PLAYER_HEALTH = 120.0f;
    /** Initial player health */
    public static final float INITIAL_PLAYER_HEALTH = 40.0f;
    /** The star of the level. This must be >=0. */
    private int star;
    /** Initial player health */
    private static final int INITIAL_PLAYER_STAR = 0;

    /** Whether the raft is actively firing */
    private boolean fire;
    /** The amount to slow the character down, while they aren't moving */
    private final float damping = 20f;
    /** The amount to accelerate the character */
    private final float thrust = 10f;
    /** The maximum character speed allowed */
    private final float maxSpeed = 20f;
    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();

    // METHODS
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

    /** Returns the player movement input. */
    protected Vector2 getMovementInput() { return movementInput; }

    /** Sets the player movement input. */
    public void setMovementInput(Vector2 value) { movementInput.set(value); }

    /** @return whether the player is actively firing */
    public boolean isFire() { return fire; }

    /** @param fire whether to set the player to actively firing */
    public void setFire(boolean fire) { this.fire = fire; }

    /**
     * Applies the force to the body of this dude
     *
     * This method should be called after the force attribute is set.
     */
    public void applyInputForce() {
        if (super.isDestroyed()) {
            return;
        }

        if (movementInput.isZero()) {
            // Damp out player motion
            forceCache.set(getLinearVelocity()).scl(-damping);
            body.applyForce(forceCache,getPosition(),true);
        } else {
            // Accelerate player based on input
            forceCache.set(movementInput).scl(thrust);
            body.applyLinearImpulse(forceCache,getPosition(),true);
        }

        // Velocity too high, clamp it
        float speedRatio = maxSpeed / getLinearVelocity().len();
        if (speedRatio < 1) {
            setLinearVelocity(getLinearVelocity().scl(speedRatio));
        }
    }

    /** Getter and setters for health */
    public float getHealth() { return health; }

    /** Getter and setters for health */
    public float getHealthRatio() { return health / MAXIMUM_PLAYER_HEALTH; }

    public void setHealth(float newHealth) {
        health = Math.max(0, newHealth);
    }

    public void addHealth(float wood) {
        health = Math.min(health + wood, MAXIMUM_PLAYER_HEALTH);
    }

    /** Constructor for Raft object
     * @param position: position of raft
     */
    public Raft(Vector2 position) {
        super(2.8f, 1.3f);
//        setRadius(1.4f);
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        this.health = INITIAL_PLAYER_HEALTH;
        this.star = INITIAL_PLAYER_STAR;
        fixture.filter.categoryBits = CATEGORY_PLAYER;
        fixture.filter.maskBits = MASK_PLAYER;
    }

    /** Add one star to the player star count */
    protected void addStar() { star++; }

    /** Get the star of the level */
    protected int getStar() { return star; }

    /** Reduce player health based on distance traveled and movement cost. */
    public void applyMoveCost(float dt) {
        if (!movementInput.isZero()) {
            float L = getLinearVelocity().len();
            if (L > 0.15) { // L < 0.15 could be from moving into a wall, so we ignore it
                float cost = MOVE_COST * L * dt; // base movement cost (no current)
                if (!waterVelocity.isZero()) { // reduced cost if moving with the current, increased if moving against the current
                    // TODO: Demian: I wrote this code, but looking at it again, I should probably rewrite it so it doesn't use all this math.
                    float c = (float)Math.cos(getLinearVelocity().angleRad(waterVelocity));// c = +1 with current, -1 against current
                    float b = 1 + (WITH_CURRENT-AGAINST_CURRENT)*0.5f*c + ((WITH_CURRENT+AGAINST_CURRENT)*0.5f-1)*c*c;// interpolate between modifiers based on c
                    cost *= b;
                }
                health -= cost;
            }
        }
    }

    /** @return whether the player health is below zero */
    public boolean isDead() { return health < 0f; }

    /** How far the raft could travel with its current health, in game units, assuming they don't use currents or drift */
    public float getPotentialDistance() {
        return health / MOVE_COST;
    }

    @Override
    public void setTexture(Texture texture) {
        super.setTexture(texture);
        float w = getWidth()*drawScale.x / texture.getWidth();
        textureScale.set(w, w);
        origin.set(texture.getWidth()/2.0f, texture.getWidth()/2.0f * getHeight()/getWidth());
    }

    @Override
    public float getCrossSectionalArea() {
        return 1.5f*super.getCrossSectionalArea();
    }
}
