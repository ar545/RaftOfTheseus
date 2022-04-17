package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Spear extends GameObject {
    /** Scaling factor the speed of a bullet. */
    public static float BULLET_SPEED;
    /** Health cost for creating a bullet. */
    public static float BULLET_DAMAGE;
    /** Size of a bullet. */
    private static float BULLET_SIZE;
    /** Range of a bullet. */
    private static float BULLET_RANGE_FLY;
    private static float BULLET_RANGE_FALL;
    /** Original bullet position. */
    private Vector2 originalPos;
    /** Whether it was created by the player or not. */
    private boolean player;

    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    public ObjectType getType() { return ObjectType.BULLET; }
    public static void setConstants(JsonValue objParams){
        BULLET_SPEED = objParams.getFloat(0);
        BULLET_DAMAGE = objParams.getFloat(1);
        BULLET_SIZE = objParams.getFloat(2);
        BULLET_RANGE_FLY = objParams.getFloat(3);
        BULLET_RANGE_FALL = objParams.getFloat(4);
    }


    public Spear(Vector2 position, boolean player) {
        physicsObject = new BoxObstacle(0.6f, 2f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);

        if(player) {
            physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER_BULLET;
            physicsObject.getFilterData().maskBits = MASK_PLAYER_BULLET;
        } else {
            physicsObject.getFilterData().categoryBits = CATEGORY_ENEMY_BULLET;
            physicsObject.getFilterData().maskBits = MASK_ENEMY_BULLET;
        }

        physicsObject.setFriction(0);
        physicsObject.setRestitution(0);
        physicsObject.setLinearDamping(0);

        this.player = player;
        originalPos = new Vector2(position);
    }

    @Override
    public void applyDrag(){
        float dist = originalPos.sub(this.getPosition()).len();
        Vector2 dragCache = new Vector2(0,0);
        if( dist < BULLET_RANGE_FLY ) {
            dragCache.scl(dragCache.len() * 0.5f * getCrossSectionalArea());
            physicsObject.getBody().applyForce(dragCache, getPosition(), true);
        } else if ( BULLET_RANGE_FLY <= dist && dist <= BULLET_RANGE_FALL) {
            super.applyDrag();
        } else {
            SoundController.getInstance().playSFX("spear_splash");
            this.setDestroyed(true);
        }
    }

}
