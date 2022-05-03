package edu.cornell.gdiac.raftoftheseus.model.util;

public class FrameCalculator {

    /** Incrementer for progressing through each frame. */
    private int increment = 0;
    /** Time elapsed to keep track when to increment. */
    private float timeElapsed = 0;
    /** Which frame to use in the filmstrip. */
    private int frame = 0;
    /** Whether to draw something as a different color. */
    boolean flash = false;

    /** Default constructor */
    public FrameCalculator(){}

    /** Constructor when the starting frame is different from 0.
     * @param startFrame the new starting frame.
     */
    public FrameCalculator(int startFrame){
        frame = startFrame;
    }

    /**@return the current frame calculated. */
    public int getFrame(){ return frame; }
    /**Set the starting frame of this calculator for checking purposes. */
    public void setFrame(int frame){ this.frame = frame; }
    /**Reset the increment to 0. */
    public void resetIncrement(){ increment = 0; }
    /**The amount of time that has elapsed. */
    public float getTimeElapsed() { return timeElapsed; }
    /**Reset the time elapsed to 0.*/
    public void resetTimeElapsed(){ timeElapsed = 0; }
    /**@param dt the time change in seconds to be added. */
    public void addTime(float dt){ timeElapsed += dt; }
    /** @return whether this animation should flash */
    public boolean getFlash(){ return flash; }
    /** @param flash whether this animation should be flashing or not. */
    public void setFlash(boolean flash){ this.flash = flash; }
    /** Method to reset timeelapsed and increment. */
    public void resetAll(){
        resetIncrement();
        resetTimeElapsed();
    }

    /**
     * Sets the frame of the animation based on the FSM and time given.
     * @param animationSpeed how many seconds should pass between each frame.
     * @param start which frame in the FilmStrip the animation starts on.
     * @param frames the number of frames this animation has.
     * @param reverse whether the animation should be drawn backwards.
     * @return whether it has reached the last animation image.
     */
    public boolean setFrame(float animationSpeed, int start, int frames, boolean reverse){
        if (timeElapsed > animationSpeed){
            resetTimeElapsed();
            increment += 1;
            frame = start + (reverse ? (frames - 1) - increment % frames : increment % frames);
        }
        return reverse ? frame == start : frame == frames - 1 + start;
    }

    /**
     * Checks whether the current frame is the starting or ending frame.
     * @param start the starting index.
     * @param frames the amount of frames for the given animation.
     * @param beginning whether to check for the starting or ending index.
     * @return whether the current frame is the start or end frame.
     */
    public boolean isFrame(int start, int frames, boolean beginning){
        return beginning ? frame == start : frame == frames - 1 + start;
    }

    /**
     * Method to check whether enough time has elapsed to change color.
     * @param flashingSpeed how much time should elapse before changing color.
     */
    public void checkFlash(float flashingSpeed){
        if (timeElapsed > flashingSpeed){
            flash = !flash;
            resetTimeElapsed();
        }
    }
}
