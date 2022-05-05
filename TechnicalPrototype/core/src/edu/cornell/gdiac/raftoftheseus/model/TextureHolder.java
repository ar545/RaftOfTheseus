package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class TextureHolder {

    public TextureRegion texture;
    public Vector2 origin = new Vector2(0, 0);
    public Vector2 textureScale = new Vector2(1, 1);
    public Vector2 textureOffset = new Vector2(0, 0);
    public float angle = 0;
    public Color color = Color.WHITE;

    public TextureHolder(TextureRegion texture){
        this.texture = texture;
        origin.set(texture.getRegionWidth()/2.0f, texture.getRegionHeight()/2.0f);
    }

    public void setOrigin(Vector2 origin) { this.origin = origin; }

    public void setTextureScale(Vector2 textureScale) { this.textureScale = textureScale; }

    public void setTextureOffset(Vector2 textureOffset) { this.textureOffset = textureOffset; }

    public void setAngle(float angle) { this.angle = angle; }

    public void setColor(Color color) { this.color = color; }
}
