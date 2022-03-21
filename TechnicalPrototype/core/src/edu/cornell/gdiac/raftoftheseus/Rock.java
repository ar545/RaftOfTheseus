package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.PolygonObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

public class Rock extends BoxObstacle {
    public ObjectType getType() {
        return ObjectType.OBSTACLE;
    }

    /** Constructor for rock */
    public Rock(Vector2 position) {
        super(2.98f, 2.98f);
        setPosition(position);
        setBodyType(BodyDef.BodyType.StaticBody);
        fixture.filter.categoryBits = CATEGORY_TERRAIN;
        fixture.filter.maskBits = MASK_TERRAIN;
    }

}
