package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

/**
 * Base class for all Model objects in the game.
 */
public abstract class GameObject extends WheelObstacle {
    /**
     * Enum specifying the type of this game object.
     */
    public enum ObjectType {
        /** A ship, which lives until it is destroyed by a shell */
        RAFT,
        /** A piece of driftwood */
        WOOD,
        /** The obstacle that player cannot overcome (e.g. a rock)*/
        OBSTACLE,
        /** The current that player will suffer or benefit from */
        CURRENT,
        /** The enemy */
        ENEMY,
        /** The goal tile */
        GOAL,
        /** A treasure collectible */
        TREASURE,
        /** A bullet shot by the player */
        BULLET
    }

    /** How much to scale the texture before displaying (screen pixels / texture pixels) */
    private Vector2 textureScale;

    // ACCESSORS
    public void setTexture(Texture texture) {
        super.setTexture(new TextureRegion(texture));
        textureScale = new Vector2(2.0f*getRadius()*drawScale.x / texture.getWidth(),
                2.0f*getRadius()*drawScale.x / texture.getHeight());
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
    public GameObject() {
        super(1.49f);
        setDrawScale(100.0f/3.0f, 100.0f/3.0f);
        setDensity(1.0f);
        setFriction(0.1f);
        setRestitution(0.1f);
        setFixedRotation(true);
    }

    /**
     * Draws the texture physics object.
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.x,getAngle(),textureScale.x,textureScale.y);
        }
    }

    /** @return the most recent aka cached position */
    public Vector2 getPositionCache() {
        return positionCache;
    }
}
