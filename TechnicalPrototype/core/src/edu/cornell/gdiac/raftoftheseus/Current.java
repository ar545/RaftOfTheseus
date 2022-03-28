package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.PolygonObstacle;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

import java.util.Random;

public class Current extends BoxObstacle {

    /**
     * Enum specifying the type of this game object.
     */
    public enum Direction {
        /** A current that push the player upward */
        NORTH,
        /** A current that push the player downward */
        SOUTH,
        /** A current that push the player rightward */
        EAST,
        /** A current that push the player leftward */
        WEST,
        /** A current that push the player up-right, from downtown to cornell */
        NORTH_EAST,
        /** A current that push the player down-left */
        SOUTH_WEST,
        /** A current that push the player down-right */
        EAST_SOUTH,
        /** A current that push the player up-left */
        WEST_NORTH,
        /** A current that has no effect on player, for testing purposes only */
        NONE
    }

    /** The speed at which a current flows = factor * magnitude, in units per second
     * Current Magnitude Ratio Constant, used in current constructor calls */
    private static final int CURRENT_SPEED_FACTOR = 1;

    // ATTRIBUTES
    /** Direction of the current */
    private final Direction direction;
    /** Magnitude of the current */
    private final float magnitude;

    // METHODS
    public ObjectType getType() {
        return ObjectType.CURRENT;
    }

    /** constructor with known direction */
    public Current(Vector2 position, Direction direction, int magnitude_input){
        super(3f, 3f);
        setPosition(position);
        setBodyType(BodyDef.BodyType.StaticBody);
        setSensor(true);
        this.direction = direction;
        setRotationFromDirection();
        fixture.filter.categoryBits = CATEGORY_CURRENT;
        fixture.filter.maskBits = MASK_CURRENT;
        magnitude = (float) magnitude_input * CURRENT_SPEED_FACTOR;
    }

    private void setRotationFromDirection() {
        switch(direction){
            case EAST:
                setAngle(0.0f);
                break;
            case NORTH:
                setAngle(90.0f);
                break;
            case WEST:
                setAngle(180.0f);
                break;
            case SOUTH:
                setAngle(-90.0f);
                break;
            case NORTH_EAST:
                setAngle(45.0f);
                break;
            case WEST_NORTH:
                setAngle(135.0f);
                break;
            case SOUTH_WEST:
                setAngle(-135.0f);
                break;
            case EAST_SOUTH:
                setAngle(-45.0f);
                break;
            default:
                break;
        }
    }

    /** get the direction of the current */
    public Direction getDirection() {
        return direction;
    }

    /** get the direction vector of the current
     * @return currents should be normalized to their magnitude. */
    public Vector2 getDirectionVector() {
        switch (this.direction){
            case EAST:
                return new Vector2(magnitude, 0);
            case WEST:
                return new Vector2(-magnitude, 0);
            case NORTH:
                return new Vector2(0, magnitude);
            case SOUTH:
                return new Vector2(0, -magnitude);
            case NORTH_EAST:
                return new Vector2(1, 1).nor().scl(magnitude);
            case EAST_SOUTH:
                return new Vector2(1, -1).nor().scl(magnitude);
            case SOUTH_WEST:
                return new Vector2(-1, -1).nor().scl(magnitude);
            case WEST_NORTH:
                return new Vector2(-1, 1).nor().scl(magnitude);
            default:
                return new Vector2(0, 0);
        }
    }
}
