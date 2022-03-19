package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;
import org.w3c.dom.Text;

/**
 * Class that provides the menu screen for the state of the game.
 * */
public class MenuMode implements Screen, InputProcessor {

    /** Background texture for start-up */
    private Texture background;
    /** Texture for the levels to select. */
    private Texture[] levels;

    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** Standard window size (for scaling) */
    private static int STANDARD_WIDTH  = 800;
    /** Standard window height (for scaling) */
    private static int STANDARD_HEIGHT = 700;
    /** Standard button scale */
    private static float BUTTON_SCALE  = 0.75f;
    /** Scaling factor. */
    private float scale;
    /** The height of the canvas window (necessary since sprite origin != screen origin) */
    private int heightY;

    /** Current state of a given level's button */
    private int[] levelPressState;
    /** Whether this player mode is still active */
    private boolean active;
    /** Level count **/
    private static final int LEVEL_COUNT = 9;

    /**
     * Creates a MenuMode with the default size and position.
     *
     * @param canvas The game canvas to draw to
     */
    public MenuMode(GameCanvas canvas) {
        this.canvas = canvas;
        // resize with canvas dimensions
        resize(canvas.getWidth(),canvas.getHeight());
        Gdx.input.setInputProcessor(this);
        active = true;
    }

    /**
     * Populates this mode from the given the directory.
     *
     * @param directory Reference to the asset directory.
     */
    public void populate(AssetDirectory directory) {
        background = directory.getEntry("menu", Texture.class);
        levels = new Texture[9];
        for (int i = 0; i < LEVEL_COUNT; i++) {
            levels[i] = directory.getEntry("level_" + i, Texture.class);
        }
    }

    /**
     * Called when this screen becomes the current screen.
     */
    public void show() {
        active = true;
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        active = false;
    }

    /**
     * Draw the status of this player mode.
     */
    private void draw() {
        canvas.begin();
        canvas.draw(background, 0, 0);
        for (int i = 0; i < LEVEL_COUNT; i++) {
            canvas.draw(levels[i], Color.WHITE, levels[i].getWidth(), levels[i].getHeight(),
                    20 + i * levels[i].getWidth(), 20 + i * levels[i].getWidth(),
                    0, BUTTON_SCALE * scale, BUTTON_SCALE * scale);
        }
        canvas.end();
    }

    /**
     * Called when the Screen should render itself.
     */
    public void render(float delta) {
        if (active) {
            draw();

            // TODO - notify the listener to exit
        }
    }

    /**
     * Called when this screen should release all resources.
     */
    public void dispose() {
    }

    /** Called when the Screen is resized.
     *
     * @param width The new width in pixels
     * @param height The new height in pixels
     */
    public void resize(int width, int height) {
        float sx = ((float)width)/STANDARD_WIDTH;
        float sy = ((float)height)/STANDARD_HEIGHT;
        scale = (sx < sy ? sx : sy);
        heightY = height;
    }

    public void pause() {
        // auto-generated
    }

    @Override
    public void resume() {
        // auto-generated
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
