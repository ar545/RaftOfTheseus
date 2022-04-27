package edu.cornell.gdiac.raftoftheseus.model.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Note extends Projectile {

    /**
     * @param objParams the "spear" child in "object_settings.json"
     */
    public static void setConstants(JsonValue objParams){
        SPEED = objParams.getFloat(0);
        DAMAGE = objParams.getFloat(1);
        TEXTURE_SCALE = objParams.getFloat(2);
        RANGE_FLY = objParams.getInt(3);
        RANGE_FALL = objParams.getInt(4);
        FORCE = objParams.getInt(5);
        WIDTH = objParams.getInt(6);
    }

    // CONSTANTS
    public static float SPEED;
    /** Health cost for creating a spear. */
    public static float DAMAGE;
    /** Size of a spear. */
    public static float TEXTURE_SCALE;
    private static float WIDTH;
    /** Range of a spear. */
    private static int RANGE_FLY;
    private static int RANGE_FALL;
    private static float FORCE;

    public Note(Vector2 pos, Vector2 dir) {
        physicsObject = new WheelObstacle(WIDTH);
        physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY_BULLET;
        physicsObject.getFilterData().maskBits = MASK_NOTE;
        physicsObject.setPosition(pos);
        setBody(dir.scl(SPEED));
    }

    @Override
    public ObjectType getType() { return ObjectType.NOTE; }

    @Override
    protected void setTextureTransform() {
        float h = TEXTURE_SCALE / texture.getRegionHeight();
        textureScale = new Vector2(h, h);
        textureOffset = new Vector2(0, 0);
    }

    /** @return the float of this note that will be applied to the raft */
    public Vector2 getForce() {
        return getLinearVelocity().cpy().nor().scl(FORCE).rotateDeg(180);
    }

    /**
     * Applying drag to slow the projectile down. Depends on how far the spear has traveled.
     */
    @Override
    public void update(float delta) {}

    /** @return whether this spear is set to be destroyed. */
    public boolean outMaxDistance(){
        return outMaxDistance(RANGE_FALL);
    }
}
