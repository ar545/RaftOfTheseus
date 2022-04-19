package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
public enum HydraState implements State<Hydra> {

    IDLE(){
        @Override
        public void update(Hydra entity) {
            if (entity.inRange() && entity.canSee())
                entity.getStateMachine().changeState(ACTIVE);
            checkStun(entity);
        }
    },
    ACTIVE(){
        @Override
        public void update(Hydra entity) {
            entity.setTimeStamp();
            if (checkStun(entity)){
                return;
            }
            else if (!entity.inRange() || !entity.canSee())
                entity.getStateMachine().changeState(IDLE);
            else if(entity.canFire()){
                entity.resetTimeStamp();
                entity.getStateMachine().changeState(PRIMING);
            } else if(entity.canFire())
                entity.getStateMachine().changeState(PRIMING);
            {}
        }
    },
    PRIMING(){
        @Override
        public void update(Hydra entity) {
            if (checkStun(entity)){
                return;
            }
            if(entity.inAttackRange()){
                entity.getStateMachine().changeState(HITTING);
            } else if(entity.canFire()){
                entity.getStateMachine().changeState(SPLASHING);
            }
        }
    },
    SPLASHING(){
        @Override
        public void update(Hydra entity) {
            if (checkStun(entity)){
                return;
            } else if (entity.hasFired()){
                entity.resetHasFired();
                entity.resetTimeStamp();
                entity.getStateMachine().changeState(ACTIVE);
            }
        }
    },
    HITTING(){
        @Override
        public void update(Hydra entity) {
            if (checkStun(entity)){
                return;
            }
            if(entity.hasAttacked()){
                entity.resetHasAttacked();
                entity.getStateMachine().changeState(ACTIVE);
            }
        }
    },
    STUNNED(){
        @Override
        public void update(Hydra entity) {
            entity.setTimeStamp();
            if(entity.stunElapsed()){
                entity.resetTimeStamp();
                entity.getStateMachine().changeState(ACTIVE);
            }
        }
    };
    
    private static boolean checkStun(Hydra entity){
        if (entity.isHit()){
            entity.setHit(false);
            entity.getStateMachine().changeState(STUNNED);
            return true;
        }
        return false;
    }


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
