package edu.cornell.gdiac.raftoftheseus.model.util;

import com.badlogic.gdx.utils.TimeUtils;

/**
 * Class to help factor out timestamp related code for duration or state dependent events.
 */
public class Timer {

    /** The time when the Timer was called to stamp. */
    private long timeStamp = 0L;
    /** Whether the time has been stamped for this timer. */
    private boolean timeStamped = false;
    /** Whether this time can fire again. */
    private boolean canFire = true;

    /** Start a timestamp and return true. */
    public boolean fireTimeStamp(){
        if(canTimeStamp() && canFire){
            setTimeStamp();
            canFire = false;
            return true;
        }
        return false;
    }

    /** Set a timestamp if one has not already been set. */
    public void setTimeStamp(){
        if(!timeStamped) {
            timeStamp = TimeUtils.millis();
            timeStamped = true;
        }
    }

    /** @return whether this timer can set a new timestamp for condition checking. */
    public boolean canTimeStamp(){ return !timeStamped; }
    /** Resets this timer to allow a new time stamp. */
    public void resetTimeStamp(){ timeStamped = false; }
    /** Reset this timer so it can fire again. */
    public void resetCanFire(){ canFire = true; }

    /**
     * @param reset whether to reset this timeStamp if the allotted time has passed.
     * @param time the amount of time required to pass in seconds.
     * @return whether the given period of time has elapsed since the last call to resetTimeStamp.
     */
    public boolean hasTimeElapsed(float time, boolean reset){ return hasTimeElapsed((long) (time * 1000), reset); }

    /**
     * @param reset whether to reset this timeStamp if the allotted time has passed.
     * @param time the amount of time required to pass in milliseconds
     * @return whether the given period of time has elapsed since the last call to resetTimeStamp.
     */
    public boolean hasTimeElapsed(long time, boolean reset){
        boolean passed = timeStamped && TimeUtils.timeSinceMillis(timeStamp) > time;
        if(passed){
            if(reset) resetTimeStamp();
            return true;
        }
        return false;
    }
}
