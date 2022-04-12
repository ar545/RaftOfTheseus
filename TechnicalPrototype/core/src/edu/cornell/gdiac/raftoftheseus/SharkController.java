package edu.cornell.gdiac.raftoftheseus;


import java.util.Random;
import com.badlogic.gdx.utils.JsonValue;
import static edu.cornell.gdiac.raftoftheseus.Shark.enemyState.*;



public class SharkController {

    Random rand = new Random();
    /**
     * How close a target must be for us to chase it
     */
    private static int CHASE_DIST = 12;

    /**
     * How close a target must be for us to chase it while enraged
     */
    private static final int ENRAGE_CHASE_DIST = 18;

    /**
     * How many ticks to enrage for
     */
    private static final int ENRAGE_DURATION = 5 * 30;

    private int id;

    private Shark shark;

    private Raft raft;

    private Shark.enemyState state;
    /**
     * The number of ticks since we started this controller
     */
    private long ticks;
    /**
     * The number of ticks when we last entered the enraged state
     */
    private long enrage_timestamp;

    // Set class constants
    public static void setConstants(JsonValue objParams){
        CHASE_DIST = objParams.getInt("chase distance", 12);
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

    private void enrage(){
            enrage_timestamp = ticks;
            shark.setEnraged(true);
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
                    shark.setEnraged(false);
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
        return (shark != null && !shark.isDestroyed());
    }
}
