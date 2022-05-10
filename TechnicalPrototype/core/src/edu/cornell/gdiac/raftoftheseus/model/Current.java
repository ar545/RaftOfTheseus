package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;

public class Current extends GameObject {

    /**
     * Set the strength and size of currents
     * @param objParams current child
     */
    public static void setConstants(JsonValue objParams){
        SIZE = objParams.getFloat("size");
        WEAK_MAGNITUDE = objParams.getFloat("weak");
        STRONG_MAGNITUDE = objParams.getFloat("strong");
    }

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

    // ATTRIBUTES
    /** Direction of the current */
    private final Direction direction;
    /** Magnitude of the current. The speed at which a current flows = factor * magnitude, in units per second
     * Current Magnitude Ratio Constant, used in current constructor calls */
    private static float SIZE;
    private static float WEAK_MAGNITUDE;
    private static float STRONG_MAGNITUDE;
    private float drawAngle;
    private boolean isStrong;

    // METHODS
    public ObjectType getType() {
        return ObjectType.CURRENT;
    }

    /**
     * Constructor with known direction
     * Note that the map representation of the Current is rotated but the current itself is not.
     */
    public Current(Vector2 position, Direction direction, boolean isStrong){
        physicsObject = new BoxObstacle(SIZE, SIZE);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.setSensor(true);
        this.direction = direction;
        drawAngle = getDrawAngle();
        physicsObject.getFilterData().categoryBits = CATEGORY_CURRENT;
        physicsObject.getFilterData().maskBits = MASK_CURRENT;
        this.isStrong = isStrong;
    }

    @Override
    public void draw(GameCanvas canvas) {
        draw(canvas, Color.WHITE);
    }

    @Override
    public void draw(GameCanvas canvas, Color color) {
        if (texture != null) {
            canvas.draw(texture, color, origin.x, origin.y, getX() + textureOffset.x, getY() + textureOffset.y, drawAngle, textureScale.x, textureScale.y);
        }
    }

    /** Returns how many degrees to rotate the graphic based on the direction. */
    private float getDrawAngle() {
        switch(direction){
            case EAST:
                return 0.0f;
            case NORTH:
                return 90.0f;
            case WEST:
                return 180.0f;
            case SOUTH:
                return -90.0f;
            case NORTH_EAST:
                return 45.0f;
            case WEST_NORTH:
                return 135.0f;
            case SOUTH_WEST:
                return -135.0f;
            case EAST_SOUTH:
                return -45.0f;
            default:
                throw new RuntimeException("Direction mismatch at box2d location:" + getPosition().x + ", " + getPosition().y);
        }
    }

    /** get the direction of the current */
    public Direction getDirection() {
        return direction;
    }

    /** get the direction vector of the current
     * @return currents should be normalized to their magnitude. */
    public Vector2 getDirectionVector() {
        float magnitude = isStrong ? STRONG_MAGNITUDE : WEAK_MAGNITUDE;
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

    /**
     * Scales the given current magnitude to the 0...1 range, for use in the shader.
     */
    public static float getMaxMagnitude() {
        return STRONG_MAGNITUDE;
    }
}
