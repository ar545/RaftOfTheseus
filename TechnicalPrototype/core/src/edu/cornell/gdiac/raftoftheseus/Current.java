package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;

import java.util.Random;

public class Current extends GameObject {

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

    // ATTRIBUTES
    /** Direction of the current */
    private Direction direction;
    /** Current speed */
    private float speed;

//    /** speed of the current (keep in mind current speed of the player is 4) */
//    private static final int CURRENT_SPEED = 2;

    // METHODS
    public ObjectType getType() {
        return ObjectType.CURRENT;
    }

    /** constructor with known direction */
    public Current(Vector2 position, Direction direction, float speed){
        super();
        this.direction = direction;
        this.speed = speed;
        setPosition(position);
        setBodyType(BodyDef.BodyType.StaticBody);
        setSensor(true);
        setRotationFromDirection();
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
                return new Vector2(speed, 0);
            case WEST:
                return new Vector2(-speed, 0);
            case NORTH:
                return new Vector2(0, speed);
            case SOUTH:
                return new Vector2(0, -speed);
            default:
                return new Vector2(0, 0);
        }
    }

    public float getSpeed() {return speed;}

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /** set the direction of the current */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    // TODO: should the currents update?
    public void update(float dt) {
        // nothing for now
    }
}
