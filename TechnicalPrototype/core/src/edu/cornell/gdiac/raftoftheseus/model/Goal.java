package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.raftoftheseus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Goal extends GameObject {
    public ObjectType getType() {
        return ObjectType.GOAL;
    }

    public Goal(Vector2 position) {
        physicsObject = new CapsuleObstacle(5.8f, 4.1f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_NON_PUSHABLE;
        physicsObject.getFilterData().maskBits = MASK_GOAL;
    }

    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * 1.31f;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(0.0f,(texture.getRegionHeight()*textureScale.y - getHeight())/2f);
    }
}
