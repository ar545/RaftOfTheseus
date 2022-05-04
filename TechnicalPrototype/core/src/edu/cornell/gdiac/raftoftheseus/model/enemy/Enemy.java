package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import edu.cornell.gdiac.raftoftheseus.model.util.FrameCalculator;
import edu.cornell.gdiac.raftoftheseus.model.GameObject;
import edu.cornell.gdiac.raftoftheseus.model.Raft;
import edu.cornell.gdiac.raftoftheseus.model.util.TextureHolder;
import edu.cornell.gdiac.raftoftheseus.model.util.Timer;

public abstract class Enemy<T, K extends State<T>> extends GameObject {

    /** The player */
    protected Raft player;
    private long timeStamp = 0L;
    private boolean timeStamped = false;
    private long attackStamp = 0L;
    private boolean attackStamped = false;

    /** Timers */
    protected Timer stateTimer = new Timer();
    protected Timer attackTimer = new Timer();

    /** For stun animation. */
    protected FrameCalculator stunFC = new FrameCalculator();
    protected TextureHolder stunTexture = new TextureHolder();

    /**
     * Constructor
     * @param player the player for targeting
     */
    public Enemy(Raft player){ this.player = player; }

    /**
     * Method to switch the state of the FSM when applicable.
     * @param dt the time increment
     */
    public abstract void updateAI(float dt);
    /** @return whether the player is in a given range of this Shark. */
    public boolean inRange(float dist){ return getTargetDistance() < dist; }
    /** @return how far this enemy is from the player. */
    public float getTargetDistance() { return player.getPosition().cpy().sub(getPosition()).len(); }
    /** @param playerCurrentVelocity how fast the player is moving on a current. */
    public abstract Vector2 getTargetDirection(Vector2 playerCurrentVelocity);

    /** Return the FSM of this enemy. */
    public abstract StateMachine<T, K> getStateMachine();
    /** Method to call when the enemy is hit by the player spear. */
    public abstract boolean setHit();

    /** Method to ensure that each enemy has their stun texture set. */
    public abstract void setStunTexture(TextureRegion value);
}