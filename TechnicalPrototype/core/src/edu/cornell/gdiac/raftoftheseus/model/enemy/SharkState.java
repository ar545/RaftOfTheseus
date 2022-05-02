package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
public enum SharkState implements State<Shark> {
    IDLE(){
        @Override
        public void update(Shark entity) {
            entity.setDesiredVelocity(0, false);
            if(entity.canSee() && entity.inRange(Shark.APPROACH_RANGE)) {
                entity.getStateMachine().changeState(APPROACH);
            }
        }
    },
    APPROACH(){
        @Override
        public void update(Shark entity) {
            entity.setDesiredVelocity(Shark.APPROACH_SPEED, true);
            if(!(entity.canSee() && entity.inRange(Shark.APPROACH_RANGE))) {
                entity.getStateMachine().changeState(IDLE);
            } else {
                if (entity.inRange(Shark.ATTACK_RANGE)) {
                    entity.getStateMachine().changeState(PAUSE_BEFORE_ATTACK);
                }
            }
        }
    },
    PAUSE_BEFORE_ATTACK(){
        @Override
        public void update(Shark entity) {
            entity.setDesiredVelocity(0, true);
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(Shark.ATTACK_WINDUP_TIME)){
                entity.getStateMachine().changeState(ATTACK);
            }
        }
    },
    ATTACK(){
        @Override
        public void update(Shark entity) {
            entity.setDesiredVelocity(Shark.ATTACK_SPEED, false); // shark doesn't change direction mid-charge
            entity.setTimeStamp();
            if(entity.isDoneWithAttackAnimation()) {
                entity.getStateMachine().changeState(PAUSE_AFTER_ATTACK);
            }
        }
    },
    PAUSE_AFTER_ATTACK(){
        @Override
        public void update(Shark entity) {
            entity.setDesiredVelocity(0, false);
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(Shark.ATTACK_COOLDOWN_TIME)){
                entity.getStateMachine().changeState(IDLE);
            }
        }
    },
    STUNNED(){
        @Override
        public void update(Shark entity) {
            entity.setDesiredVelocity(0, false);
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(Shark.STUN_TIME)){
                entity.getStateMachine().changeState(IDLE);
            }
        }
    },
    DYING(){
        @Override
        public void enter(Shark entity){
        }
        @Override
        public void update(Shark entity) {
        }
    };

    @Override
    public void enter(Shark entity) {
    }

    @Override
    public void exit(Shark entity) {
        entity.resetTimeStamp();
        entity.resetFrame();
    }

    @Override
    public boolean onMessage(Shark entity, Telegram telegram) {
        return false;
    }
}
