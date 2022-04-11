package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.obstacle.WheelObstacle;

import java.util.Random;

public class Siren extends WheelObstacle implements Telegraph {
    /** Parameters */
    private Raft targetRaft;
    private Vector2 moveVector = new Vector2();
    private StateMachine<Siren, SirenController> stateMachine;
    private static void setConstants(JsonValue objParams){
    }


    public GameObject.ObjectType getType() {
        return GameObject.ObjectType.ENEMY;
    }
    @Override
    public boolean handleMessage(Telegram msg) {
        return false;
    }

    public void setTargetRaft(Raft targetRaft) {
        this.targetRaft = targetRaft;
    }

    public Siren(Vector2 position, Raft targetRaft) {
        super();
        setPosition(position);
        setBodyType(BodyDef.BodyType.DynamicBody);
        this.targetRaft = targetRaft;
        fixture.filter.categoryBits = CATEGORY_ENEMY;
        fixture.filter.maskBits = MASK_ENEMY;
        stateMachine = new DefaultStateMachine<Siren, SirenController>(this, SirenController.SPAWN);
    }

    //    // TODO: this will change depending on implementation of AIController
    public void update(float dt) {
        super.update(dt);
        if (moveVector != null && targetRaft != null) {
            body.applyLinearImpulse(moveVector, getPosition(), true);
        }
    }


    /**
     * Sets moveVector so that applying it as a linear impulse brings this object's velocity closer to
     * moveVector*topSpeed.
     * Precondition: moveVector.len() == 1.
     * @param topSpeed Won't apply an impulse that takes us above this speed
     * @param smoothing Impulse is scaled by (1-smoothing). Higher smoothing means wider turns, slower responses.
     */
    private void calculateImpulse(float topSpeed, float smoothing) {
        float currentSpeed = getLinearVelocity().dot(moveVector); // current speed in that direction
        float impulseMagnitude = (topSpeed - currentSpeed)*body.getMass()*(1-smoothing);
        moveVector.scl(impulseMagnitude);
    }

    @Override
    public float getCrossSectionalArea() {
        return super.getCrossSectionalArea()*0.2f; // sharks are less affected by drag
    }
}
