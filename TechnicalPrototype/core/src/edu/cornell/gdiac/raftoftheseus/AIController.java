package edu.cornell.gdiac.raftoftheseus;


import java.util.Random;

import static edu.cornell.gdiac.raftoftheseus.Enemy.enemyState.*;

public class AIController {
    Random rand = new Random();

    /**
     * How close a target must be for us to chase it
     */
    private static final int CHASE_DIST = 12;

    /**
     * How close a target must be for us to chase it while enraged
     */
    private static final int ENRAGE_CHASE_DIST = 18;

    /**
     * How many ticks to enrage for
     */
    private static final int ENRAGE_DURATION = 5 * 30;

    private int id;

    private Enemy enemy;

    private Raft raft;

    private Enemy.enemyState state;
    /**
     * The number of ticks since we started this controller
     */
    private long ticks;
    /**
     * The number of ticks when we last entered the enraged state
     */
    private long enrage_timestamp;

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

    private void enrage(){
            enrage_timestamp = ticks;
            enemy.setEnraged(true);
            state = ENRAGE;
    }


    private void changeStateIfApplicable() {
//        System.out.println(dist());
        int p = rand.nextInt(30000);
        switch (state) {
            case SPAWN:
                state = WANDER;
                break;
            case WANDER:

//                System.out.println("dfsf");
//                System.out.println(ticks);
//
//                System.out.println(p);
//
//                System.out.println(dist() <= ENRAGE_CHASE_DIST);
                if (p <= ticks && dist() <= ENRAGE_CHASE_DIST){
                    enrage();
                }
                else if (dist() <= CHASE_DIST)
                    state = CHASE;
//                System.out.println(state);

                break;
            case CHASE:
                if (p <= ticks && dist() <= ENRAGE_CHASE_DIST){
                    enrage();
                }
                else if (dist() > CHASE_DIST)
                    state = WANDER;
                break;
            case ENRAGE:
                if (ticks >= enrage_timestamp + ENRAGE_DURATION || dist() > ENRAGE_CHASE_DIST){
                    state = WANDER;
                    enemy.setEnraged(false);
                }
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
