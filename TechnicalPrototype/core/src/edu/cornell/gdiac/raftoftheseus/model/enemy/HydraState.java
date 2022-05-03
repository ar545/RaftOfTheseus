package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
public enum HydraState implements State<Hydra> {

    IDLE(){
        @Override
        public void update(Hydra entity) {
            if (entity.inRange(Hydra.FIRING_RANGE) && entity.canSee()) entity.getStateMachine().changeState(ACTIVE);
        }
    },
    ACTIVE(){
        @Override
        public void update(Hydra entity) {
            entity.setTimeStamp();
            if (!entity.inRange(Hydra.FIRING_RANGE) || !entity.canSee()) entity.getStateMachine().changeState(IDLE);
//            else if(entity.canFire()){
//                entity.resetTimeStamp();
//                entity.getStateMachine().changeState(PRIMING);
//            } else if(entity.canFire())  entity.getStateMachine().changeState(PRIMING);
        }
    },
    PRIMING(){
        @Override
        public void update(Hydra entity) {
            if(entity.canFire()) entity.getStateMachine().changeState(SPLASHING);
        }
    },
    SPLASHING(){
        @Override
        public void update(Hydra entity) {
            if (entity.hasFired()){
                entity.resetHasFired();
                entity.resetTimeStamp();
                entity.getStateMachine().changeState(ACTIVE);
            }
        }
    },
    STUNNED(){
        @Override
        public void update(Hydra entity) {
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(Hydra.STUN_TIME)){
                entity.resetTimeStamp();
                entity.getStateMachine().changeState(ACTIVE);
            }
        }
    };


    @Override
    public void enter(Hydra entity) {

    }

    @Override
    public void exit(Hydra entity) {

    }

    @Override
    public boolean onMessage(Hydra entity, Telegram telegram) {
        return false;
    }
}
