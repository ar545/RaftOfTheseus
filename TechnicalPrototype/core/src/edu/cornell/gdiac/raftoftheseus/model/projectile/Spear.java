package edu.cornell.gdiac.raftoftheseus.model.projectile;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.util.Animated;
import edu.cornell.gdiac.raftoftheseus.model.util.FrameCalculator;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class Spear extends Projectile implements Animated {

    /**
     * @param objParams the "spear" child in "object_settings.json"
     */
    public static void setConstants(JsonValue objParams){
        SPEED = objParams.getFloat("speed scale");
        DAMAGE = objParams.getFloat("health cost");
        TEXTURE_SCALE = objParams.getFloat("texture scale");
        TEXTURE_XO = objParams.getFloat("texture x offset");
        TEXTURE_YO = objParams.getFloat("texture y offset");
        LENGTH = objParams.getFloat("body length");
        WIDTH = objParams.getFloat("body width");
        SPEAR_XO = objParams.getFloat("raft x offset");
        SPEAR_YO = objParams.getFloat("raft y offset");
        ANGLE = objParams.getFloat("angle");
        RANGE_FLY = objParams.getInt("range fly");
        RANGE_FALL = objParams.getInt("range fall");
        FLOAT_RANGE = objParams.getFloat("float range");
        FLOAT_SPEED = objParams.getFloat("float speed");
        IDLE_AS = objParams.getFloat("idle as");
        IDLE_SF = objParams.getInt("idle sf");
        IDLE_FC = objParams.getInt("idle fc");
        FIRED_AS = objParams.getFloat("fired as");
        FIRED_SF = objParams.getInt("fired sf");
        FIRED_FC = objParams.getInt("fired fc");
        DEST_AS = objParams.getFloat("dest as");
        DEST_SF = objParams.getInt("dest sf");
        DEST_FC = objParams.getInt("dest fc");
    }

    // SPEAR
    public static float SPEED;
    /** Health cost for creating a spear. */
    public static float DAMAGE;
    /** Size of a spear. */
    public static float TEXTURE_SCALE;
    private static float TEXTURE_XO;
    private static float TEXTURE_YO;
    private static float LENGTH;
    private static float WIDTH;
    /** Range of a spear. */
    private static int RANGE_FLY;
    private static int RANGE_FALL;
    private static float SPEAR_XO;
    private static float SPEAR_YO;
    private static float ANGLE;
    private static float FLOAT_RANGE;
    private static float FLOAT_SPEED;

    // Animation
    private static float IDLE_AS;
    private static int IDLE_SF;
    private static int IDLE_FC;
    private static float FIRED_AS;
    private static int FIRED_SF;
    private static int FIRED_FC;
    private static float DEST_AS;
    private static int DEST_SF;
    private static int DEST_FC;
    private FrameCalculator fc = new FrameCalculator();

    /** Spear state. */
    private enum SpearState{
        IDLE,
        FIRED,
        DESTROYED
    }
    private SpearState spearState = SpearState.IDLE;
    private boolean toDestroy;

    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    @Override
    public ObjectType getType() { return ObjectType.SPEAR; }

    public static float getSpearRange() {
        return (float)RANGE_FALL;
    }

    /**
     * Constructor for the Spear.
     */
    public Spear(Vector2 pos) {
        // Does not initially interact with anything.
        physicsObject = new BoxObstacle(WIDTH, LENGTH);
        physicsObject.getFilterData().categoryBits = CATEGORY_PLAYER_BULLET;
        physicsObject.getFilterData().maskBits = 0;
        setPosition(pos.add(SPEAR_XO, SPEAR_YO));
        setAngle(ANGLE);
    }

    // STATE CHANGES

    /**
     * Detach the spear from the raft
     * @param dir which way the spear will head
     * @param raft_speed the rafts current speed
     */
    public void fire(Vector2 dir, Vector2 raft_speed){
        spearState = SpearState.FIRED;
        Filter f = physicsObject.getFilterData();
        f.maskBits = MASK_PLAYER_BULLET;
        physicsObject.setFilterData(f);
        setAngle(dir.angleDeg()-90f);
        setBody(dir.scl(SPEED).mulAdd(raft_speed, 0.5f));
        fc.resetAll();
    }

    /** @return whether spear is fire state or not. */
    private boolean isFired(){ return spearState == SpearState.FIRED; }
    /** Applying drag to slow the projectile down. Depends on how far the spear has traveled. */
    @Override
    public void update(float delta) {
        if(isFired()) super.update(delta, RANGE_FLY);
    }
    /** @return whether this spear is set to be destroyed. */
    public boolean outMaxDistance(){ return isFired() && outMaxDistance(RANGE_FALL) && isFired(); }
    /** Deactivate the interactions of this spear before destruction. */
    public void deactivate(){
        getBody().setLinearVelocity(0, 0);
        Filter f = physicsObject.getFilterData();
        f.categoryBits = 0;
        f.maskBits = 0;
        spearState = SpearState.DESTROYED;
        fc.resetAll();
    }
    /** @return whether the death animation of the spear has finished. */
    public boolean isToDestroy(){ return toDestroy && spearState == SpearState.DESTROYED; }

    // ANIMATION

    @Override
    public void setTexture(TextureRegion value){
        texture = value;
        origin.set(texture.getRegionWidth()/2.0f + TEXTURE_XO, texture.getRegionHeight()/2.0f + TEXTURE_YO);
        setTextureTransform();
    }

    /** Set Spear to stretch slightly larger than its hitbox. */
    @Override
    protected void setTextureTransform() {
        float h = TEXTURE_SCALE / texture.getRegionHeight();
        textureScale = new Vector2(h, h);
        textureOffset = new Vector2(0, 0);
    }

    // TODO Make spear move slowly when player turns direction

    /**
     * Change the position of this spear relative to the raft.
     * @param pos the raft position
     */
    public void setFloatPosition(Vector2 pos, float floatTime, float flip, Vector2 dir){
        float y_float = (float) Math.sin(floatTime*FLOAT_SPEED) * FLOAT_RANGE;
        pos.add(SPEAR_XO, SPEAR_YO + y_float);
        setPosition(pos);
        setAngle(dir.sub(getPosition()).rotateDeg(-90f).angleDeg());
    }

    @Override
    public FrameCalculator getFrameCalculator(){ return fc; }
    @Override
    public void setAnimationFrame(float dt) {
        fc.addTime(dt);
        switch (spearState){
            case IDLE:
                fc.setFrame(IDLE_AS, IDLE_SF, IDLE_FC, false);
                break;
            case FIRED:
                fc.setFrame(FIRED_AS, FIRED_SF, FIRED_FC, false);
                break;
            case DESTROYED:
                if(fc.isFrame(DEST_SF, DEST_FC, false)){
                    toDestroy = true;
                    return;
                }
                fc.setFrame(DEST_AS, DEST_SF, DEST_FC, false);
                break;
        }
    }

    /**
     * Set the appropriate image frame first before drawing the Siren.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas){
        ((FilmStrip) texture).setFrame(fc.getFrame());
        super.draw(canvas);
    }
}
