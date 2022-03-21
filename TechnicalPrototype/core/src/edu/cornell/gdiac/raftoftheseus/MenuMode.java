package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.Rectangle;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;
import org.w3c.dom.Text;

/**
 * Class that provides the menu screen for the state of the game.
 * */
public class MenuMode implements Screen, InputProcessor {

    /** Background texture */
    private Texture background;
    /** Title texture */
    private Texture title;
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
    private static float BUTTON_SCALE  = 1.25f;
    /** Level padding x-axis inset */
    private static int PADDING_X = 100;
    /** Level button padding along the y-axis */
    private static int PADDING_Y = 100;
    /** Number of levels in each row */
    private static int NUM_COLS = 5;
    /** Padding between columns */
    private static int colPadding;
    /** Scaling factor. */
    private float scale;
    /** The height of the canvas window (necessary since sprite origin != screen origin) */
    private int heightY;

    /** Current state of a given level's button */
    private int levelPressState;
    /** Level selected */
    private int selectedLevel;
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
        active = true;
        levelPressState = 0;
        selectedLevel = -1;
    }

    /**
     * Populates this mode from the given the directory.
     *
     * @param directory Reference to the asset directory.
     */
    public void populate(AssetDirectory directory) {
        background = directory.getEntry("menu_background", Texture.class);
        title = directory.getEntry("menu_title", Texture.class);
        levels = new Texture[LEVEL_COUNT];
        for (int i = 0; i < LEVEL_COUNT; i++) {
            levels[i] = directory.getEntry("level_" + i, Texture.class);
        }
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the level selected by the player
     *
     * @return level selected
     */
    public int getSelectedLevel() { return selectedLevel; }

    /**
     * Returns true if the player has selected a level
     *
     * @return true if the player has selected a level
     */
    public boolean isReady() {
        return levelPressState == 2;
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
        colPadding = ((canvas.getWidth() - 2 * PADDING_X - NUM_COLS * levels[0].getWidth()) / (NUM_COLS - 1));
        canvas.begin();
        canvas.draw(background, 0, 0);
        canvas.draw(title, canvas.getWidth() / 2 - (title.getWidth() / 2),  (6 * canvas.getHeight() / 10));
        for (int i = 0; i < LEVEL_COUNT; i++) {
            Texture levelButton = levels[i];
            Color tint = (levelPressState == 1 && selectedLevel == i ? Color.GRAY: Color.WHITE);
            int buttonX = PADDING_X + (i % NUM_COLS) * (colPadding + levelButton.getWidth()) + (levelButton.getWidth() / 2);
            int buttonY = (4 * canvas.getHeight() / 10) - (i / NUM_COLS) * (PADDING_Y + levelButton.getHeight()) + (levelButton.getHeight() / 2);
            canvas.draw(levels[i], tint, levels[i].getWidth() / 2, levels[i].getHeight() / 2, buttonX,
                    buttonY, 0, BUTTON_SCALE * scale, BUTTON_SCALE * scale);
        }
        canvas.end();
    }

    /**
     * Called when the Screen should render itself.
     */
    public void render(float delta) {
        if (active) {
            draw();
            if (isReady() && listener != null) {
                listener.exitScreen(this, 0);
            }
        }
    }

    /**
     * Called when this screen should release all resources.
     */
    public void dispose() { }

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

    /**
     * Called when the Screen is paused.
     */
    public void pause() {
        // auto-generated
    }

    /**
     * Called when the Screen is resumed from a paused state.
     */
    public void resume() {
        // auto-generated
    }

    /**
     * Called when the screen was touched or a mouse button was pressed.
     *
     * @param screenX The x coordinate, origin is in the upper left corner
     * @param screenY The y coordinate, origin is in the upper left corner
     * @param pointer the pointer for the event
     * @param button the button
     * @return whether the input was processed
     */
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (levels == null || levelPressState == 2) return true;

        screenY = heightY - screenY;
        for (int i = 0; i < LEVEL_COUNT; i++) {
            Texture levelButton = levels[i];
            int buttonX = PADDING_X + (i % NUM_COLS) * (colPadding + levelButton.getWidth());
            int buttonY = (4 * canvas.getHeight() / 10) - (i / NUM_COLS) * (PADDING_Y + levelButton.getHeight());
            if (buttonX <= screenX && screenX <= buttonX + levelButton.getWidth() &&
                    buttonY <= screenY && screenY <= buttonY + levelButton.getHeight()) {
                levelPressState = 1;
                selectedLevel = i;
            }
        }

        return false;
    }

    /**
     * Called when a finger was lifted or a mouse button was released
     *
     * @param screenX The x coordinate, origin is in the upper left corner
     * @param screenY The y coordinate, origin is in the upper left corner
     * @param pointer the pointer for the event
     * @param button the button
     * @return whether the input was processed
     */
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (levelPressState == 1) {
            levelPressState = 2;
            return false;
        }
        return true;
    }

    /**
     * Called when a key is typed (UNSUPPORTED)
     *
     * @param character the key typed
     * @return whether to hand the event to other listeners.
     */
    public boolean keyTyped(char character) { return true; }

    /**
     * Called when a key is pressed (UNSUPPORTED)
     *
     * @param keycode the key pressed
     * @return whether to hand the event to other listeners.
     */
    public boolean keyDown(int keycode) { return true; }

    /**
     * Called when a key is released (UNSUPPORTED)
     *
     * @param keycode the key released
     * @return whether to hand the event to other listeners.
     */
    public boolean keyUp(int keycode) { return true; }

    /**
     * Called when the mouse or finger was dragged. (UNSUPPORTED)
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @return whether to hand the event to other listeners.
     */
    public boolean touchDragged(int screenX, int screenY, int pointer) { return true; }

    /**
     * Called when the mouse was moved without any buttons being pressed. (UNSUPPORTED)
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @return whether to hand the event to other listeners.
     */
    public boolean mouseMoved(int screenX, int screenY) {
        return true;
    }

    /**
     * Called when the mouse wheel was scrolled. (UNSUPPORTED)
     *
     * @param dx the amount of horizontal scroll
     * @param dy the amount of vertical scroll
     *
     * @return whether to hand the event to other listeners.
     */
    public boolean scrolled(float dx, float dy) { return true; }
}
