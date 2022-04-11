package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.raftoftheseus.obstacle.SimpleObstacle;

/**
 * Base class for all Model objects in the game.
 */
public abstract class GameObject extends SimpleObstacle {
    /**
     * Enum specifying the type of this game object.
     */
    public enum ObjectType {
        /**
         * A ship, which lives until it is destroyed by a shell
         */
        RAFT,
        /**
         * A piece of driftwood
         */
        WOOD,
        /**
         * The obstacle that player cannot overcome (e.g. a rock)
         */
        OBSTACLE,
        /**
         * The current that player will suffer or benefit from
         */
        CURRENT,
        /**
         * The enemy
         */
        ENEMY,
        /**
         * The goal tile
         */
        GOAL,
        /**
         * A treasure collectible
         */
        TREASURE,
        /**
         * A bullet shot by the player
         */
        BULLET,
        HYDRA
    }

    /** Collision filtering categories */
    protected final static short CATEGORY_PLAYER = 1<<1;
    protected final static short CATEGORY_ENEMY = 1<<2;
    protected final static short CATEGORY_PLAYER_BULLET = 1<<3;
    protected final static short CATEGORY_ENEMY_BULLET = 1<<4;
    protected final static short CATEGORY_CURRENT = 1<<5;
    protected final static short CATEGORY_TERRAIN = 1<<6;
    protected final static short CATEGORY_PUSHABLE = 1<<7;
    protected final static short CATEGORY_NON_PUSHABLE = 1<<8;
    /** Collision filtering masks */
    protected final static short MASK_PLAYER = CATEGORY_ENEMY | CATEGORY_ENEMY_BULLET | CATEGORY_CURRENT
            | CATEGORY_TERRAIN | CATEGORY_PUSHABLE | CATEGORY_NON_PUSHABLE;
    protected final static short MASK_PLAYER_BULLET = CATEGORY_ENEMY | CATEGORY_TERRAIN;
    protected final static short MASK_ENEMY = CATEGORY_PLAYER | CATEGORY_PLAYER_BULLET | CATEGORY_CURRENT
            | CATEGORY_TERRAIN | CATEGORY_PUSHABLE;
    protected final static short MASK_ENEMY_BULLET = CATEGORY_PLAYER | CATEGORY_TERRAIN;
    protected final static short MASK_CURRENT = CATEGORY_PLAYER | CATEGORY_ENEMY | CATEGORY_PUSHABLE;
    protected final static short MASK_TERRAIN = CATEGORY_PLAYER | CATEGORY_ENEMY | CATEGORY_PLAYER_BULLET
            | CATEGORY_ENEMY_BULLET | CATEGORY_PUSHABLE;
    protected final static short MASK_WOOD = CATEGORY_PLAYER | CATEGORY_ENEMY | CATEGORY_CURRENT | CATEGORY_TERRAIN;
    protected final static short MASK_TREASURE = CATEGORY_PLAYER;// treasure isn't pushed around by anything
    protected final static short MASK_GOAL = CATEGORY_PLAYER;

    /**
     * How much to scale the texture before displaying (screen pixels / texture pixels)
     */
    public Vector2 textureScale;

    /** Combined force vectors of all currents affecting this object */
    private Vector2 currentsCache = new Vector2();
//    /** The speed at which a current flows, in units per second */
//    private final float waterSpeed = 15.0f;
    /** The average velocity of water flowing near this object */
    protected Vector2 waterVelocity = new Vector2();
    /** Modifier on force applied by current */
    protected final float dragCoefficient = 0.5f;
    /** cache vector for calculation */
    protected Vector2 dragCache = new Vector2(0,0);

    public void enterCurrent(Vector2 f) {
        currentsCache.add(f);
        waterVelocity.set(currentsCache); // cancelled normalization (.nor().scl(waterSpeed)) due to speed variant
    }

    public void exitCurrent(Vector2 f) {
        currentsCache.sub(f);
        if (currentsCache.isZero(0.1f)) {
            currentsCache.setZero();
            waterVelocity.setZero();
        } else
            waterVelocity.set(currentsCache); // cancelled normalization (.nor().scl(waterSpeed)) due to speed variant
    }

    // ABSTRACT METHODS

    /**
     * Returns the type of this object. We use this instead of runtime-typing for performance reasons.
     *
     * @return the type of this object.
     */
    public abstract ObjectType getType();

    // NON-ABSTRACT METHODS

    /**
     * Returns true if this object is destroyed.
     */
    public boolean isDestroyed() {
        return isRemoved();
    }

    /**
     * Sets whether this object is destroyed.
     *
     * @param value whether this object is destroyed
     */
    public void setDestroyed(boolean value) {
        markRemoved(value);
    }

    /**
     * Constructs a trivial game object
     */
    public GameObject(float x, float y) {
        super(x, y);

        setDensity(1.0f);
        setFriction(0.0f);
        setRestitution(0.0f);
        setLinearDamping(0.0f);
        setFixedRotation(true);
        super.setDrawScale(1,1);
    }


    /**
     * Draws the texture physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y,
                    getAngle(), textureScale.x, textureScale.y);
        }
    }


    /**
     * @return the most recent aka cached position
     */
    public Vector2 getPositionCache() {
        return positionCache;
    }

    public void applyDrag() {
        dragCache.set(waterVelocity).sub(getLinearVelocity());
        dragCache.scl(dragCache.len() * dragCoefficient * getCrossSectionalArea());
        body.applyForce(dragCache, getPosition(), true);
    }

    /** The cross-sectional area of this object which is underwater. Used for drag calculation. */
    public float getCrossSectionalArea() {
        return 0;
    }
}
