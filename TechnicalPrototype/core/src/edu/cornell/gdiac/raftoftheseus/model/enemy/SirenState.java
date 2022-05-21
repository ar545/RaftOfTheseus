package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

public enum SirenState implements State<Siren> {

    IDLE() {
        @Override
        public void enter(Siren entity) {
            entity.setStationaryMask();
        }

        @Override
        public void update (Siren entity){
            entity.stateTimer.setTimeStamp();
            if(entity.stateTimer.hasTimeElapsed(Siren.IDLE_TIME, true)) {
                entity.getStateMachine().changeState(SINGING);
            }
        }
    },
    SINGING() {
        @Override
        public void update (Siren entity){
            entity.stateTimer.setTimeStamp();
            if(entity.stateTimer.hasTimeElapsed(Siren.SINGING_TIME, true)) {
                entity.attackTimer.resetTimeStamp();
                entity.attackTimer.setTimeStamp();
                if(entity.isStationary()) entity.getStateMachine().changeState(IDLE);
                else {

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
        public void enter(Siren entity) {
            entity.setFlyingMask();
            entity.setMoveVector();
            entity.scaleMoveVector(false);
        }

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
            entity.stateTimer.setTimeStamp();
            if(entity.stateTimer.hasTimeElapsed(Siren.STUN_TIME, true)){
                entity.getFrameCalculator().setFlash(false);
                entity.getStateMachine().changeState(TAKEOFF);
            }
        }
    };

    @Override
    public void enter(Siren entity) {}

    @Override
    public void exit(Siren entity) {
        entity.stateTimer.resetTimeStamp();
        entity.getFrameCalculator().resetIncrement();
        entity.getFrameCalculator().resetTimeElapsed();
    }

    @Override
    public boolean onMessage(Siren entity, Telegram telegram) {
        return false;
    }
}
