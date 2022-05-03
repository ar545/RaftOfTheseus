package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.model.GameObject;
import edu.cornell.gdiac.raftoftheseus.model.Raft;

public abstract class Enemy<T, K extends State<T>> extends GameObject {

    /** The player */
    protected Raft player;
    private long timeStamp = 0L;
    private boolean timeStamped = false;
    private long attackStamp = 0L;
    private boolean attackStamped = false;

    /**
     * Constructor
     * @param player
     */
    public Enemy(Raft player){
        this.player = player;
    }

    /**
     * Method to switch the state of the FSM when applicable.
     * @param dt the time increment
     */
    public abstract void updateAI(float dt);

    /** Set a timestamp if one has not already been set. */
    public void setTimeStamp(){
        if(!timeStamped) {
            timeStamp = TimeUtils.millis();
            timeStamped = true;
        }
    }
    /** Method to allow a new time stamp. */
    public void resetTimeStamp(){ timeStamped = false; }
    /** @return Whether the given period of time (in seconds) has elapsed since the last call to resetTimeStamp. */
    public boolean hasTimeElapsed(float time){ return TimeUtils.timeSinceMillis(timeStamp) > time*1000; }
    /** Method to start recording time between firing */
    public void setAttackStamp(){
        if(!attackStamped){
            attackStamp = TimeUtils.millis();
            attackStamped = true;
        }
    }
    /** Method to allow a new time stamp. */
    public void resetAttackStamp(){ attackStamped = false; }
    /** @return Whether the given period of time (in seconds) has elapsed since the last call to resetTimeStamp. */
    public boolean hasAttackTimeElapsed(float time){ return TimeUtils.timeSinceMillis(attackStamp) > time*1000; }


    /** @return whether the player is in a given range of this Shark. */
    public boolean inRange(float dist){ return getTargetDistance() < dist; }
    /** @return how far this enemy is from the player. */
    public float getTargetDistance() { return player.getPosition().cpy().sub(getPosition()).len(); }
    /** Get how much damage is done to the player.
     * @param playerCurrentVelocity how fast the player is moving on a current. */
    public abstract Vector2 getTargetDirection(Vector2 playerCurrentVelocity);

    /** Return the FSM of this enemy. */
    public abstract StateMachine<T, K> getStateMachine();
    /** Called when the enemy is hit by the player spear. */
    public abstract boolean setHit();
}
