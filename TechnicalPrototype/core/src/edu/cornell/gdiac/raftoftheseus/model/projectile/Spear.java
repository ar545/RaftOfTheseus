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
        SPEED = objParams.getFloat(0);
        DAMAGE = objParams.getFloat(1);
        TEXTURE_SCALE = objParams.getFloat(2);
        RANGE_FLY = objParams.getInt(3);
        RANGE_FALL = objParams.getInt(4);
    }

    // SPEAR
    public static float SPEED;
    /** Health cost for creating a spear. */
    public static float DAMAGE;
    /** Size of a spear. */
    public static float TEXTURE_SCALE;
    private static float LENGTH = 1f;
    private static float WIDTH = 0.1f;
    /** Range of a spear. */
    private static int RANGE_FLY;
    private static int RANGE_FALL;

    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    @Override
    public ObjectType getType() { return ObjectType.SPEAR; }

    public static float getSpearRange() {
        return (float)RANGE_FALL;
    }

    /**
     * Constructor for the Spear.
     */
    public Spear(Vector2 pos, Vector2 dir, Vector2 raft_speed) {
        super(pos.cpy());
        physicsObject = new BoxObstacle(WIDTH, LENGTH);
        physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER_BULLET;
        physicsObject.getFilterData().maskBits = MASK_PLAYER_BULLET;
        setAngle(dir.angleDeg()-90f);
        setBody(dir.scl(SPEED).mulAdd(raft_speed, 0.5f));
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
        super.update(delta, RANGE_FLY);
    }

    /** @return whether this spear is set to be destroyed. */
    public boolean outMaxDistance(){
        return outMaxDistance(RANGE_FALL);
    }
}
