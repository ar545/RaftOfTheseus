package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

public enum SirenState implements State<Siren> {

    IDLE() {
        @Override
        public void update (Siren entity){
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(Siren.IDLE_TIME)) {
                entity.getStateMachine().changeState(SINGING);
            }
        }
    },
    SINGING() {
        @Override
        public void update (Siren entity){
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(Siren.SINGING_TIME)) {
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
                entity.stopMove();
                entity.getStateMachine().changeState(LANDING);
            }
        }
    },
    STUNNED(){
        @Override
        public void update(Siren entity) {
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(Siren.STUN_TIME)){
                entity.getFrameCalculator().setFlash(false);
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
        entity.getFrameCalculator().resetIncrement();
        entity.getFrameCalculator().resetTimeElapsed();
    }

    @Override
    public boolean onMessage(Siren entity, Telegram telegram) {
        return false;
    }
}
