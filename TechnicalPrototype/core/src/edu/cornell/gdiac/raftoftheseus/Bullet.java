package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Bullet extends BoxObstacle {
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

    private static Texture text;


    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    public ObjectType getType() { return ObjectType.BULLET; }
    public static void setConstants(JsonValue objParams){
        BULLET_SPEED = objParams.getFloat(0);
        BULLET_DAMAGE = objParams.getFloat(1);
        BULLET_SIZE = objParams.getFloat(2);
        BULLET_RANGE_FLY = objParams.getFloat(3);
        BULLET_RANGE_FALL = objParams.getFloat(4);
    }
    public static void setText(Texture t){
        text = t;
    }


    public Bullet(Vector2 position, boolean player) {
        super(0.6f, 3);
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        setFriction(0);
        setRestitution(0);
        setLinearDamping(0);
        this.player = player;
        originalPos = new Vector2(position);
        if(player) {
            fixture.filter.categoryBits = CATEGORY_PLAYER_BULLET;
            fixture.filter.maskBits = MASK_PLAYER_BULLET;
        } else {
            fixture.filter.categoryBits = CATEGORY_ENEMY_BULLET;
            fixture.filter.maskBits = MASK_ENEMY_BULLET;
        }
        super.setTexture(text);
    }

    @Override
    public void applyDrag(){
        float dist = originalPos.sub(this.getPosition()).len();
        if( dist < BULLET_RANGE_FLY ) {
            dragCache.scl(dragCache.len() * dragCoefficient * getCrossSectionalArea());
            body.applyForce(dragCache, getPosition(), true);
        } else if ( BULLET_RANGE_FLY <= dist && dist <= BULLET_RANGE_FALL) {
            super.applyDrag();
        } else {
            this.setDestroyed(true);
        }
    }

}
