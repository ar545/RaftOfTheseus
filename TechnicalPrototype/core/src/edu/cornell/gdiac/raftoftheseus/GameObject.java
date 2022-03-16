package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;

public abstract class GameObject {

    /// FIELDS: Initialization structures to store body information
    /** Stores the body information for this shape */
    protected BodyDef bodyinfo;

    /// Track garbage collection status
    /** Whether the object should be removed from the world on next pass */
    private boolean toRemove;

    /// Abstract Methods
    /**
     * Draws the texture physics object.
     * @param canvas Drawing context
     */
    public abstract void draw(GameCanvas canvas);

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
    }

    public void markRemoved(boolean b) {
        toRemove = true;
    }
}
