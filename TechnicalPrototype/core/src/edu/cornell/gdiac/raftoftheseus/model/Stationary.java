package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.PolygonObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Class that represents objects that do not move and cannot be collected or destroyed by the player.
 */
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
        PLANT_RADIUS = objParams.getFloat("plant radius");
    }

    // Constants
    private static float TERRAIN_WIDTH;
    private static float TERRAIN_HEIGHT;
    private static float SHARP_ROCK_DAMAGE;
    private static float SHARP_ROCK_BOUNCE;
    private static float ROCK_WIDTH;
    private static float ROCK_HEIGHT;
    private static float PLANT_RADIUS;

    // If this object is sharp (must be a sharp rock)
    private boolean sharp = false;

    // Enum to represent the different types of stationary objects.
    public enum StationaryType {
        REGULAR_ROCK,
        SHARP_ROCK,
        TERRAIN,
        CLIFF_TERRAIN,
        PLANT,
        WALL
    }
    private final StationaryType stationaryType;
    protected final int terrainType = 0;

    @Override
    public ObjectType getType() {
        return ObjectType.STATIONARY;
    }
    public StationaryType getStationaryType() { return stationaryType; }

    /**
     * Generalized constructor for collision or no collision only stationary objects.
     * @param position where the object is located.
     * @param rt the type it has.
     */
    public Stationary(Vector2 position, StationaryType rt) {
        stationaryType = rt;
        switch(stationaryType){
            case REGULAR_ROCK:
                initBoxBody(ROCK_WIDTH, ROCK_HEIGHT);
                break;
            case SHARP_ROCK:
                initBoxBody(ROCK_WIDTH, ROCK_HEIGHT);
                physicsObject.setRestitution(SHARP_ROCK_BOUNCE);
                this.sharp = true;
                break;
            case TERRAIN: case CLIFF_TERRAIN:
                initBoxBody(TERRAIN_WIDTH, TERRAIN_HEIGHT);
                break;
            case PLANT:
                initWheelBody(PLANT_RADIUS);
                break;
            default:
                throw new RuntimeException("Incorrect constructor called.");
        }
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
    }

    /**
     * Constructor for walls only
     * @param polygonVertices where the wall's corners are located
     */
    public Stationary(float[] polygonVertices){
        physicsObject = new PolygonObstacle(polygonVertices);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        setTerrainBits();
        stationaryType = StationaryType.WALL;
    }

    /**
     * @param width of box obstacle
     * @param height of box obstacle
     */
    private void initBoxBody(float width, float height){
        physicsObject = new BoxObstacle(width, height);
        setTerrainBits();
    }

    /** Set the masking as terrain for collision detection. */
    private void setTerrainBits(){
        physicsObject.getFilterData().categoryBits = CATEGORY_TERRAIN;
        physicsObject.getFilterData().maskBits = MASK_TERRAIN;
    }

    /** @param radius of wheel obstacle */
    private void initWheelBody(float radius){
        physicsObject = new WheelObstacle(radius);
        physicsObject.getFilterData().maskBits = MASK_SCENERY;
    }

    @Override
    protected void setTextureTransform() {
        float w = TERRAIN_WIDTH / texture.getRegionWidth();
        textureScale = new Vector2(w, w);
        switch(stationaryType){
            case PLANT:
                textureOffset = new Vector2(0.0f,(texture.getRegionHeight()*textureScale.y - PLANT_RADIUS)/2f + 0.5f);
            case WALL:
                break;
            default:
                textureOffset = new Vector2(0.0f,(texture.getRegionHeight()*textureScale.y - TERRAIN_HEIGHT)/2f + 0.5f);
        }
    }

    /** @return Whether this is a sharp rock or not. */
    public boolean isSharp() { return sharp && stationaryType == StationaryType.SHARP_ROCK; }

    /** @return how much damage this rock does to the player. */
    public static float getSharpRockDamage() { return SHARP_ROCK_DAMAGE; }

    @Override
    public void draw(GameCanvas canvas, Color c){
        drawer(canvas, c, super::draw);
    }

    @Override
    public void draw(GameCanvas canvas){
        drawer(canvas, Color.WHITE, super::draw);
    }

    /**
     * Function to factor out matching logic for drawing.
     * @param gc drawing context
     * @param c color to uses
     * @param d the superclass draw function
     */
    private void drawer(GameCanvas gc, Color c, BiConsumer<GameCanvas, Color> d){
        switch (stationaryType){
            case WALL:
                break;
            case PLANT:
                break;
            default:
                d.accept(gc, c);
        }
    }
}
