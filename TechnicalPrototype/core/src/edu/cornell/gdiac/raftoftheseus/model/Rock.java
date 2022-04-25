package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Rock extends GameObject {

    /**
     * Load the constants for the rock
     * @param objParams is the "rock" child of object_settings.json
     */
    public static void setConstants(JsonValue objParams){
        WIDTH = objParams.getFloat("width");
        HEIGHT = objParams.getFloat("height");
        DAMAGE = objParams.getFloat("damage");
        RADIUS = objParams.getFloat("radius");
    }

    // Constants
    private static float RADIUS;
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
//        physicsObject = new WheelObstacle(RADIUS);
        setPosition(position.add(0, HEIGHT/3));
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_TERRAIN;
        physicsObject.getFilterData().maskBits = MASK_TERRAIN;
        this.sharp = sharp;
    }

    @Override
    protected void setTextureTransform() {
        float w = WIDTH / texture.getRegionWidth();
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(0.0f,(texture.getRegionHeight()*textureScale.y - HEIGHT)/2f);
    }

    /** @return Whether this rock is sharp or not. */
    public boolean isSharp() { return sharp; }

    /** @return how much damage this rock does to the player. */
    public static float getDAMAGE() { return DAMAGE; }
}
