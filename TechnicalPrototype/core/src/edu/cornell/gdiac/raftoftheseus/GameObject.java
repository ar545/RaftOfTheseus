package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
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


    /** Reference to texture origin */
    protected Vector2 origin;
    /** Radius of the object in pixels (used for display only) */
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
//    /** Object position (centered on the texture middle) */
//    protected Vector2 position;
//    /** Object velocity vector */
//    protected Vector2 velocity;

    /// FIELDS: Initialization structures to store body information
    /** Stores the body information for this shape */
    protected BodyDef bodyinfo;
    /** Stores the body for this shape */
    protected Body body;
    /** A cache value for when the user wants to access the body position */
    protected Vector2 positionCache = new Vector2();
    /** A cache value for when the user wants to access the linear velocity */
    protected Vector2 velocityCache = new Vector2();

    /// Track garbage collection status
    /** Whether the object should be removed from the world (object list) at next timestamp. */
    protected boolean destroyed;

    // ABSTRACT METHODS

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
        // code copied from Physics lab Obstacle constructor

        // Object has yet to be deactivated
        destroyed = false;

        // Allocate the body information
        bodyinfo = new BodyDef();
        bodyinfo.awake  = true;
        bodyinfo.allowSleep = true;
        bodyinfo.gravityScale = 1.0f;
