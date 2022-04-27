package edu.cornell.gdiac.raftoftheseus.model.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.model.GameObject;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Projectile extends GameObject {

    /** Original projectile position. */
    private Vector2 originalPos;

    public Projectile(){}

    @Override
    public ObjectType getType() {
        return null;
    }

    /** Set attributes of physics body after declaration. */
    public void setBody(Vector2 speed){
        physicsObject.setLinearVelocity(speed);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.setFriction(0);
        physicsObject.setRestitution(0);
        physicsObject.setLinearDamping(0);
        originalPos = getPosition().cpy();
    }

    /** @return how far this spear has traveled. */
    public float getDistTraveled(){
        return this.getPosition().cpy().sub(originalPos).len();
    }
    /** @return whether the projectile is still flying at max speed. */
    public boolean inFlyDistance(float RANGE_FLY){ return getDistTraveled() < RANGE_FLY; }
    /** @return whether the projectile should be destroyed. */
    public boolean outMaxDistance(float RANGE_FALL){
        return getDistTraveled() > RANGE_FALL;
    }

    /** Update projectile based on its range. */
    public void update(float delta, float RANGE_FLY) {
        super.update(delta);
        if(inFlyDistance(RANGE_FLY)) {
            physicsObject.getBody().applyForce(physicsObject.getLinearVelocity().scl(-2f), getPosition(), true);
        }
    }
}
