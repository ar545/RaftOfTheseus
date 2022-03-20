package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.PolygonObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Wall extends PolygonObstacle {
    public ObjectType getType() {
        return ObjectType.OBSTACLE;
    }

//    // rocks don't update
//    public void update(float dt) {
//        // nothing
//    }

    /** Constructor for wall */
    public Wall(float[] polygonVertices){
        super(polygonVertices);
        setBodyType(BodyDef.BodyType.StaticBody);
    }
}