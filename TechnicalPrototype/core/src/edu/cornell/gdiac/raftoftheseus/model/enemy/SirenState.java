package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

public enum SirenState implements State<Siren> {

    IDLE() {
        @Override
        public void update (Siren entity){
            entity.setTimeStamp();
            if(entity.isPastIdleCooldown()) {
                entity.getStateMachine().changeState(SINGING);
            }
        }
    },
    SINGING() {
        @Override
        public void update (Siren entity){
            entity.setTimeStamp();
            if(entity.isDoneSinging()) {
                entity.resetAttackStamp();
                entity.resetTimeStamp();
                if(entity.isStationary()) entity.getStateMachine().changeState(IDLE);
                else {
                    entity.setMoveVector();
                    entity.scaleMoveVector(false);
                    entity.getStateMachine().changeState(TAKEOFF);
                }
            }
        }
    },
    LANDING(){
        @Override
        public void update (Siren entity){
            if(entity.isAnimationDone()) {
                entity.getStateMachine().changeState(IDLE);
            }
        }
    },
    TAKEOFF(){
        @Override
        public void update (Siren entity){
            if(entity.isAnimationDone()) {
                entity.scaleMoveVector(true);
                entity.getStateMachine().changeState(FLYING);
            }
        }
    },
    FLYING() {
        @Override
        public void update (Siren entity){
            if(entity.nearLanding()){
                entity.incrementWaypoint();
                entity.getStateMachine().changeState(LANDING);
            }
        }

        @Override
        public void exit (Siren entity){
            entity.stopMove();
            entity.resetFrame();
        }
    },
    STUNNED(){
        @Override
        public void update(Siren entity) {
            entity.setTimeStamp();
            if(entity.stunElapsed()){
                entity.unsetFlash();
                entity.resetTimeStamp();
                entity.getStateMachine().changeState(SINGING);
            }
        }
    };

    @Override
    public void enter(Siren entity) {

    }

    @Override
    public void exit(Siren entity) {
        entity.resetTimeStamp();
        entity.resetFrame();
    }

    @Override
    public boolean onMessage(Siren entity, Telegram telegram) {
        return false;
    }
}
