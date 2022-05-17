package edu.cornell.gdiac.raftoftheseus.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class HealthBar
{
    /** the number of floats per vertex in the libgdx spritebatch */
    private static final int FLOAT_PER_VERTEX = 5;
    /** the number of vertices per rectangle */
    private static final int VERTEX_PER_RECTANGLE = 4;
    /** what is pi in degree? */
    private static final float FULL_DEGREE = 180;
    /** position of the lower-left corner to draw the texture */
    public Vector2 position = new Vector2();
    /** reference to the texture */
    public Texture texture;
    /** reference to the texture top */
    public Texture textureTop;
    /** The vertices representing the cut texture */
    public float[] vertices = new float[VERTEX_PER_RECTANGLE * FLOAT_PER_VERTEX];
    /** How far in time do we retrace the health */
    private static final int retracing_health_size = 20;
    /** health history */
    private float[] retracing_health = new float[retracing_health_size];

    /** the contrast color to show when the player is suffering from damage */
    private Color altColor = Color.SKY;

    private boolean retracing_tick = false;
    /** pointer to the retracing array, where to read from and write to, and move the pointer forward */
    int retracing_index = 0;
    /** Constructor call with known texture */
    public HealthBar(Texture texture, Texture texture_top){ position.setZero(); this.texture = texture; this.textureTop = texture_top; }

    public void adjustMode(boolean accessibilityMode){
        if(accessibilityMode){ altColor = Color.BLUE; } else { altColor = Color.SKY; }
    }

    /** This function calculate the correct health bar color
     * @param median for red color the median should be 1/3 and 2/3 for green color
     * @param health the health percentage for the player
     * @return the rgb code representing the red or green color
     * old color function: Color c = new Color(Math.min(1, 2 - health * 2), Math.min(health * 2f, 1), 0, 1);*/
    public float makeColor(float median, float health){ return Math.max(0, Math.min((1.5f - 3 * Math.abs(health - median)), 1)); }

    /** update the vertices according to the angle to cut the texture, and update the texture with selected color
     * @param health the health to draw the cut texture */
    public void update(float health, Color color) {
        float angle = (1-health) * FULL_DEGREE; // float representing the cut angle, 180 as dead, 0 as full health

        Vector2 d = (new Vector2(1.0f, 0.0f)).rotateDeg(angle);
        Vector2 c = new Vector2(0.5f, 0.5f);
        Vector2 la = (new Vector2(d)).scl( 1.0f).add(c);
        Vector2 lb = (new Vector2(d)).scl(-1.0f).add(c);

        Vector2 tl = new Vector2(0, 1); // top left
        Vector2 tr = new Vector2(1, 1); // top right
        Vector2 bl = new Vector2(0, 0); // bottom left
        Vector2 br = new Vector2(1, 0); // bottom right

        Vector2 i1 = new Vector2();
        Vector2 i2 = new Vector2();

        if (Intersector.intersectSegments(c, la, tl, tr, i1) || Intersector.intersectSegments(c, lb, tl, tr, i1))
            i2.set(1.0f - i1.x, 1.0f - i1.y);
        else {
            if (Intersector.intersectSegments(c, la, tl, bl, i1) || Intersector.intersectSegments(c, lb, tl, bl, i1))
                i2.set(1.0f - i1.x, 1.0f - i1.y);
        }

        Vector2[] vertexList = new Vector2[] {
                tl, tr, bl, br, i1, i2
        };

        Array<VertexAngle> vas = new Array<>();
        for (Vector2 v : vertexList) {
            Vector2 vd = (new Vector2(v)).sub(c);
            float a = d.angleDeg(vd);
            VertexAngle va = new VertexAngle();
            va.v = v;
            va.a = a;
            vas.add(va);
        }

        vas.sort((a, b) -> Float.compare(a.a, b.a));

        Array<Vector2> nv = new Array<>();
        for (VertexAngle va : vas)
            nv.add(va.v);
        int index = nv.indexOf(i1, true);
        int idx = 0;
        Vector2 vertex;
        for(int j = 0; j < VERTEX_PER_RECTANGLE; ++j) {
            if(angle < 45f){
                vertex = nv.get((index + j) % nv.size);
            }else{
                vertex = nv.get((index + j + 3) % nv.size);
            }
            float width = texture.getWidth();
            float height = texture.getWidth();
            float fx2 = position.x + width * vertex.x - width / 2.0f;
            float fy2 = position.y + height * vertex.y - height / 2.0f;

            vertices[idx++] = fx2;
            vertices[idx++] = fy2;
            vertices[idx++] = color.toFloatBits(); // Color.WHITE_FLOAT_BITS
            vertices[idx++] = vertex.x;
            vertices[idx++] = 1.0f - vertex.y;
        }
    }

    /** Draw the new cut shape
     * @param batch the reference to the canvas sprite batch */
    public void render(PolygonSpriteBatch batch) {
        batch.draw(texture, vertices, 0, FLOAT_PER_VERTEX * VERTEX_PER_RECTANGLE);
    }

    /** Sequence of health bar drawing */
    public void updateAndDraw(Vector2 position, float health, float firing, PolygonSpriteBatch spriteBatch) {
        // set position
        this.position.set(position);
        // make color (red - green transition color)
        Color color = new Color(makeColor((float)1/3, health), makeColor((float)2/3, health), 0.2f, 1);
        float retrace_health = retraceHealth(health);
        if(retrace_health > health){
            Color bkgColor = Color.ROYAL;
            if(retracing_index % 7 < 3){ bkgColor = altColor; }
           // cut according to retrace_health
           update(retrace_health, bkgColor);
           // render to retrace health
           render(spriteBatch);
        }
        // cut according to real-time health
        update(health, color);
        // render real-time health
        render(spriteBatch);
        drawTop(spriteBatch, firing, position);
    }

    private void drawTop(PolygonSpriteBatch spriteBatch, float firing, Vector2 position) {
        TextureRegion tr = makeTopBar(firing);
        spriteBatch.setColor(Color.RED);
        spriteBatch.draw(tr, position.x - ((float) textureTop.getWidth() / 2), position.y);
        spriteBatch.setColor(Color.WHITE);
    }

    private TextureRegion makeTopBar(float firing) {
        float widthRatio = 0.42f + 0.16f * firing; // take width between 0.42 and 0.58
        return new TextureRegion(textureTop, (int) (textureTop.getWidth() * widthRatio), textureTop.getHeight() / 2); // only upper half is drawn
    }

    /** find the health "retracing_health_size" screens ago */
    private float retraceHealth(float health) {
        retracing_tick = !retracing_tick;
        if(retracing_tick){ return retracing_health[retracing_index]; } else {
            float result = retracing_health[retracing_index];
            retracing_health[retracing_index] = health;
            retracing_index ++;
            if(retracing_index >= retracing_health_size){ retracing_index = 0; }
            return result;
        }
    }

    public static class VertexAngle {
        public Vector2 v;
        public float a;
    }

    //  Function for render with translation
    public void renderTranslation(PolygonSpriteBatch batch, Vector2 translation) {
        translate(translation.x, translation.y);
        batch.draw(texture, vertices, 0, FLOAT_PER_VERTEX * VERTEX_PER_RECTANGLE);
        translate(-translation.x, -translation.y);
    }

    public void translate(float x, float y) {
        for(int i = 0; i < vertices.length; i += FLOAT_PER_VERTEX) {
            vertices[i] += x;
            vertices[i + 1] += y;
        }
    }
}
