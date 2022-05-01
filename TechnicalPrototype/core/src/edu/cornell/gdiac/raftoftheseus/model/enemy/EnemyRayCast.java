package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;

import static edu.cornell.gdiac.raftoftheseus.model.GameObject.CATEGORY_PLAYER;

public class EnemyRayCast implements RayCastCallback {

    private boolean canSee = true;

    public boolean getCanSee(){
        if(!canSee){
            canSee = true;
            return false;
        }
        return true;
    }

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        if(fixture.getFilterData().categoryBits != CATEGORY_PLAYER){
            canSee = false;
            return 0;
        }
        return 1;
    }
}

