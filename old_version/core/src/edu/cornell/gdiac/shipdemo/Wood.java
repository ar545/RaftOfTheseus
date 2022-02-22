package edu.cornell.gdiac.shipdemo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.util.FilmStrip;

/**
 * Model class representing collectable driftwood.
 */
public class Wood extends GameObject {
    // TODO: Implement whatever is needed here.

    @Override
    public ObjectType getType() {
        return ObjectType.WOOD;
    }

    /**
     * Creates a new wood pile at the given location.
     *
     * @param x The initial x-coordinate of the center
     * @param y The initial y-coordinate of the center
     */
    public Wood(float x, float y) {
        position = new Vector2(x, y);
        radius = 64;
    }
}
