package edu.cornell.gdiac.shipdemo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.util.FilmStrip;

/**
 * Model class representing collectable driftwood.
 */
public class Wood {
    // TODO: Implement whatever is needed here.

    /** Position of the wodd */
    private Vector2 pos;
    private TextureRegion woodSprite;
    private float DEFAULT_SCALE = 1;
    private Color tint = new Color(1,1,1,1);

    /**
     * Creates a new wood pile at the given location.
     *
     * @param x The initial x-coordinate of the center
     * @param y The initial y-coordinate of the center
     */
    public Wood(float x, float y) {
        // TODO: implement
        pos = new Vector2(x, y);
    }

    public Vector2 getPosition() {
        return pos;
    }

    public float getDiameter() {
        return 128.0f;
    }

    public void drawWood(GameCanvas canvas) {
        if (woodSprite == null) {
            return;
        }
        // For placement purposes, put origin in center.
        float ox = 0.5f * woodSprite.getRegionWidth();
        float oy = 0.5f * woodSprite.getRegionHeight();

        // Need to negate y scale because of coordinate access flip.
        // Then draw the ship
        canvas.draw(woodSprite, tint, ox, oy, pos.x, pos.y, 0, DEFAULT_SCALE, DEFAULT_SCALE);
    }

    public void setTexture(TextureRegion value) {
        woodSprite = value;
    }
}
