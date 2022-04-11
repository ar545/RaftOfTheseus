package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.utils.JsonValue;

import static edu.cornell.gdiac.raftoftheseus.Hydra.EnemyState.SPAWN;

public class SirenController {

    private static float RANGE;
    private int id;
    private Siren siren;
    private Raft raft;
    private Hydra.EnemyState state;
    /**
     * The number of ticks since we started this controller
     */
    private long ticks;

    // Set class constants
    public static void setConstants(JsonValue objParams){
        RANGE = objParams.getInt("range", 12);
    }

    public SirenController(int id, Siren siren) {
        this.id = id;
        this.siren = siren;
        this.raft = raft;
        state = SPAWN;
        ticks = 0;
    }
}
