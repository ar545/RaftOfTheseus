package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

/**
 * Model class for driftwood.
 */
public class Wood extends GameObject {
    // ATTRIBUTES
    /** How many logs is in this pile of wood. player health will add correspondingly */
    private final float wood;

    // CONSTANTS
    /** the wood health scale */
    private final static float WOOD_HEALTH_SCALE = 4f;
    /** the maximum log generated for each pile of wood */
    private final static int RANDOM_WOOD_GENERATION = 5;

    public ObjectType getType() {
        return ObjectType.WOOD;
    }

    /** Constructor for Wood object
     * @param position: position of wood
     * @param value: amount of wood
     */
    public Wood(Vector2 position, int value) {
        physicsObject = new WheelObstacle(1.25f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.DynamicBody);
        physicsObject.setSensor(true);
        physicsObject.getFilterData().categoryBits = CATEGORY_PUSHABLE;
        physicsObject.getFilterData().maskBits = MASK_WOOD;

        physicsObject.setDensity(0.2f);
        wood = value;
    }

    /** generate wood at random location */
    public Wood(Vector2 bound){
        this(new Vector2(bound.x * (float) Math.random(), (float) Math.random() * bound.y), RANDOM_WOOD_GENERATION);
    }

    /** return the number of logs in this pile of wood
     * @return float representing player health replenish */
    public float getWood() {
        return wood * WOOD_HEALTH_SCALE;
    }
}
