package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.util.TextureHolder;
import edu.cornell.gdiac.raftoftheseus.obstacle.SimpleObstacle;

/**
 * Base class for all Model objects in the game.
 */
public abstract class GameObject {
    /**
     * Enum specifying the type of this game object.
     */
    public enum ObjectType {
        RAFT, // aka Player
        WOOD,
        STATIONARY, // Rock, Plants, Terrain
        CURRENT,
        SHARK,
        GOAL,
        TREASURE,
        SPEAR,
        HYDRA,
        SIREN,
        NOTE,
        SHIPWRECK
    }

    /** Collision filtering categories */
    public final static short CATEGORY_PLAYER = 1<<1;
    protected final static short CATEGORY_ENEMY = 1<<2;
    protected final static short CATEGORY_PLAYER_BULLET = 1<<3;
    protected final static short CATEGORY_ENEMY_BULLET = 1<<4;
    protected final static short CATEGORY_CURRENT = 1<<5;
    public final static short CATEGORY_TERRAIN = 1<<6;
    protected final static short CATEGORY_PUSHABLE = 1<<7;
    protected final static short CATEGORY_NON_PUSHABLE = 1<<8;
    protected final static short CATEGORY_PLAYER_SENSOR = 1<<9;
    public final static short CATEGORY_LIGHT_BLOCK = 1<<10;
    public final static short CATEGORY_LIGHT_NON = 1<<11;
    protected final static short CATEGORY_DESTRUCTIBLE = 1<<12;
    /** Collision filtering masks */
    protected final static short MASK_PLAYER = CATEGORY_ENEMY | CATEGORY_ENEMY_BULLET | CATEGORY_CURRENT
            | CATEGORY_TERRAIN;
    protected final static short MASK_PLAYER_BULLET = CATEGORY_ENEMY | CATEGORY_TERRAIN | CATEGORY_DESTRUCTIBLE;
    protected final static short MASK_ENEMY = CATEGORY_PLAYER | CATEGORY_PLAYER_BULLET | CATEGORY_CURRENT
            | CATEGORY_TERRAIN | CATEGORY_PUSHABLE | CATEGORY_NON_PUSHABLE;
    protected final static short MASK_NOTE = CATEGORY_PLAYER_SENSOR;
    protected final static short MASK_CURRENT = CATEGORY_PLAYER | CATEGORY_ENEMY | CATEGORY_PUSHABLE;
    protected final static short MASK_TERRAIN = CATEGORY_PLAYER | CATEGORY_ENEMY |
            CATEGORY_ENEMY_BULLET | CATEGORY_PUSHABLE | CATEGORY_LIGHT_BLOCK;
    protected final static short MASK_WOOD = CATEGORY_PLAYER_SENSOR | CATEGORY_ENEMY | CATEGORY_CURRENT | CATEGORY_TERRAIN;
    protected final static short MASK_TREASURE = CATEGORY_PLAYER_SENSOR;// treasure isn't pushed around by anything
    protected final static short MASK_GOAL = CATEGORY_PLAYER_SENSOR | CATEGORY_ENEMY;
    protected final static short MASK_SIREN = CATEGORY_PLAYER_BULLET; // Siren only interacts with bullet
    protected final static short MASK_PLAYER_SENSOR = CATEGORY_PUSHABLE | CATEGORY_NON_PUSHABLE | CATEGORY_ENEMY_BULLET;
    protected final static short MASK_DESTRUCTIBLE = CATEGORY_PLAYER_BULLET;
    protected final static short MASK_SCENERY = 0; // Plants don't interact with anything.

    /** How much to scale the texture before displaying (Box2D units / texture pixels) */
    protected Vector2 textureScale;
    /** How much to translate the texture before displaying (in Box2D units) */
    protected Vector2 textureOffset;
    /** TextureRegion for this object */
    protected TextureRegion texture; // should be a TextureRegion and not a Texture, in case we want to optimize memory usage later
    /** The texture origin for drawing */
    protected Vector2 origin = new Vector2();

    // ABSTRACT METHODS

    /**
     * Returns the type of this object. We use this instead of runtime-typing for performance reasons.
     *
     * @return the type of this object.
     */
    public abstract ObjectType getType();

    // PHYSICS INTERFACE

