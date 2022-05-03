package edu.cornell.gdiac.raftoftheseus.model;

public interface Animated {

    /** Method to set the animation frame of an object before drawing based on time elapsed.
     * @param dt the amount of time since the last game loop.
     */
    public void setAnimationFrame(float dt);

    /**
     * @return this animated object's framecalculator
     */
    public FrameCalculator getFrameCalculator();
}
