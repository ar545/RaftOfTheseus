package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
public enum NewSharkState implements State<NewShark> {
    IDLE(){
        @Override
        public void update(NewShark entity) {
            entity.setDesiredVelocity(0, false);
            if(entity.canSee() && entity.inRange(NewShark.APPROACH_RANGE)) {
                entity.getStateMachine().changeState(APPROACH);
            }
        }
    },
    APPROACH(){
        @Override
        public void update(NewShark entity) {
            entity.setDesiredVelocity(NewShark.APPROACH_SPEED, true);
            if(!(entity.canSee() && entity.inRange(NewShark.APPROACH_RANGE))) {
                entity.getStateMachine().changeState(IDLE);
            } else {
                if (entity.inRange(NewShark.ATTACK_RANGE)) {
                    entity.getStateMachine().changeState(PAUSE_BEFORE_ATTACK);
                }
            }
        }
    },
    PAUSE_BEFORE_ATTACK(){
        @Override
        public void update(NewShark entity) {
            entity.setDesiredVelocity(0, true);
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(NewShark.ATTACK_WINDUP_TIME)){
                entity.getStateMachine().changeState(ATTACK);
            }
        }
    },
    ATTACK(){
        @Override
        public void update(NewShark entity) {
            entity.setDesiredVelocity(NewShark.ATTACK_SPEED, false); // shark doesn't change direction mid-charge
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(NewShark.ATTACK_WINDUP_TIME)) {
                entity.getStateMachine().changeState(PAUSE_AFTER_ATTACK);
            }
        }
    },
    PAUSE_AFTER_ATTACK(){
        @Override
        public void update(NewShark entity) {
            entity.setDesiredVelocity(0, false);
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(NewShark.ATTACK_COOLDOWN_TIME)){
                entity.getStateMachine().changeState(IDLE);
            }
        }
    },
    STUNNED(){
        @Override
        public void update(NewShark entity) {
            entity.setDesiredVelocity(0, false);
            entity.setTimeStamp();
            if(entity.hasTimeElapsed(NewShark.STUN_TIME)){
                entity.getStateMachine().changeState(IDLE);
            }
        }
    },
    DYING(){
        @Override
        public void enter(NewShark entity){
        }
        @Override
        public void update(NewShark entity) {
        }
    };

    @Override
    public void enter(NewShark entity) {
    }

    @Override
    public void exit(NewShark entity) {
        entity.resetTimeStamp();
        entity.resetFrame();
    }

    @Override
    public boolean onMessage(NewShark entity, Telegram telegram) {
        return false;
    }
}
