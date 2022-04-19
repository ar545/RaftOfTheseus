package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;

public class Rock extends GameObject {

    /**
     * Load the constants for the rock
     * @param objParams is the "rock" child of object_settings.json
     */
    public static void setConstants(JsonValue objParams){
        WIDTH = objParams.getFloat(0);
        HEIGHT = objParams.getFloat(1);
        DAMAGE = objParams.getFloat(2);
    }

    // Constants
    private static float WIDTH;
    private static float HEIGHT;
    private static float DAMAGE;

    // If this rock is sharp
    private boolean sharp;

    public ObjectType getType() {
        return ObjectType.ROCK;
    }

    /** Constructor for rock */
    public Rock(Vector2 position, boolean sharp) {
        physicsObject = new BoxObstacle(WIDTH, HEIGHT);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_TERRAIN;
        physicsObject.getFilterData().maskBits = MASK_TERRAIN;
        this.sharp = sharp;
    }

    /** @return Whether this rock is sharp or not. */
    public boolean isSharp() { return sharp; }

    /** @return how much damage this rock does to the player. */
    public static float getDAMAGE() { return DAMAGE; }
}
