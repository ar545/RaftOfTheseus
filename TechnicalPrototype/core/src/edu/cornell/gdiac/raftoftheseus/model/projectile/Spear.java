package edu.cornell.gdiac.raftoftheseus.model.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.raftoftheseus.model.GameObject;
import edu.cornell.gdiac.raftoftheseus.obstacle.BoxObstacle;

public class Spear extends Projectile {

    /*=*=*=*=*=*=*=*=*=* INTERFACE *=*=*=*=*=*=*=*=*=*/
    @Override
    public ObjectType getType() { return ObjectType.SPEAR; }

    /**
     * Constructor for the Spear.
     */
    public Spear(Vector2 pos, Vector2 dir, Vector2 raft_speed) {
        super(pos.cpy(),"spear");
        physicsObject.setLinearVelocity(dir.scl(SPEED).mulAdd(raft_speed, 0.5f));
        setAngle(dir.angleDeg()-90f);
    }

    /**
     * Set Spear to stretch slightly larger than its hitbox.
     */
    @Override
    protected void setTextureTransform() {
        float h = TEXTURE_SCALE / texture.getRegionHeight();
        textureScale = new Vector2(h, h);
        textureOffset = new Vector2(0, 0);
    }
}
