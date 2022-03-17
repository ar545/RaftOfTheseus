package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.util.FilmStrip;

/**
 * Base class for all Model objects in the game.
 */
public abstract class GameObject {


    // added enum and some attributes from the Environment class in the GameplayPrototype
    // since it wasn't in our architecture diagram
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

    /** Object position (centered on the texture middle) */
    protected Vector2 position;
    /** Reference to texture origin */
    protected Vector2 origin;
    /** Radius of the object (used for collisions and display) */
    protected float radius;
    /** Rotation of object (used for displaying, in degrees clockwise) -*/
    protected float rotation = 0.0f;
    /** CURRENT image for this object. May change over time. */
    protected FilmStrip animator;

    // ACCESSORS
    public void setTexture(Texture texture) {
        animator = new FilmStrip(texture,1,1,1);
        origin = new Vector2(animator.getRegionWidth()/2.0f, animator.getRegionHeight()/2.0f);
    }

    // ATTRIBUTES
    /** Object velocity vector */
    protected Vector2 velocity;
    /** Whether or not the object should be removed at next timestep. */
    protected boolean destroyed; // keep?

    /// FIELDS: Initialization structures to store body information
    /** Stores the body information for this shape */
    protected BodyDef bodyinfo; // keep?

    /// Track garbage collection status
    /** Whether the object should be removed from the world on next pass */
    private boolean toRemove; // keep?

    // ABSTRACT METHODDS

    /**
     * Returns the type of this object.
     *
     * We use this instead of runtime-typing for performance reasons.
     *
     * @return the type of this object.
     */
    public abstract ObjectType getType();

    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     *
     * This method is called AFTER the collision resolution state. Therefore, it
     * should not be used to process actions or any other gameplay information.  Its
     * primary purpose is to adjust changes to the fixture, which have to take place
     * after collision.
     *
     * @param dt Timing values from parent loop
     */
    public abstract void update(float dt);

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     *
     * Implementations of this method should NOT retain a reference to World.
     * That is a tight coupling that we should avoid.
     *
     * @param world Box2D world to store body
     *
     * @return true if object allocation succeeded
     */
    public abstract boolean activatePhysics(World world);

    /**
     * Destroys the physics Body(s) of this object if applicable,
     * removing them from the world.
     *
     * @param world Box2D world that stores body
     */
    public abstract void deactivatePhysics(World world);

    // NON-ABSTRACT METHODS
    public float getX() {
        return bodyinfo.position.x;
    }

    public float getY() {
        return bodyinfo.position.y;
    }

    public boolean isRemoved() {
        return toRemove;
    }

    public Object getName() {
        return null;
    } // change?

    public void markRemoved(boolean b) {
        toRemove = true;
    }

    /**
     * Returns true if this object is destroyed.
     *
     * Objects are not removed immediately when destroyed.  They are garbage collected
     * at the end of the frame.  This tells us whether the object should be garbage
     * collected at the frame end.
     *
     * @return true if this object is destroyed
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Sets whether this object is destroyed.
     *
     * Objects are not removed immediately when destroyed.  They are garbage collected
     * at the end of the frame.  This tells us whether the object should be garbage
     * collected at the frame end.
     *
     * @param value whether this object is destroyed
     */
    public void setDestroyed(boolean value) {
        destroyed = value;
    }

    /**
     * Constructs a trivial game object
     *
     * The created object has no position or size.  These should be set by the subclasses.
     */
    public GameObject() {
        destroyed = false;
    }

    /**
     * Draws the texture physics object.
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        canvas.draw(animator, Color.WHITE, origin.x, origin.y,
                position.x, position.y, 0.0f, 1.0f, 1.f);
    }

    /**
     * Draws this object to the canvas
     *
     * There is only one drawing pass in this application, so you can draw the objects
     * in any order.
     *
     * @param canvas The drawing context
     */
    public void drawAffine(GameCanvas canvas, Vector2 affine) {
        float s = 2.0f*radius / animator.getRegionHeight();
        canvas.draw(animator, Color.WHITE, origin.x, origin.y,
                position.x + affine.x, position.y + affine.y, rotation, s, s);
    }
}
