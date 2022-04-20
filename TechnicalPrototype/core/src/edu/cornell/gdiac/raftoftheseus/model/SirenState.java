package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

public enum SirenState implements State<Siren> {

    IDLE() {
        @Override
        public void update (Siren entity){
            entity.setTimeStamp();
            if(entity.isPastIdleCooldown()) {
                entity.getStateMachine().changeState(SINGING);
                entity.resetTimeStamp();
            }
        }
    },
    LANDING(){
        @Override
        public void update (Siren entity){
            entity.getStateMachine().changeState(IDLE);
            entity.stopMove();
        }
    },
    TAKEOFF(){
        @Override
        public void update (Siren entity){
            changeToFlying(entity);
        }
    },
    FLYING() {
        @Override
        public void update (Siren entity){
            if(entity.nearLanding()){
                entity.getStateMachine().changeState(LANDING);
            }
            else if(entity.fromFirstLocation()){
                entity.setMoveVector(true);
            } else if(entity.fromSecondLocation()){
                entity.setMoveVector(false);
            }
        }
    },
    SINGING() {
        @Override
        public void update (Siren entity){
            if(entity.inAttackRange()){
                entity.getStateMachine().changeState(ATTACKING);
            } else {
                entity.setTimeStamp();
                if(entity.isDoneSinging()) {
                    entity.getStateMachine().changeState(TAKEOFF);
                }
            }
        }
    },
    ATTACKING() {
        @Override
        public void update (Siren entity){
            if(entity.hasAttacked()){
                entity.resetHasAttacked();
                SirenState.changeToFlying(entity);
            }
        }
    };

    private static void changeToFlying(Siren entity){
        if(entity.fromFirstLocation()){
            entity.setFromSecondLocation();
        } else {
            entity.setFromFirstLocation();
        }
        entity.getStateMachine().changeState(FLYING);
        entity.resetTimeStamp();
    }

    @Override
    public void enter(Siren entity) {

    }

    @Override
    public void exit(Siren entity) {

    }

    @Override
    public boolean onMessage(Siren entity, Telegram telegram) {
        return false;
    }
}