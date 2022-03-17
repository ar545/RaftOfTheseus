package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Treasure extends GameObject {

    // ATTRIBUTES
    /** Has the treasure been collected yet? */
    protected boolean collected;
    /** Treasure's position. */
    public Vector2 position;

    public Treasure(Vector2 position) {
        super();
        radius = 50;
        this.position = position;
        collected = false;
    }

    public ObjectType getType() {
        return ObjectType.TREASURE;
    }

    public boolean getCollected() { return collected; }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public void update(float dt) {
        if(collected) {
            // TODO: update player score
            this.setDestroyed(true);
        }
    }

    // need to be implemented?
    public boolean activatePhysics(World world) {
        return false;
    }

    // need to be implemented?
    public void deactivatePhysics(World world) {

    }
}
