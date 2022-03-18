package edu.cornell.gdiac.raftoftheseus;


import static edu.cornell.gdiac.raftoftheseus.Enemy.enemyState.*;

public class AIController {
    /**
     * How close a target must be for us to chase it
     */
    private static final int CHASE_DIST = 200;

    private int id;

    private Enemy enemy;

    private Raft raft;

    private Enemy.enemyState state;
    /**
     * The number of ticks since we started this controller
     */
    private long ticks;

    public AIController(int id, Enemy enemy, Raft raft) {
        this.id = id;
        this.enemy = enemy;
        this.raft = raft;
        state = SPAWN;
        ticks = 0;
    }

    public long getTicks(){
        return ticks;
    }

    public Enemy.enemyState getAction() {
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

    private double dist(){
        return Math.sqrt(Math.pow(enemy.getX() - raft.getX(), 2) + Math.pow(enemy.getY() - raft.getY(), 2));
    }


    private void changeStateIfApplicable() {
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
}
