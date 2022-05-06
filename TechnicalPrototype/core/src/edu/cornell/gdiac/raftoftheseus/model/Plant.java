package edu.cornell.gdiac.raftoftheseus.model;

import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.util.FrameCalculator;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.util.FilmStrip;

/** A plant is a special Stationary that contains animation frame. For now, only plant D has filmstrip. */
public class Plant extends Stationary {

    private final FrameCalculator fc = new FrameCalculator(0);

    public Plant(Vector2 position, StationaryType rt, int terrain) { super(position, rt, terrain); }

    public void setAnimationFrame(float dt) {
        if(terrainType == Stationary.plantD) {
            fc.addTime(dt);
            fc.setFrame(0.1f, 0, 24, false);
        }
    }

    public void draw(GameCanvas canvas){
        if(terrainType == Stationary.plantD) { ((FilmStrip) texture).setFrame(fc.getFrame()); }
        super.draw(canvas);
    }
}
