package edu.cornell.gdiac.raftoftheseus.model.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.model.GameObject;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;

public class Spear extends Projectile {

    /**
     * @param objParams the "spear" child in "object_settings.json"
     */
    public static void setConstants(JsonValue objParams){
        SPEED = objParams.getFloat("speed scale");
        DAMAGE = objParams.getFloat("health cost");
        TEXTURE_SCALE = objParams.getFloat("texture scale");
        LENGTH = objParams.getFloat("body length");
        WIDTH = objParams.getFloat("body width");
        SPEAR_XO = objParams.getFloat("x offset");
        SPEAR_YO = objParams.getFloat("y offset");
        ANGLE = objParams.getFloat("angle");
        RANGE_FLY = objParams.getInt("range fly");
        RANGE_FALL = objParams.getInt("range fall");
        FLOAT_RANGE = objParams.getFloat("float range");
        FLOAT_SPEED = objParams.getFloat("float speed");
    }

    // SPEAR
    public static float SPEED;
    /** Health cost for creating a spear. */
    public static float DAMAGE;
    /** Size of a spear. */
    public static float TEXTURE_SCALE;
    private static float LENGTH;
    private static float WIDTH;
    /** Range of a spear. */
    private static int RANGE_FLY;
    private static int RANGE_FALL;
    private static float SPEAR_XO;
    private static float SPEAR_YO;
    private static float ANGLE;
    private static float FLOAT_RANGE;
    private static float FLOAT_SPEED;
    /** Whether it has been fired. */
    private boolean fired = false;


    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    @Override
    public ObjectType getType() { return ObjectType.SPEAR; }

    public static float getSpearRange() {
        return (float)RANGE_FALL;
    }

    /**
     * Constructor for the Spear.
     */
    public Spear(Vector2 pos) {
        // Does not initially interact with anything.
        physicsObject = new BoxObstacle(WIDTH, LENGTH);
        physicsObject.getFilterData().categoryBits = 0;
        physicsObject.getFilterData().maskBits = 0;
        setPosition(pos.add(SPEAR_XO, SPEAR_YO));
        setAngle(ANGLE);
    }

    public void fire(Vector2 dir, Vector2 raft_speed){
        fired = true;
        physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER_BULLET;
        physicsObject.getFilterData().maskBits = MASK_PLAYER_BULLET;
        setAngle(dir.angleDeg()-90f);
        setBody(dir.scl(SPEED).mulAdd(raft_speed, 0.5f));
    }

    /**
     * Change the position of this spear relative to the raft.
     * @param pos the raft position
     */
    public void setFloatPosition(Vector2 pos, float floatTime, float flip){
        float y_float = (float) Math.sin(floatTime*FLOAT_SPEED) * FLOAT_RANGE;
        pos.add(SPEAR_XO * flip, SPEAR_YO + y_float);
        setPosition(pos);
        setAngle(-Math.abs(getAngle()) * flip);
    }

    /**
     * Set Spear to stretch slightly larger than its hitbox.
     */
    @Override
    protected void setTextureTransform() {
        float h = TEXTURE_SCALE / texture.getRegionHeight();
        textureScale = new Vector2(h, h);
        textureOffset = new Vector2(0, 0);
    }

    /**
     * Applying drag to slow the projectile down. Depends on how far the spear has traveled.
     */
    @Override
    public void update(float delta) {
        if(fired) super.update(delta, RANGE_FLY);
    }

    /** @return whether this spear is set to be destroyed. */
    public boolean outMaxDistance(){
        return fired && outMaxDistance(RANGE_FALL);
    }
}
