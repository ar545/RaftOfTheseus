package edu.cornell.gdiac.raftoftheseus;


import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

import static edu.cornell.gdiac.raftoftheseus.Enemy.enemyState.*;

public class AIController {
    /**
     * How close a target must be for us to chase it
     */
    private static int CHASE_DIST = 12;

    private int id;

    private Enemy enemy;

    private Raft raft;

    private Enemy.enemyState state;
    /**
     * The number of ticks since we started this controller
     */
    private long ticks;

    // Set class constants
    public static void setConstants(JsonValue objParams){
        CHASE_DIST = objParams.getChild("shark ai").getInt("chase distance", 12);
    }

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

    public Enemy.enemyState getState(){
        return state;
    }

    private float dist(){
        return enemy.getPosition().dst(raft.getPosition());
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
        return (enemy != null && !enemy.isDestroyed());
    }
}