//        bodyinfo.position.set(x,y);
        bodyinfo.fixedRotation = false;
        // Objects are physics objects unless otherwise noted
        bodyinfo.type = BodyDef.BodyType.DynamicBody;
    }

    /**
     * Draws the texture physics object.
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        canvas.draw(animator, Color.WHITE, origin.x, origin.y,
                getX(), getY(), 0.0f, 1.0f, 1.f);
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
                100.0f/3.0f*getX() + affine.x, 100.0f/3.0f*getY() + affine.y, rotation, s, s);
    }

    /// BodyDef Methods
    /**
     * Returns the body type for Box2D physics
     *
     * If you want to lock a body in place (e.g. a platform) set this value to STATIC.
     * KINEMATIC allows the object to move (and some limited collisions), but ignores
     * external forces (e.g. gravity). DYNAMIC makes this is a full-blown physics object.
     *
     * @return the body type for Box2D physics
     */
    public BodyDef.BodyType getBodyType() {
        return (body != null ? body.getType() : bodyinfo.type);
    }

    /**
     * Returns the body type for Box2D physics
     *
     * If you want to lock a body in place (e.g. a platform) set this value to STATIC.
     * KINEMATIC allows the object to move (and some limited collisions), but ignores
     * external forces (e.g. gravity). DYNAMIC makes this is a full-blown physics object.
     *
     * @return the body type for Box2D physics
     */
    public void setBodyType(BodyDef.BodyType value) {
        if (body != null) {
            body.setType(value);
        } else {
            bodyinfo.type = value;
        }
    }

    /**
     * Returns the current position for this physics body
     *
     * This method does NOT return a reference to the position vector. Changes to this
     * vector will not affect the body.  However, it returns the same vector each time
     * its is called, and so cannot be used as an allocator.
     *
     * @return the current position for this physics body
     */
    public Vector2 getPosition() {
        return (body != null ? body.getPosition() : positionCache.set(bodyinfo.position));
    }

    /**
     * Sets the current position for this physics body
     *
     * This method does not keep a reference to the parameter.
     *
     * @param value  the current position for this physics body
     */
    public void setPosition(Vector2 value) {
        if (body != null) {
            body.setTransform(value,body.getAngle());
        } else {
            bodyinfo.position.set(value);
        }
    }

    /**
     * Sets the current position for this physics body
     *
     * @param x  the x-coordinate for this physics body
     * @param y  the y-coordinate for this physics body
     */
    public void setPosition(float x, float y) {
        if (body != null) {
            positionCache.set(x,y);
            body.setTransform(positionCache,body.getAngle());
        } else {
            bodyinfo.position.set(x, y);
        }
    }

    /**
     * Returns the x-coordinate for this physics body
     *
     * @return the x-coordinate for this physics body
     */
    public float getX() {
        return (body != null ? body.getPosition().x : bodyinfo.position.x);
    }

    /**
     * Sets the x-coordinate for this physics body
     *
     * @param value  the x-coordinate for this physics body
     */
    public void setX(float value) {
        if (body != null) {
            positionCache.set(value,body.getPosition().y);
            body.setTransform(positionCache,body.getAngle());
        } else {
            bodyinfo.position.x = value;
        }
    }

    /**
     * Returns the y-coordinate for this physics body
     *
     * @return the y-coordinate for this physics body
     */
    public float getY() {
        return (body != null ? body.getPosition().y : bodyinfo.position.y);
    }

    /**
     * Sets the y-coordinate for this physics body
     *
     * @param value  the y-coordinate for this physics body
     */
    public void setY(float value) {
        if (body != null) {
            positionCache.set(body.getPosition().x,value);
            body.setTransform(positionCache,body.getAngle());
        } else {
            bodyinfo.position.y = value;
        }
    }

    /**
     * Returns the angle of rotation for this body (about the center).
     *
     * The value returned is in radians
     *
     * @return the angle of rotation for this body
     */
    public float getAngle() {
        return (body != null ? body.getAngle() : bodyinfo.angle);
    }

    /**
     * Sets the angle of rotation for this body (about the center).
     *
     * @param value  the angle of rotation for this body (in radians)
     */
    public void setAngle(float value) {
        if (body != null) {
            body.setTransform(body.getPosition(),value);
        } else {
            bodyinfo.angle = value;
        }
    }

    /**
     * Returns the linear velocity for this physics body
     *
     * This method does NOT return a reference to the velocity vector. Changes to this
     * vector will not affect the body.  However, it returns the same vector each time
     * its is called, and so cannot be used as an allocator.
     *
     * @return the linear velocity for this physics body
     */
    public Vector2 getLinearVelocity() {
        return (body != null ? body.getLinearVelocity() : velocityCache.set(bodyinfo.linearVelocity));
    }

    /**
     * Sets the linear velocity for this physics body
     *
     * This method does not keep a reference to the parameter.
     *
     * @param value  the linear velocity for this physics body
     */
    public void setLinearVelocity(Vector2 value) {
        if (body != null) {
            body.setLinearVelocity(value);
        } else {
            bodyinfo.linearVelocity.set(value);
        }
    }

    /**
     * Returns the x-velocity for this physics body
     *
     * @return the x-velocity for this physics body
     */
    public float getVX() {
        return (body != null ? body.getLinearVelocity().x : bodyinfo.linearVelocity.x);
    }

    /**
     * Sets the x-velocity for this physics body
     *
     * @param value  the x-velocity for this physics body
     */
    public void setVX(float value) {
        if (body != null) {
            velocityCache.set(value,body.getLinearVelocity().y);
            body.setLinearVelocity(velocityCache);
        } else {
            bodyinfo.linearVelocity.x = value;
        }
    }

    /**
     * Returns the y-velocity for this physics body
     *
     * @return the y-velocity for this physics body
     */
    public float getVY() {
        return (body != null ? body.getLinearVelocity().y : bodyinfo.linearVelocity.y);
    }

    /**
     * Sets the y-velocity for this physics body
     *
     * @param value  the y-velocity for this physics body
     */
    public void setVY(float value) {
        if (body != null) {
            velocityCache.set(body.getLinearVelocity().x,value);
            body.setLinearVelocity(velocityCache);
        } else {
            bodyinfo.linearVelocity.y = value;
        }
    }

    /**
     * Returns the angular velocity for this physics body
     *
     * The rate of change is measured in radians per step
     *
     * @return the angular velocity for this physics body
     */
    public float getAngularVelocity() {
        return bodyinfo.angularVelocity;
    }

    /**
     * Sets the angular velocity for this physics body
     *
     * @param value the angular velocity for this physics body (in radians)
     */
    public void setAngularVelocity(float value) {
        bodyinfo.angularVelocity = value;
    }

    /** @return the most recent aka cached position */
    public Vector2 getPositionCache() {
        return positionCache;
    }

}
