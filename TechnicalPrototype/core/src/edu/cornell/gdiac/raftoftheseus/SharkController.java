package edu.cornell.gdiac.raftoftheseus;


import com.badlogic.gdx.utils.JsonValue;

import static edu.cornell.gdiac.raftoftheseus.Shark.enemyState.*;

public class SharkController {
    /**
     * How close a target must be for us to chase it
     */
    private static int CHASE_DIST = 12;

    private int id;

    private Shark shark;

    private Raft raft;

    private Shark.enemyState state;
    /**
     * The number of ticks since we started this controller
     */
    private long ticks;

    // Set class constants
    public static void setConstants(JsonValue objParams){
        CHASE_DIST = objParams.getChild("shark ai").getInt("chase distance", 12);
    }

    public SharkController(int id, Shark shark, Raft raft) {
        this.id = id;
        this.shark = shark;
        this.raft = raft;
        state = SPAWN;
        ticks = 0;
    }

    public long getTicks(){
        return ticks;
    }

    public Shark.enemyState getAction() {
        // Increment the number of ticks.
        ticks++;

        // Do not need to rework ourselves every frame. Just every 10 ticks.
        if ((id + ticks) % 10 == 0) {
            // Process the FSM
            changeStateIfApplicable();
            // Pathfinding
//            markGoalTiles();
//            move = getMoveAlongPathToGoalTile();
        }

        return state;
    }

    public Shark.enemyState getState(){
        return state;
    }

    private float dist(){
        return shark.getPosition().dst(raft.getPosition());
    }


    private void changeStateIfApplicable() {
//        System.out.println(dist());
        switch (state) {
            case SPAWN:
                state = WANDER;
                break;
            case WANDER:
                if (dist() <= CHASE_DIST)
                    state = CHASE;
                break;
            case CHASE:
                if (dist() > CHASE_DIST)
                    state = WANDER;
                break;
            default:
                // illegal state
                assert (false);
                state = WANDER;
                break;
        }
    }

    public boolean isAlive() {
        return (shark != null && !shark.isDestroyed());
    }
}
