package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.utils.JsonValue;

import static edu.cornell.gdiac.raftoftheseus.Hydra.EnemyState.*;

public class HydraController {

    private int id;
    private Hydra hydra;
    private Raft raft;
    private Hydra.EnemyState state;
    /**
     * The number of ticks since we started this controller
     */
    private long ticks;

    public HydraController(int id, Hydra hydra, Raft raft) {
        this.id = id;
        this.hydra = hydra;
        this.raft = raft;
        state = SPAWN;
        ticks = 0;
    }

    public long getTicks(){
        return ticks;
    }

    public Hydra.EnemyState getAction() {
        // Increment the number of ticks.
        ticks++;

        // Do not need to rework ourselves every frame. Just every 10 ticks.
        if ((id + ticks) % 10 == 0) {
            changeStateIfApplicable();
        }

        return state;
    }

    public Hydra.EnemyState getState(){
        return state;
    }


    private void changeStateIfApplicable() {
//        System.out.println(dist());
        switch (state) {
            case SPAWN:
                state = IDLE;
                break;
            case IDLE:
                if (hydra.dist() <= RANGE && hydra.canSee())
                    state = ACTIVE;
                break;
            case ACTIVE:
                if (hydra.isHit()){
                    hydra.setHit(false);
                    state = STUNNED;
                }
                if (hydra.dist() > RANGE || !hydra.canSee())
                    state = IDLE;
                else if(hydra.canFire()){
                    state = SPLASHING;
                }
                break;
            case STUNNED:
            default:
                // illegal state
                assert (false);
                state = IDLE;
                break;
        }
    }

    public boolean isAlive() {
        return (hydra != null && !hydra.isDestroyed());
    }
}
