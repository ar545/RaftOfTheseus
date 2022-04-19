package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;

public class Spear extends GameObject {
    /** Scaling factor the speed of a bullet. */
    public static float SPEAR_SPEED;
    /** Health cost for creating a bullet. */
    public static float SPEAR_DAMAGE;
    /** Size of a bullet. */
    private static float SPEAR_TEXTURE_SCALE;
    private static float SPEAR_LENGTH = 1f;
    private static float SPEAR_WIDTH = 0.1f;
    /** Range of a bullet. */
    private static long SPEAR_RANGE_FLY;
    private static long SPEAR_RANGE_FALL;
    /** Original bullet position. */
    private Vector2 originalPos;
    /** Timer to destroy bullet. */

    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    public ObjectType getType() { return ObjectType.SPEAR; }
    public static void setConstants(JsonValue objParams){
        SPEAR_SPEED = objParams.getFloat(0);
        SPEAR_DAMAGE = objParams.getFloat(1);
        SPEAR_TEXTURE_SCALE = objParams.getFloat(2);
        SPEAR_RANGE_FLY = objParams.getLong(3);
        SPEAR_RANGE_FALL = objParams.getLong(4);
    }

    /**
     * Constructor for the Spear.
     */
    public Spear(Vector2 position) {
        physicsObject = new BoxObstacle(SPEAR_WIDTH, SPEAR_LENGTH);

        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);

        physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER_BULLET;
        physicsObject.getFilterData().maskBits = MASK_PLAYER_BULLET;

        physicsObject.setFriction(0);
        physicsObject.setRestitution(0);
        physicsObject.setLinearDamping(0);

        originalPos = new Vector2(position);
        setPosition(position);
    }

    /**
     * Applying drag to slow the spear down. Depends on how far the spear has traveled.
     */
    @Override
    public void update(float delta) {
        super.update(delta);
        if(inFlyDistance()) {
            physicsObject.getBody().applyForce(physicsObject.getLinearVelocity().scl(-2f), getPosition(), true);
        }
    }

    /**
     * Set Spear to stretch slightly larger than its hitbox.
     */
    @Override
    protected void setTextureTransform() {
        float h = SPEAR_TEXTURE_SCALE / texture.getRegionHeight();
        textureScale = new Vector2(h, h);
        textureOffset = new Vector2(0, 0);
    }

    /**
     * @return how far this spear has traveled.
     */
    private float getDistTraveled(){
        return this.getPosition().cpy().sub(originalPos).len();
    }

    /**
     * @return whether the spear is still flying at max speed
     */
    private boolean inFlyDistance(){
        return getDistTraveled() < SPEAR_RANGE_FLY;
    }

    /**
     * @return wheter the spear should be destroyed
     */
    public boolean outMaxDistance(){
        return getDistTraveled() > SPEAR_RANGE_FALL;
    }

}
