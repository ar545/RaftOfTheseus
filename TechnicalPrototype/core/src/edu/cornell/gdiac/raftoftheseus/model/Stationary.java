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

/**
 * Class that represents objects that do not move and cannot be collected or destroyed by the player.
 */
public class Stationary extends GameObject {

    // Enum to represent the different types of stationary objects.
    public enum StationaryType {
        REGULAR_ROCK,
        SHARP_ROCK,
        TERRAIN,
        CLIFF_TERRAIN,
        WALL
    }

    /* Pre-defined constants for plants */
    protected static final int plantA = -1;
    protected static final int plantB = -2;
    protected static final int plantC = -3;
    protected static final int plantD = -4;
    protected static final int NON_ROCK = Integer.MAX_VALUE;
    protected static final int REGULAR = 0;

    // Constants
    private static float SHARP_ROCK_DAMAGE;
    private static float SHARP_ROCK_BOUNCE;
    private static float ROCK_WIDTH;
    private static float ROCK_HEIGHT;
    /** size of the plant texture, not hit-box; plant hit-box is the same as terrain hit-box */
    private static float PLANT_SIZE;
    /** size of the texture and hit-box of all terrain, both width and height */
    private static float TERRAIN_SIZE;

    /* Fields */
    private final StationaryType stationaryType;
    /** 1-13 for texture alas, 0 for default, negative for plants */
    protected int terrainType = REGULAR;
    /** @return whether this rock is plant (pre-req: is terrain) */
    private boolean isPlant(){ return isPlant(terrainType); }
    /** @return Whether this is a sharp rock or not. */
    public boolean isSharp() { return stationaryType == StationaryType.SHARP_ROCK; }
    /** @return Whether this is a cliff terrain or not. */
    public boolean hasCliff() { return stationaryType == StationaryType.CLIFF_TERRAIN; }
    /** @return if the stationary type is a terrain type */
    public static boolean isPlant(int rock_int) { return rock_int < REGULAR; }
    @Override
    public ObjectType getType() { return ObjectType.STATIONARY; }
    public StationaryType getStationaryType() { return stationaryType; }

    /**
     * Load the constants for the rock
     * @param objParams is the "rock" child of object_settings.json
     */
    public static void setConstants(JsonValue objParams){
        TERRAIN_SIZE = objParams.getFloat("terrain size");
        SHARP_ROCK_DAMAGE = objParams.getFloat("sharp rock damage");
        SHARP_ROCK_BOUNCE = objParams.getFloat("sharp rock restitution");
        ROCK_WIDTH = objParams.getFloat("rock width");
        ROCK_HEIGHT = objParams.getFloat("rock height");
        PLANT_SIZE = objParams.getFloat("plant size");
    }

    /** Constructor call for rock, fixed terrainType of 0
     * @param position where the object is located.
     * @param rt the type it has. */
    public Stationary(Vector2 position, StationaryType rt) {
        stationaryType = rt;
        switch(stationaryType){
            case REGULAR_ROCK:
                initBoxBody(ROCK_WIDTH, ROCK_HEIGHT);
                break;
            case SHARP_ROCK:
                initBoxBody(ROCK_WIDTH, ROCK_HEIGHT);
                physicsObject.setRestitution(SHARP_ROCK_BOUNCE);
                break;
            case TERRAIN: case CLIFF_TERRAIN: default:
                throw new RuntimeException("Stationary.java: passed param is not ROCK, incorrect constructor use.");
        }
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
    }

    /** Constructor call for terrain, require to know terrain type int
     * @param position where the object is located.
     * @param rt the type it has.
     * @param terrain the terrainType */
    public Stationary(Vector2 position, StationaryType rt, int terrain) {
        stationaryType = rt;
        if(stationaryType == StationaryType.TERRAIN || stationaryType == StationaryType.CLIFF_TERRAIN){
            terrainType = terrain;
            initBoxBody(TERRAIN_SIZE, TERRAIN_SIZE);
            setPosition(position);
            physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        }else{ throw new RuntimeException("Stationary.java: passed param is not TERRAIN, incorrect constructor use."); }
    }

    /** Constructor for walls only
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
        float w = TERRAIN_SIZE / texture.getRegionWidth();
        textureScale = new Vector2(w, w);
        switch(stationaryType){
            case WALL:
                break;
            case CLIFF_TERRAIN: case TERRAIN:
                if(isPlant()) {
                    textureOffset = new Vector2(0.0f,(texture.getRegionHeight() * textureScale.y - PLANT_SIZE)/2f + 0.5f);
                }else{
                    textureOffset = new Vector2(0.0f,(texture.getRegionHeight() * textureScale.y - TERRAIN_SIZE)/2f + 0.5f);
                }
                break;
            default:
                textureOffset = new Vector2(0.0f,(texture.getRegionHeight()*textureScale.y - TERRAIN_SIZE)/2f + 0.5f);
        }
    }

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
            default:
                d.accept(gc, c);
        }
    }
}
