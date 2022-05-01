package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Plant extends GameObject{

    /**
     * Load the constants for the plant
     * @param objParams is the "plant" child of object_settings.json
     */
    public static void setConstants(JsonValue objParams){
        RADIUS = objParams.getFloat("radius");
    }

    // Constants
    private static float RADIUS;

    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.ROCK;
    }

    /** Constructor for rock */
    public Plant(Vector2 position) {
        physicsObject = new WheelObstacle(RADIUS);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().maskBits = MASK_SCENERY;
    }

//    @Override
//    protected void setTextureTransform() {
//        textureOffset = new Vector2(0.0f,(texture.getRegionHeight()*textureScale.y - HEIGHT)/2f + 0.5f);
//    }
}
