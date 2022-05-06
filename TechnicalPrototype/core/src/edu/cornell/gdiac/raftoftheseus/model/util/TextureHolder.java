package edu.cornell.gdiac.raftoftheseus.model.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/**
 * Class that holds the same information required to draw a texture in GameObject, but for additional textures.
 */
public class TextureHolder {

    private TextureRegion texture;
    private Vector2 origin = new Vector2(0, 0);
    private Vector2 textureScale = new Vector2(1, 1);
    private Vector2 textureOffset = new Vector2(0, 0);
    private float angle = 0;
    private Color color = Color.WHITE;

    public TextureHolder(TextureRegion texture){
        setTexture(texture);
    }

    public void setTexture(TextureRegion texture) {
        this.texture = texture;
        origin.set(texture.getRegionWidth()/2.0f, texture.getRegionHeight()/2.0f);
    }
    public TextureRegion getTexture() { return texture; }

    public void setOrigin(Vector2 origin) { this.origin = origin; }
    public Vector2 getOrigin() { return origin; }

    public void setTextureScale(Vector2 textureScale) { this.textureScale = textureScale; }
    public Vector2 getTextureScale() { return textureScale; }

    public void setTextureOffset(Vector2 textureOffset) { this.textureOffset = textureOffset; }
    public Vector2 getTextureOffset() { return textureOffset; }

    public void setAngle(float angle) { this.angle = angle; }
    public float getAngle() { return angle; }

    public void setColor(Color color) { this.color = color; }
    public Color getColor() { return color; }
}
