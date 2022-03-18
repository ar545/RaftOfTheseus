package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.math.Vector2;
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
        setRotationFromDirection();
    }

    private static Random generator = new Random(0); // Make it deterministic
    public static int rollInt(int min, int max) {
        return generator.nextInt(max-min+1)+min;
    }

    /** constructor with random direction */
    public Current(){
        super();
        radius = 50;

        switch(rollInt(0,3)){
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

    // TODO: fix
    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     *
     * Implementations of this method should NOT retain a reference to World.
     * That is a tight coupling that we should avoid.
     *
     * @param world Box2D world to store body
     *
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
//        // Make a body, if possible
//        bodyinfo.active = true;
//        body = world.createBody(bodyinfo);
//        body.setUserData(this);
//
//        // Only initialize if a body was created.
//        if (body != null) {
//            createFixtures();
//            return true;
//        }
//
//        bodyinfo.active = false;
//        return false;
        return false;
    }

    // TODO: fix
    /**
     * Destroys the physics Body(s) of this object if applicable,
     * removing them from the world.
     *
     * @param world Box2D world that stores body
     */
    public void deactivatePhysics(World world) {
        // Should be good for most (simple) applications.
//        if (body != null) {
//            // Snapshot the values
//            setBodyState(body);
//            world.destroyBody(body);
//            body = null;
//            bodyinfo.active = false;
//
    }
}
