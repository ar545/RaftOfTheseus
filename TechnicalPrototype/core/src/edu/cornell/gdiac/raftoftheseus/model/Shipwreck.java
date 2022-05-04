package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.SimpleObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class Shipwreck extends GameObject{

    /**
     * Load the constants for the rock
     * @param objParams is the "rock" child of object_settings.json
     */
    public static void setConstants(JsonValue objParams){
        WIDTH = objParams.getFloat("width");
        HEIGHT = objParams.getFloat("height");
        HEALTH = objParams.getInt("health");
        DROPS = objParams.getInt("drops");
        HIT_WIDTH = objParams.getFloat("hit box width");
        HIT_HEIGHT = objParams.getFloat("hit box height");
        DRAW_SCALE = objParams.getFloat("draw size");
        Y_ADJUSTMENT = objParams.getFloat("y adjustment");
    }

    // Constants
    private static float WIDTH;
    private static float HEIGHT;
    private static float HIT_WIDTH;
    private static float HIT_HEIGHT;
    private static float DRAW_SCALE;
    private static float Y_ADJUSTMENT;
    private static int HEALTH;
    private static int DROPS;
    private static int FRAME_COUNT;

    // The current health of this shipwreck.
    private int health;

    // Additional physics object
    private SimpleObstacle hitbox;

    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.SHIPWRECK;
    }

    /**
     * Constructor for Shipwreck, creates a second sensor physics body to allow for easier hitting.
     * @param position where this Shipwreck is location
     */
    public Shipwreck(Vector2 position) {
        physicsObject = new BoxObstacle(WIDTH, HEIGHT);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_TERRAIN;
        physicsObject.getFilterData().maskBits = MASK_TERRAIN;

        hitbox = new BoxObstacle(HIT_WIDTH, HIT_HEIGHT);
        hitbox.setPosition(physicsObject.getX(), physicsObject.getY() + HIT_HEIGHT/2 - HEIGHT/2);
        hitbox.setSensor(true);
        hitbox.setBodyType(BodyDef.BodyType.StaticBody);
        hitbox.getFilterData().categoryBits = CATEGORY_DESTRUCTIBLE;
        hitbox.getFilterData().maskBits = MASK_DESTRUCTIBLE;
        health = HEALTH;
    }

    /** @return how much wood a shipwreck drops. */
    public static int getDrops() { return DROPS; }

    /** @return whether this shipwreck should be destroyed or not. */
    public boolean noHealth() { return health <= 0; }

    /** Reduce shipwreck health by one. */
    public void takeDamage() { health--; }

    @Override
    protected void setTextureTransform() {
        float w = DRAW_SCALE / texture.getRegionWidth();
        float h = DRAW_SCALE / texture.getRegionHeight();
        textureScale = new Vector2(w, h);
        textureOffset = new Vector2(0, DRAW_SCALE/2 - HEIGHT/2 + Y_ADJUSTMENT);
    }

    @Override
    public void deactivatePhysics(World world) {
        super.deactivatePhysics(world);
        hitbox.deactivatePhysics(world);
    }

    @Override
    public void activatePhysics(World world) {
        super.activatePhysics(world);
        hitbox.activatePhysics(world);
        hitbox.getBody().setUserData(this);
    }

    @Override
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        hitbox.drawDebug(canvas);
    }

    @Override
    public void draw(GameCanvas canvas){
        int frame;
        if (health == 2) {
            frame = 0;
        } else {
            frame = 2;
        }
        ((FilmStrip) texture).setFrame(frame);
        super.draw(canvas);
    }
}

