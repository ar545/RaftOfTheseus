package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.PolygonObstacle;

public class Wall extends GameObject {
    public ObjectType getType() {
        return ObjectType.OBSTACLE;
    }

    /** Constructor for wall */
    public Wall(float[] polygonVertices){
        physicsObject = new PolygonObstacle(polygonVertices);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_TERRAIN;
        physicsObject.getFilterData().maskBits = MASK_TERRAIN;
    }

    @Override
    public void draw(GameCanvas canvas) {
        // do nothing (wall drawing is disabled for now)
    }
}