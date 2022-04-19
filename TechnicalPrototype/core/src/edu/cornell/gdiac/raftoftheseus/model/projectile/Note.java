package edu.cornell.gdiac.raftoftheseus.model.projectile;

import com.badlogic.gdx.math.Vector2;

public class Note extends Projectile {

    private Vector2 direction;

    private static float FORCE;

    public Note(Vector2 pos, Vector2 dir, String s) {
        super(pos, s);
    }

    public static float getFORCE() { return FORCE; }
}
