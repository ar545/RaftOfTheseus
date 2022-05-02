package edu.cornell.gdiac.raftoftheseus.model.enemy;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;

import static edu.cornell.gdiac.raftoftheseus.model.GameObject.CATEGORY_LIGHT_BLOCK;

public class EnemyRayCast implements RayCastCallback {

    private boolean canSee = true;

    public void reset() {
        canSee = true;
    }

    public boolean getCanSee(){
        return canSee;
    }

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        if(fixture.getFilterData().categoryBits == CATEGORY_LIGHT_BLOCK){
            canSee = false;
            return 0;
        }
        return 1;
    }
}