    /** An Obstacle which encapsulates Box2D physics behavior and attributes. This is the GameObject's "hit box". */
    protected SimpleObstacle physicsObject;

    /*
     * The following methods should all assume that physicsObject is not null. This means that any subclass of
     * GameObject should make sure to initialize its physicsObject before doing other things.
     */

    public Vector2 getPosition() {
        return physicsObject.getPosition();
    }

    public void setPosition(Vector2 position) {
        physicsObject.setPosition(position);
    }

    public Vector2 getLinearVelocity() {
        return physicsObject.getLinearVelocity();
    }

    public float getX() {
        return physicsObject.getX();
    }

    public float getY() {
        return physicsObject.getY();
    }

    public float getAngle() {
        return physicsObject.getAngle();
    }

    public void setAngle(float angle) {
        physicsObject.setAngle(angle);
    }

    public float getHeight() {
        return physicsObject.getHeight();
    }

    public float getWidth() {
        return physicsObject.getWidth();
    }

    public void update(float dt) {
        physicsObject.update(dt);
    }

    public Body getBody(){ return physicsObject.getBody(); }

    public void deactivatePhysics(World world) {
        physicsObject.deactivatePhysics(world);
    }

    public void activatePhysics(World world) {
        physicsObject.activatePhysics(world);
        physicsObject.getBody().setUserData(this);
    }


    /** Returns true if this object is destroyed. */
    public boolean isDestroyed() {
        return physicsObject.isRemoved();
    }

    /**
     * Sets whether this object is destroyed.
     * @param value whether this object is destroyed
     */
    public void setDestroyed(boolean value) {
        physicsObject.markRemoved(value);
    }

    /**
     * Draws the texture physics object.
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        draw(canvas, Color.WHITE);
    }

    /**
     * Draws the texture physics object.
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas, Color color) {
        if (texture != null) {
            canvas.draw(texture, color, origin.x, origin.y, getX() + textureOffset.x, getY() + textureOffset.y, getAngle(), textureScale.x, textureScale.y);
        }
    }

    /**
     * Drawing an additional texture for a GameObject.
     * @param canvas drawing context
     * @param holder the additional texture
     */
    public void draw(GameCanvas canvas, TextureHolder holder) {
        if (holder.getTexture() != null) {
            canvas.draw(holder.getTexture(), holder.getColor(), holder.getOrigin().x, holder.getOrigin().y,
                    getX() + holder.getTextureOffset().x, getY() + holder.getTextureOffset().y,
                    getAngle(), holder.getTextureScale().x, holder.getTextureScale().y);
        }
    }

    public void drawDebug(GameCanvas canvas) {
        physicsObject.drawDebug(canvas);
    }

    // Texture Information

	public TextureRegion getTexture() {
		return texture;
	}

	public void setTexture(TextureRegion value) {
        texture = value;
        origin.set(texture.getRegionWidth()/2.0f, texture.getRegionHeight()/2.0f);
        setTextureTransform();
	}

    public void setTexture(TextureRegion value, float scale) {
        texture = value;
        origin.set(texture.getRegionWidth()/2.0f, texture.getRegionHeight()/2.0f);
        textureScale = new Vector2(scale,scale);
        textureOffset = new Vector2(0,0);
    }



    /**
     * Sets textureScale and textureOffset. May be overridden by subclasses to display textures that aren't
     * stretched to fit the hitbox.
     */
    protected void setTextureTransform() {
        textureScale = new Vector2(getWidth() / texture.getRegionWidth(), getHeight() / texture.getRegionHeight());
//        System.out.println(textureScale);
        textureOffset = new Vector2(0,0);
    }

    // Current physics information

    /** Whether this object is affected by the current */
    public boolean AffectedByCurrent(){
        return getType() == ObjectType.RAFT || getType() == ObjectType.WOOD || getType() == ObjectType.SHARK;
    }

    public boolean posXVel(){ return getLinearVelocity().x > 0; }
    public boolean negXVel(){ return getLinearVelocity().x < 0; }
    public int setTextureXOrientation(boolean reverse){
        if(posXVel()){
            textureScale.x = (reverse ? -1 : 1) * Math.abs(textureScale.x);
            return 1;
        } else if(negXVel()){
            textureScale.x = (reverse ? 1 : -1) * Math.abs(textureScale.x);
            return -1;
        }
        return 1;
    }
}
