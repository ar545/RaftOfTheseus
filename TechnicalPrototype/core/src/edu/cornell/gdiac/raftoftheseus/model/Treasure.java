package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.util.FrameCalculator;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class Treasure extends GameObject {

    private static final float STARBURST_OFFSET = 4.5f;

    private class Starburst extends GameObject{
        public final int SB_SF = 0;

        public final int SB_FC = 7;

        public final float SB_AS = 0.15f;

        public FrameCalculator fc;

        public Starburst() {
            fc = new FrameCalculator(SB_SF);
        }

        @Override
        public ObjectType getType() {
            return null;
        }

//        canvas.draw(fuelTexture, player_position.x - 50, player_position.y);
    }

    // ATTRIBUTES
    /** Has the treasure been collected yet? */
    protected boolean collected;

    private static int OPEN_SF = 0;

    private static int OPEN_FC = 7;

    private static float OPEN_AS = 0.05f;

    private boolean animated;

    private Starburst sb;

    private Raft raft;

    /** Frame calculator for animation. */
    private FrameCalculator fc = new FrameCalculator(OPEN_SF);

    public Treasure(Vector2 position, Raft raft) {

        physicsObject = new WheelObstacle(0.9f);
        setPosition(position);
        physicsObject.setBodyType(BodyDef.BodyType.StaticBody);
        physicsObject.getFilterData().categoryBits = CATEGORY_NON_PUSHABLE;
        physicsObject.getFilterData().maskBits = MASK_TREASURE;
        animated = false;
        this.raft = raft;

        collected = false;
    }

    public ObjectType getType() {
        return ObjectType.TREASURE;
    }

    public boolean getCollected() { return collected; }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public void initSB(FilmStrip t){
        sb = new Starburst();
        sb.setTexture(t, 3f/t.getRegionWidth());
    }

    public void update(float dt) {
        super.update(dt);
        if (collected && animated) {
            this.setDestroyed(true);
        }
    }

    @Override
    protected void setTextureTransform() {
        float w = getWidth() / texture.getRegionWidth() * 1.5f;
        textureScale = new Vector2(w, w);
        textureOffset = new Vector2(0, 0);
    }


    public void setAnimationFrame(float dt) {
        if(collected) {
            if (fc.getFrame() < OPEN_FC - 1) {
                fc.addTime(dt);
                fc.setFrame(OPEN_AS, OPEN_SF, OPEN_FC, false);
            }
            sb.fc.addTime(dt);
            sb.fc.setFrame(sb.SB_AS, sb.SB_SF, sb.SB_FC, false);
            if(sb.fc.getFrame() == sb.SB_FC - 1){
                animated=true;
            }
        }
    }

    @Override
    public void draw(GameCanvas canvas){
        ((FilmStrip) texture).setFrame(fc.getFrame());
        ((FilmStrip) sb.texture).setFrame(sb.fc.getFrame());
        super.draw(canvas);
        if (collected) {
            if(sb != null && raft != null) canvas.draw(sb.texture, Color.WHITE, sb.origin.x, sb.origin.y, raft.getX() + sb.textureOffset.x, raft.getY() + sb.textureOffset.y + STARBURST_OFFSET, getAngle(), sb.textureScale.x, sb.textureScale.y);
        }
    }

}
