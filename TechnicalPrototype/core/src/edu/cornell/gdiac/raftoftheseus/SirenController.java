package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.utils.JsonValue;

import static edu.cornell.gdiac.raftoftheseus.Hydra.EnemyState.SPAWN;

public enum SirenController implements State<Siren> {

    SPAWN() {
        @Override
        public void update (Siren entity){

        }
    },
    SINGING() {@Override
    public void update (Siren entity){

    }},
    FLYING() {@Override
    public void update (Siren entity){

    }},
    IDLE() {@Override
    public void update (Siren entity){

    }},
    ATTACKING() {@Override
    public void update (Siren entity){

    }};

    @Override
    public void enter(Siren entity) {

    }

    @Override
    public void update(Siren entity) {

    }

    @Override
    public void exit(Siren entity) {

    }

    @Override
    public boolean onMessage(Siren entity, Telegram telegram) {
        return false;
    }
}
