package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;

public class SettingsMode implements Screen, InputProcessor {

    /** Background texture */
    private Texture background;
    /** Texture for the exit button */
    private Texture exitButton;
    /** Font for drawing text */
    private BitmapFont displayFont;
    /** Slider for volume */
    private Slider volumeSlider;
    /** Slider for sound effects */
    private Slider soundEffectsSlider;
    /** Texture for the WASD icon */
    private Texture wasdIcon;
    /** Texture for the arrows icon */
    private Texture arrrowsIcon;
    /** Texture for the keys */
    private Texture keysIcon;

    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** Standard window size (for scaling) */
    private static int STANDARD_WIDTH  = 800;
    /** Standard window height (for scaling) */
    private static int STANDARD_HEIGHT = 700;
    /** Scaling factor. */
    private float scale;
    /** Scheme scale */
    private float SCHEME_SCALE = 0.9f;
    /** Keys scale */
    private float KEYS_SCALE = 0.6f;
    /** The height of the canvas window (necessary since sprite origin != screen origin) */
    private int heightY;
    /** Exit button x-position */
    private float exitButtonX;
    /** Exit button y-position */
    private float exitButtonY;
    /** Exit button clicked */
    private boolean exitPressed;
    /** Previous screen (4 - start, 5 - menu, 6 - world) */
    private int previousMode;

    /** Whether this player mode is still active */
    private boolean active;

    /**
     * Creates a SettingsMode with the default size and position.
     *
     * @param canvas The game canvas to draw to
     */
    public SettingsMode(GameCanvas canvas) {
        this.canvas = canvas;
        // resize with canvas dimensions
        resize(canvas.getWidth(), canvas.getHeight());
        active = true;
        exitPressed = false;
    }

    /**
     * Populates this mode from given the directory
     *
     * @param directory Reference to the asset directory
     */
    public void populate(AssetDirectory directory) {
        background = directory.getEntry("sea_background", Texture.class);
        exitButton = directory.getEntry("exit_button", Texture.class);
        wasdIcon = directory.getEntry("settings_wasd", Texture.class);
        arrrowsIcon = directory.getEntry("settings_arrows", Texture.class);
        keysIcon = directory.getEntry("settings_keys", Texture.class);
        displayFont = directory.getEntry( "diogenes", BitmapFont.class);
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to request to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the previous mode value
     * @param previousMode
     */
    public void setPreviousMode(int previousMode) { this.previousMode = previousMode; }

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
        canvas.drawBackground(background, true);
        exitButtonX = 0.025f * canvas.getWidth();
        exitButtonY = 0.9f * canvas.getHeight();
        canvas.draw(exitButton, exitButtonX,  exitButtonY);
        displayFont.getData().setScale(0.95f);
        float textY = 3.5f * canvas.getHeight() / 10;
        canvas.drawTextCentered("SETTINGS", displayFont, textY);
        displayFont.getData().setScale(0.5f);
        int paddingX = 100;
        int paddingY = 65;
        textY = 5f * canvas.getHeight() / 10 + textY - paddingY * 1.5f;
        canvas.drawText("VOLUME", displayFont, paddingX, textY);
        displayFont.getData().setScale(0.4f);
        textY = textY - paddingY;
        canvas.drawText("MUSIC", displayFont, 1.5f * paddingX, textY);
        textY = textY - paddingY;
        canvas.drawText("SOUND EFFECTS", displayFont, 1.5f * paddingX, textY);
        displayFont.getData().setScale(0.5f);
        textY = textY - paddingY;
        canvas.drawText("KEYBOARD SCHEME", displayFont, paddingX, textY);
        textY = textY - paddingY;
        canvas.draw(wasdIcon, Color.WHITE, 0, wasdIcon.getHeight(), 1.5f * paddingX,
                textY, 0, scale * SCHEME_SCALE, scale * SCHEME_SCALE);
        canvas.draw(arrrowsIcon, Color.WHITE, 0, arrrowsIcon.getHeight(), 2.5f * paddingX + wasdIcon.getWidth(),
                textY, 0, scale * SCHEME_SCALE, scale * SCHEME_SCALE);
        textY = textY - paddingY - wasdIcon.getHeight() * SCHEME_SCALE;
        canvas.drawText("KEYBOARD SHORTCUTS", displayFont, paddingX, textY);
        textY = textY - paddingY;
        canvas.draw(keysIcon, Color.WHITE, 0, keysIcon.getHeight(), 1.5f * paddingX, textY, 0, scale * KEYS_SCALE, scale * KEYS_SCALE);
        canvas.end();
    }

    /**
     * Called when the Screen should render itself.
     */
    public void render(float delta) {

        if (active) {
            draw();
            if (exitPressed || InputController.getInstance().didExit()) {
                listener.exitScreen(this, previousMode);
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
        // detect exit button press
        screenY = heightY - screenY;
        if (inBounds(exitButtonX, exitButtonY, exitButton.getWidth(), exitButton.getHeight(), screenX, screenY)) {
            exitPressed = true;
            return false;
        }

        // detect sliders

        return true;
    }

    /**
     * Checks if a point is within a bound
     *
     * @param x The x-position of the drawn bound
     * @param y The y-position of the drawn bound
     * @param width The width of the drawn bound
     * @param height The height of the drawn bound
     * @param screenX The x-position to be checked
     * @param screenY The y-position to be checked
     * @return true if screenX and screenY are within the bounds, false otherwise
     */
    private boolean inBounds(float x, float y, int width, int height, int screenX, int screenY) {
        return (x <= screenX && screenX <= x + width && y <= screenY && screenY <= y + height);
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

    /** Reset the settings exit pressed state */
    public void resetPressedState() { exitPressed = false; }
}
