package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;

public class Stationary extends GameObject {

    /**
     * Load the constants for the rock
     * @param objParams is the "rock" child of object_settings.json
     */
    public static void setConstants(JsonValue objParams){
        TERRAIN_WIDTH = objParams.getFloat("terrain width");
        TERRAIN_HEIGHT = objParams.getFloat("terrain height");
        SHARP_ROCK_DAMAGE = objParams.getFloat("sharp rock damage");
        SHARP_ROCK_BOUNCE = objParams.getFloat("sharp rock restitution");
        ROCK_WIDTH = objParams.getFloat("rock width");
        ROCK_HEIGHT = objParams.getFloat("rock height");
    }

    // Constants
    private static float TERRAIN_WIDTH;
    private static float TERRAIN_HEIGHT;
    private static float SHARP_ROCK_DAMAGE;
    private static float SHARP_ROCK_BOUNCE = 0.6f;
    private static float ROCK_WIDTH;
    private static float ROCK_HEIGHT;

    // If this rock is sharp
    private boolean sharp;

    // Enum
    public enum RockType{
        ROCK,
        SHARP_ROCK,
        TERRAIN
    }
    private RockType rockType;

    public ObjectType getType() {
        return ObjectType.ROCK;
    }

    /** Constructor for rock */
    public Stationary(Vector2 position, RockType rt) {
        switch(rt){
            case ROCK:
                initBody(position, false, ROCK_WIDTH, ROCK_HEIGHT);
                break;
            case SHARP_ROCK:
                initBody(position, true, ROCK_WIDTH, ROCK_HEIGHT);
                break;
            case TERRAIN:
                initBody(position, false, TERRAIN_WIDTH, TERRAIN_HEIGHT);
                break;
        }

    }

    private void initBody(Vector2 position, boolean sharp, float width, float height){
        physicsObject = new BoxObstacle(width, height);
        this.sharp = sharp;
        setPosition(position);
        if(sharp) physicsObject.setRestitution(SHARP_ROCK_BOUNCE);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_TERRAIN;
        physicsObject.getFilterData().maskBits = MASK_TERRAIN;
        this.sharp = sharp;
    }

    @Override
    protected void setTextureTransform() {
        float w = TERRAIN_WIDTH / texture.getRegionWidth();
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(0.0f,(texture.getRegionHeight()*textureScale.y - TERRAIN_HEIGHT)/2f + 0.5f);
    }

    /** @return Whether this rock is sharp or not. */
    public boolean isSharp() { return sharp; }

    /** @return how much damage this rock does to the player. */
    public static float getSharpRockDamage() { return SHARP_ROCK_DAMAGE; }
}
