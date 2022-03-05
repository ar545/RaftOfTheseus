package edu.cornell.gdiac.optimize.entity;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.optimize.Environment;
import edu.cornell.gdiac.optimize.RandomController;

public class Current extends Environment {


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
        /** A current that has no effect on player, for testing purposes only */
        NONE
    }

    /** direction of the current */
    private Direction direction;

    /** speed of the current (keep in mind current speed of the player is 4) */
    private static final int CURRENT_SPEED = 2;

    /** get the type of wood objects
     * @return object type wood */
    @Override
    public Environment.ObjectType getType() {
        return Environment.ObjectType.CURRENT;
    }

    /** constructor with known direction */
    public Current(Direction direction){
        super();
        radius = 50;
        this.direction = direction;
        setRotationFromDirection();
    }

    /** constructor with random direction */
    public Current(){
        super();
        radius = 50;
        switch(RandomController.rollInt(0,3)){
            case 0:
                direction = Direction.NORTH;
                break;
            case 1:
                direction = Direction.EAST;
                break;
            case 2:
                direction = Direction.SOUTH;
                break;
            case 3:
                direction = Direction.WEST;
                break;
            default:
                direction = Direction.NONE;
                break;
        }
        setRotationFromDirection();
    }

    private void setRotationFromDirection() {
        switch(direction){
            case EAST:
                rotation = 0.0f;
                break;
            case NORTH:
                rotation = 90.0f;
                break;
            case WEST:
                rotation = 180.0f;
                break;
            case SOUTH:
                rotation = -90.0f;
                break;
            default:
                break;
        }
    }

    /** get the direction of the current */
    public Direction getDirection() {
        return direction;
    }

    /** get the direction vector of the current */
    public Vector2 getDirectionVector() {
        switch (this.direction){
            case EAST:
                return new Vector2(CURRENT_SPEED, 0);
            case WEST:
                return new Vector2(-CURRENT_SPEED, 0);
            case NORTH:
                return new Vector2(0, CURRENT_SPEED);
            case SOUTH:
                return new Vector2(0, -CURRENT_SPEED);
            default:
                return new Vector2(0, 0);
        }
    }

    /** set the direction of the current */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
