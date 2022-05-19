package edu.cornell.gdiac.raftoftheseus.model;

import edu.cornell.gdiac.raftoftheseus.GameCanvas;
import edu.cornell.gdiac.raftoftheseus.model.util.FrameCalculator;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.util.FilmStrip;

/** A plant is a special Stationary that contains animation frame. For now, only plant D has filmstrip. */
public class Plant extends Stationary {

    private static class Daisy{
        final static float animationSpeed = 1f;
        final static int start = 0;
        final static int frames = 16;
        final static boolean reverse = false;
        static void setFrame(FrameCalculator fc){ fc.setFrame(animationSpeed, start, frames, reverse); }
    }

    private static class PlantC{
        final static float animationSpeed = 2f;
        final static int start = 0;
        final static int frames = 8;
        final static boolean reverse = false;
        static void setFrame(FrameCalculator fc){ fc.setFrame(animationSpeed, start, frames, reverse); }
    }

    private final FrameCalculator fc = new FrameCalculator(0);

    public Plant(Vector2 position, StationaryType rt, int terrain) { super(position, rt, terrain); }

    private boolean isAnimated(){
        return terrainType == Stationary.plantD || terrainType == Stationary.plantC || terrainType == Stationary.plantA;
    }

    public void setAnimationFrame(float dt) {
        if(isAnimated()) {
            fc.addTime(dt);
        }
        if(terrainType == Stationary.plantD || terrainType == Stationary.plantA) { Daisy.setFrame(fc); }
        if(terrainType == Stationary.plantC) { PlantC.setFrame(fc); }
    }

    public void draw(GameCanvas canvas){
        if(isAnimated()) { ((FilmStrip) texture).setFrame(fc.getFrame()); }
        super.draw(canvas);
    }
}
