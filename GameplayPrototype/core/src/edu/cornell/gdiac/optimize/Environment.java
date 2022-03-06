package edu.cornell.gdiac.optimize;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.util.FilmStrip;

public abstract class Environment {

    /**
     * Enum specifying the type of this game object.
     */
    public enum ObjectType {
        /** A ship, which lives until it is destroyed by a shell */
        SHIP,
        /** A piece of driftwood */
        WOOD,
        /** The target, or if you like, The Walker White */
        TARGET,
        /** The obstacle that player cannot overcome */
        OBSTACLE,
        /** The current that player will suffer or benefit from */
        CURRENT,
        /** The enemy */
        ENEMY
    }

    /** Object position (centered on the texture middle) */
    protected Vector2 position;
    /** Reference to texture origin */
    protected Vector2 origin;
    /** Radius of the object (used for collisions and display) */
    protected float radius;
    /** Rotation of object (used for displaying, in degrees clockwise) */
    protected float rotation = 0.0f;
    /** CURRENT image for this object. May change over time. */
    protected FilmStrip animator;

    // ACCESSORS
    public void setTexture(Texture texture) {
        animator = new FilmStrip(texture,1,1,1);
        origin = new Vector2(animator.getRegionWidth()/2.0f, animator.getRegionHeight()/2.0f);
    }

    public Texture getTexture() {
        return animator == null ? null : animator.getTexture();
    }

    /**
     * Returns the position of this object (e.g. location of the center pixel)
     *
     * The value returned is a reference to the position vector, which may be
     * modified freely.
     *
     * @return the position of this object
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Returns the x-coordinate of the object position (center).
     *
     * @return the x-coordinate of the object position
     */
    public float getX() {
        return position.x;
    }

    /**
     * Sets the x-coordinate of the object position (center).
     *
     * @param value the x-coordinate of the object position
     */
    public void setX(float value) {
        position.x = value;
    }

    /**
     * Returns the y-coordinate of the object position (center).
     *
     * @return the y-coordinate of the object position
     */
    public float getY() {
        return position.y;
    }

    /**
     * Sets the y-coordinate of the object position (center).
     *
     * @param value the y-coordinate of the object position
     */
    public void setY(float value) {
        position.y = value;
    }

    /**
     * Returns the radius of this object.
     *
     * All of our objects are circles, to make collision detection easy.
     *
     * @return the radius of this object.
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Constructs a trivial environment object
     *
     * The created object has no position or size.  These should be set by the subclasses.
     */
    public Environment() {
        position = new Vector2(0.0f, 0.0f);
        radius = 0.0f;
    }

    /**
     * Draws this object to the canvas
     *
     * There is only one drawing pass in this application, so you can draw the objects
     * in any order.
     *
     * @param canvas The drawing context
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

    /**
     * Returns the type of this object.
     *
     * We use this instead of runtime-typing for performance reasons.
     *
     * @return the type of this object.
     */
    public abstract ObjectType getType();
}
