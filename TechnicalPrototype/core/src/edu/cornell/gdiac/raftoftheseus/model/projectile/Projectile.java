package edu.cornell.gdiac.raftoftheseus.model.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.model.GameObject;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Projectile extends GameObject {

    /**
     * @param objParams the "spear" child in "object_settings.json"
     */
    public static void setConstants(JsonValue objParams){
        SPEED = objParams.getFloat(0);
        DAMAGE = objParams.getFloat(1);
        TEXTURE_SCALE = objParams.getFloat(2);
        RANGE_FLY = objParams.getLong(3);
        RANGE_FALL = objParams.getLong(4);
    }

    /** Scaling factor the speed of a spear. */
    public static float SPEED;
    /** Health cost for creating a spear. */
    public static float DAMAGE;
    /** Size of a spear. */
    public static float TEXTURE_SCALE;
    private static float LENGTH = 1f;
    private static float WIDTH = 0.1f;
    /** Range of a spear. */
    private static long RANGE_FLY;
    private static long RANGE_FALL;

    /** Original projectile position. */
    private Vector2 originalPos;

    public Projectile(Vector2 pos, String s){
        originalPos = pos;
        if(s.equals("spear")) {
            physicsObject = new BoxObstacle(WIDTH, LENGTH);
            physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER_BULLET;
            physicsObject.getFilterData().maskBits = MASK_PLAYER_BULLET;
        } else if(s.equals("note")){
            physicsObject = new WheelObstacle(WIDTH);
            physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY_BULLET;
            physicsObject.getFilterData().maskBits = MASK_ENEMY_BULLET;
        }
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.setFriction(0);
        physicsObject.setRestitution(0);
        physicsObject.setLinearDamping(0);
        setPosition(pos);
    }

    @Override
    public ObjectType getType() {
        return null;
    }

    /**
     * Applying drag to slow the projectile down. Depends on how far the spear has traveled.
     */
    @Override
    public void update(float delta) {
        super.update(delta);
        if(inFlyDistance()) {
            physicsObject.getBody().applyForce(physicsObject.getLinearVelocity().scl(-2f), getPosition(), true);
        }
    }

    /**
     * @return how far this spear has traveled.
     */
    public float getDistTraveled(){
        return this.getPosition().cpy().sub(originalPos).len();
    }

    /**
     * @return whether the projectile is still flying at max speed
     */
    public boolean inFlyDistance(){
        return getDistTraveled() < RANGE_FLY;
    }

    /**
     * @return whether the projectile should be destroyed
     */
    public boolean outMaxDistance(){
        return getDistTraveled() > RANGE_FALL;
    }

}
