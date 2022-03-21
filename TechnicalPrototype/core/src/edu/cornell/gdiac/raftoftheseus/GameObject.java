package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.raftoftheseus.obstacle.PolygonObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.SimpleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

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
        BULLET
    }

    /**
     * How much to scale the texture before displaying (screen pixels / texture pixels)
     */
    public Vector2 textureScale;

    /**
     * combined force vectors of all currents affecting this object
     */
    private Vector2 currentsCache = new Vector2();
    /**
     * actual force applied by all currents (normalized and scaled)
     */
    private Vector2 currentsForce = new Vector2();
    /**
     * magnitude of force applied by current
     */
    private final float currentsMagnitude = 20.0f;

    public void enterCurrent(Vector2 f) {
        currentsCache.add(f);
        currentsForce.set(currentsCache).nor().scl(currentsMagnitude);
    }

    public void exitCurrent(Vector2 f) {
        currentsCache.sub(f);
        if (currentsCache.isZero(0.01f)) {
            currentsCache.setZero();
            currentsForce.setZero();
        } else
            currentsForce.set(currentsCache).nor().scl(currentsMagnitude);
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
        setFriction(0.1f);
        setRestitution(0.1f);
        setLinearDamping(0.1f);
        setFixedRotation(true);
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

    public void drawMap(GameCanvas canvas) {
        if (texture != null) {
            if (getType() != ObjectType.OBSTACLE &&  getType() != ObjectType.TREASURE && getType() != ObjectType.ENEMY
                    && getType() != ObjectType.WOOD) {
                canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x * 0.5f + canvas.getWidth() / 4,
                        getY() * drawScale.y * 0.5f + canvas.getHeight() / 4, getAngle(), textureScale.x * 0.5f, textureScale.y * 0.5f);
            }
        }
    }

    /**
     * @return the most recent aka cached position
     */
    public Vector2 getPositionCache() {
        return positionCache;
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        body.applyForce(currentsForce, getPosition(), true);
    }

//    /** @return the most recent aka cached position */
//    public Vector2 getPositionCache() {
//        return positionCache;
//    }
}
