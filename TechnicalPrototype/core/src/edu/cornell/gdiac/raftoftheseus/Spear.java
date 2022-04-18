package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Spear extends GameObject {
    /** Scaling factor the speed of a bullet. */
    public static float SPEAR_SPEED;
    /** Health cost for creating a bullet. */
    public static float SPEAR_DAMAGE;
    /** Size of a bullet. */
    private static float SPEAR_SIZE;
    /** Range of a bullet. */
    private static float SPEAR_RANGE_FLY;
    private static float SPEAR_RANGE_FALL;
    /** Original bullet position. */
    private Vector2 originalPos;

    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    public ObjectType getType() { return ObjectType.SPEAR; }
    public static void setConstants(JsonValue objParams){
        SPEAR_SPEED = objParams.getFloat(0);
        SPEAR_DAMAGE = objParams.getFloat(1);
        SPEAR_SIZE = objParams.getFloat(2);
        SPEAR_RANGE_FLY = objParams.getFloat(3);
        SPEAR_RANGE_FALL = objParams.getFloat(4);
    }

    /**
     * Constructor for the Spear.
     */
    public Spear(Vector2 position) {
        physicsObject = new BoxObstacle(0.1f, 2f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);

        physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER_BULLET;
        physicsObject.getFilterData().maskBits = MASK_PLAYER_BULLET;

        physicsObject.setFriction(0);
        physicsObject.setRestitution(0);
        physicsObject.setLinearDamping(0);

        originalPos = new Vector2(position);
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

    @Override
    protected void setTextureTransform() {
        float h = getHeight() / texture.getRegionHeight();
        textureScale = new Vector2(h, h);
        textureOffset = new Vector2(0, 0);
    }

    /**
     * @return how far this spear has traveled.
     */
    private float getDistTraveled(){
        return this.getPosition().sub(originalPos).len();
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
