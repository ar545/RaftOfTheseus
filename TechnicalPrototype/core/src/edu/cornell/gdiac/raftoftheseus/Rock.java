package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.PolygonObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Rock extends GameObject {

    public static void setConstants(JsonValue objParams){
        WIDTH = objParams.getFloat(0);
        HEIGHT = objParams.getFloat(1);
    }

    public static float WIDTH;
    public static float HEIGHT;

    public ObjectType getType() {
        return ObjectType.OBSTACLE;
    }

    /** Constructor for rock */
    public Rock(Vector2 position) {
        physicsObject = new BoxObstacle(WIDTH, HEIGHT);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_TERRAIN;
        physicsObject.getFilterData().maskBits = MASK_TERRAIN;
    }

}
