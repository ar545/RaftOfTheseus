package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Bullet extends WheelObstacle {
    /** Scaling factor the speed of a bullet. */
    public static float BULLET_SPEED=8;
    /** Health cost for creating a bullet. */
    public static float BULLET_DAMAGE=-5;
    private static float BULLET_SIZE = 0.25f;

    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    public ObjectType getType() { return ObjectType.BULLET; }
    public void setSpeed(float sp){ BULLET_SPEED = sp; }
    public void setDamage(float dm) { BULLET_DAMAGE = dm; }

    public Bullet(Vector2 position, boolean player) {
        super();
        setRadius(BULLET_SIZE);
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        setFriction(0);
        setRestitution(0);
        setLinearDamping(0);
        if(player) {
            // TODO: change this if enemies fire a bullet
            fixture.filter.categoryBits = CATEGORY_PLAYER_BULLET;
            fixture.filter.maskBits = MASK_PLAYER_BULLET;
        } else {
            fixture.filter.categoryBits = CATEGORY_ENEMY_BULLET;
            fixture.filter.maskBits = MASK_ENEMY_BULLET;
        }
    }

    @Override
    public void applyDrag(){
        dragCache.scl(dragCache.len() * dragCoefficient * getCrossSectionalArea());
        body.applyForce(dragCache, getPosition(), true);
    }
}
