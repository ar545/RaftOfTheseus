package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class Treasure extends GameObject {

    // ATTRIBUTES
    /** Has the treasure been collected yet? */
    protected boolean collected;

    private static int OPEN_SF = 0;

    private static int OPEN_FC = 7;

    private static float OPEN_AS = 0.05f;

    private static float OPEN_RANGE = 6.0f;

    private boolean animated;

    private Raft raft;

    /** Frame calculator for animation. */
    private FrameCalculator fc = new FrameCalculator(OPEN_SF);

    public Treasure(Vector2 position, Raft raft) {
        physicsObject = new WheelObstacle(0.9f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_NON_PUSHABLE;
        physicsObject.getFilterData().maskBits = MASK_TREASURE;
        this.raft = raft;
        animated = false;

        collected = false;
    }

    public ObjectType getType() {
        return ObjectType.TREASURE;
    }

    public boolean getCollected() { return collected; }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public void update(float dt) {
        super.update(dt);
        if (collected && animated) {
            this.setDestroyed(true);
        }
    }

//    @Override
//    protected void setTextureTransform() {
//        float w = getWidth() / texture.getRegionWidth() * TEXTURE_SCALE;
//        textureScale = new Vector2(w, w);
//        textureOffset = new Vector2(0, 0);
//    }


    public void setAnimationFrame(float dt) {

        if(collected) {
            fc.addTime(dt);
            fc.setFrame(OPEN_AS, OPEN_SF, OPEN_FC, false);
            if(fc.getFrame() == OPEN_FC - 1){
                animated=true;
            }
        }
    }

    @Override
    public void draw(GameCanvas canvas){
        ((FilmStrip) texture).setFrame(fc.getFrame());
        super.draw(canvas);
    }
}
