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
        physicsObject = new BoxObstacle(0.3f, 2f);
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
     * Applying drag to slow the object down. Depends on how far the spear has traveled.
     */
    @Override
    public void applyDrag(){
        // TODO: This method shouldn't be coupled to GameObject.applyDrag(), which no longer works because it was tied to the old implementation of currents.
        Vector2 dragCache = new Vector2(0,0);
        if(inFlyDistance()) {
            dragCache.scl(dragCache.len() * 0.5f * getCrossSectionalArea());
            physicsObject.getBody().applyForce(dragCache, getPosition(), true);
        } else if (inFallDistance()) {
            super.applyDrag();
        }
    }

    /**
     * @return how far this spear has traveled.
     */
    private float getDistTraveled(){
        return originalPos.sub(this.getPosition()).len();
    }

    /**
     * @return whether the spear is still flying at max speed
     */
    private boolean inFlyDistance(){
        return getDistTraveled() < SPEAR_RANGE_FLY;
    }

    /**
     * @return whether the spear is slowing down
     */
    private boolean inFallDistance(){
        return !inFlyDistance() && getDistTraveled() <= SPEAR_RANGE_FALL;
    }

    /**
     * @return wheter the spear should be destroyed
     */
    public boolean outMaxDistance(){
        return getDistTraveled() > SPEAR_RANGE_FALL;
    }

}
