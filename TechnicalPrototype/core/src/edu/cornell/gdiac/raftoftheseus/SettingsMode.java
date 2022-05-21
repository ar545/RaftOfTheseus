package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.raftoftheseus.singleton.InputController;
import edu.cornell.gdiac.raftoftheseus.singleton.MusicController;
import edu.cornell.gdiac.raftoftheseus.singleton.SfxController;
import edu.cornell.gdiac.util.ScreenListener;
import org.lwjgl.Sys;

import java.util.stream.StreamSupport;

public class SettingsMode implements Screen, InputProcessor {

    private Stage stage;
    private Table table;
    private Skin skin;

    /** Background texture */
    private Texture background;
    /** Texture for slider knob */
    private Texture sliderKnob;
    /** Texture for slider bar */
    private Texture sliderBar;
    /** Slider for volume */
    private Slider musicSlider;
    /** Slider for sound effects */
    private Slider soundEffectsSlider;
    /** Background for gold key */
    private Texture goldKeyBackground;
    /** Background for black key  */
    private Texture blackKeyBackground;
    /** Background for long text key */
    private Texture longTextKeyBackground;

    /** TextButton for accessibility */
    TextButton accessibilityButton;
    /** TextButton for the map key */
    TextButton mapKeyButton;
    /** TextButton for the reset key */
    TextButton resetKeyButton;
    /** TextButton for the pause key */
    TextButton pauseKeyButton;

    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;
    /** Control settings mappings */
    private static JsonValue controlSettings;

    // Load from "screen" in "obj parameters"
    public static void setContants(JsonValue objParams){
        STANDARD_WIDTH = objParams.getInt(0);
        STANDARD_HEIGHT = objParams.getInt(1);
    }

    /** Standard window size (for scaling) */
    private static int STANDARD_WIDTH;
    /** Standard window height (for scaling) */
    private static int STANDARD_HEIGHT;
    /** Scaling factor. */
    private float scale;
    /** The height of the canvas window (necessary since sprite origin != screen origin) */
    private int heightY;
    /** Exit button clicked */
    private boolean exitPressed;
    /** Previous screen (5 - menu, 6 - world) */
    private int previousMode;
    /** Music volume */
    private float musicVolume;
    /** Sound effects volume */
    private float soundEffectsVolume;
    /** Exit code to display menu screen */
    private int EXIT_MENU;
    /** Whether this player mode is still active */
    private boolean active;
    /** Whether accessibility mode is turned on */
    private boolean accessibilityModeActive;
    /** Whether map key editing mode is on */
    private boolean editMapKeyEnable;
    /** Whether reset key editing mode is on */
    private boolean editResetKeyEnable;
    /** Whether pause key editing mode is on */
    private boolean editPauseKeyEnable;

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
        accessibilityModeActive = false;
        editMapKeyEnable = false;
        editResetKeyEnable = false;
        editPauseKeyEnable = false;
        skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
        setSkinFonts();
    }

    /**
     * Populates this mode from given the directory
     *
     * @param directory Reference to the asset directory
     */
    public void populate(AssetDirectory directory) {
        background = directory.getEntry("transition_screen", Texture.class);
        goldKeyBackground = directory.getEntry("gold_key_background", Texture.class);
        blackKeyBackground = directory.getEntry("black_key_background", Texture.class);
        longTextKeyBackground = directory.getEntry("long_text_key_background", Texture.class);
        sliderKnob = directory.getEntry("slider_knob", Texture.class);
        sliderBar = directory.getEntry("slider_bar", Texture.class);
    }

    /** Sets the ScreenListener for this mode, the ScreenListener will respond to request to quit. */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    /** Sets the control settings  */
    public static void setKeyParams(JsonValue keyParams){
        controlSettings = keyParams;
    }

    /** Sets exit code to menu */
    public void setExitMenu(int value) { this.EXIT_MENU = value; }

    /** @param previousMode the previous mode value to be set */
    public void setPreviousMode(int previousMode) { this.previousMode = previousMode; }

    /** Sets the generator for fonts */
    private void setSkinFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.local("fonts/DIOGENES.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter smallParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        smallParameter.size = (int) (60*Gdx.graphics.getDensity());
        BitmapFont smallFont = generator.generateFont(smallParameter);

        FreeTypeFontGenerator.FreeTypeFontParameter mediumParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        mediumParameter.size = (int) (80*Gdx.graphics.getDensity());
        BitmapFont mediumFont = generator.generateFont(mediumParameter);

        FreeTypeFontGenerator.FreeTypeFontParameter largeParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        largeParameter.size = (int) (100*Gdx.graphics.getDensity());
        BitmapFont largeFont = generator.generateFont(largeParameter);

        skin.add("diogenes-font-small", smallFont);
        skin.add("diogenes-font-medium", mediumFont);
        skin.add("diogenes-font-large", largeFont);
    }

    /** Called when this screen becomes the current screen. */
    public void show() {
        active = true;
        stage = new Stage(new StretchViewport(canvas.getWidth(), canvas.getHeight()));
        table = new Table(skin);
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
        buildSettings();
    }

    /** Called when this screen is no longer the current screen for a Game. */
    public void hide() {
        active = false;
    }

    /** Constructs the view. */
    private void buildSettings() {
        stage.addActor(table);
        table.align(Align.top);
        table.setFillParent(true);

        skin.add("background", background);
        table.setBackground(skin.getDrawable("background"));

        float titleFontSize = 0.8f * Gdx.graphics.getDensity();
        float headingFontSize = 0.7f * Gdx.graphics.getDensity();
        float subheadingFontSize = 0.5f * Gdx.graphics.getDensity();
        float keysFontSize = 0.4f * Gdx.graphics.getDensity();

        Table part1 = new Table();
        part1.align(Align.left);
        TextButton menuButton = UICreator.createTextButton("BACK", skin, Color.WHITE, UICreator.FontSize.SMALL);
        menuButton.addListener(UICreator.createListener(menuButton, Color.GOLD, Color.WHITE, this::setExitPressed));
        part1.add(menuButton).expandX().align(Align.left).padRight(canvas.getWidth() * Gdx.graphics.getDensity()).padTop(50 * Gdx.graphics.getDensity());
        table.add(part1);
        table.row();

        Table part2 = new Table();
        part2.add(UICreator.createLabel("SETTINGS", skin,  UICreator.FontSize.LARGE)).expandX().align(Align.center);
        table.add(part2).padTop(-50);
        table.row();

        Table part3 = new Table();
        part3.add(UICreator.createLabel("VOLUME", skin,  UICreator.FontSize.MEDIUM)).padLeft(80).expandX().align(Align.left);
        part3.row();
        part3.add(UICreator.createLabel("MUSIC", skin,  UICreator.FontSize.SMALL)).padLeft(100).expandX().align(Align.left);

        Drawable sliderKnobDrawable = new TextureRegionDrawable(new TextureRegion(sliderKnob));
        Drawable sliderBarDrawable = new TextureRegionDrawable(new TextureRegion(sliderBar));
        SliderStyle sliderStyle = new SliderStyle(sliderBarDrawable, sliderKnobDrawable);

        musicVolume = MusicController.getInstance().getMasterMusicVolume() * 100f;
        Label musicValueLabel = UICreator.createLabel(String.valueOf((int) Math.floor(musicVolume)), skin,  UICreator.FontSize.SMALL);

        musicSlider = new Slider(0, 100, 1, false, sliderStyle);
        musicSlider.setValue(musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                musicVolume = musicSlider.getValue();
                SfxController.getInstance().setMasterMusicVolume(musicVolume / 100);
                MusicController.getInstance().setMasterMusicVolume(musicVolume / 100);
                musicValueLabel.setText((int) Math.floor(musicVolume));
            }
        });
        stage.addActor(musicSlider);
        part3.add(musicSlider).expandX().align(Align.left).width(600).padLeft(-100);
        part3.add(musicValueLabel).expandX().align(Align.left).width(80).padLeft(60);
        part3.row();

        soundEffectsVolume = SfxController.getInstance().getMasterSFXVolume() * 100;
        part3.add(UICreator.createLabel("SOUND EFFECTS", skin,  UICreator.FontSize.SMALL)).padLeft(100).align(Align.left);

        Label soundEffectsValueLabel = UICreator.createLabel(String.valueOf((int) Math.floor(soundEffectsVolume)), skin,  UICreator.FontSize.SMALL);
        soundEffectsSlider = new Slider(0, 100, 1, false, sliderStyle);
        soundEffectsSlider.setValue(soundEffectsVolume);
        soundEffectsSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                soundEffectsVolume = soundEffectsSlider.getValue();
                SfxController.getInstance().setMasterSfxVolume(soundEffectsVolume / 100);
                soundEffectsValueLabel.setText((int) Math.floor(soundEffectsVolume));
            }
        });
        stage.addActor(soundEffectsSlider);
        part3.add(soundEffectsSlider).expandX().align(Align.left).width(600).padLeft(-100);
        part3.add(soundEffectsValueLabel).align(Align.left).width(80).padLeft(60);
        part3.row();

        part3.add(UICreator.createLabel("ACCESSIBILITY", skin,  UICreator.FontSize.MEDIUM)).padLeft(80).expandX().align(Align.left);
        part3.row();

        Table part3a = new Table();
        part3a.add(UICreator.createLabel("ACCESSIBILITY MODE", skin,  UICreator.FontSize.SMALL)).padLeft(0).expandX().align(Align.left);
        accessibilityButton = UICreator.createTextButton(accessibilityModeActive ? "On" : "Off", skin, Color.WHITE, blackKeyBackground, UICreator.FontSize.SMALL);
        accessibilityButton.addListener(UICreator.createListener(accessibilityButton, Color.GRAY, Color.WHITE, this::changeAccessibilityMode));
        part3a.add(accessibilityButton).padLeft(30).width(80).height(60);
        part3.add(part3a);
        part3.row();

        part3.add(UICreator.createLabel("KEYBOARD SHORTCUTS", skin,  UICreator.FontSize.MEDIUM)).padLeft(80).expandX().align(Align.left);
        table.add(part3);
        table.row();

        Table part4 = new Table();
        int keysPadding = 30;

        String mapKeyString = controlSettings.get("mouse keyboard").get("map").asString();
        String resetKeyString = controlSettings.get("mouse keyboard").get("reset").asString();
        String pauseKeyString = controlSettings.get("mouse keyboard").get("pause").asString();
        mapKeyString = mapKeyString.equals("Escape") ? "Esc" : mapKeyString;
        resetKeyString = resetKeyString.equals("Escape") ? "Esc" : resetKeyString;
        pauseKeyString = pauseKeyString.equals("Escape") ? "Esc" : pauseKeyString;

        part4.add(UICreator.createLabel("MAP", skin,  UICreator.FontSize.SMALL)).padRight(keysPadding);
        mapKeyButton = UICreator.createTextButton(mapKeyString, skin, Color.WHITE, blackKeyBackground, UICreator.FontSize.SMALL);
        mapKeyButton.addListener(UICreator.createListener(mapKeyButton, this::changeKeyMapping));
        part4.add(mapKeyButton).padRight(keysPadding).width(60).height(60);

        part4.add(UICreator.createLabel("RESTART", skin,  UICreator.FontSize.SMALL)).padRight(keysPadding);
        resetKeyButton = UICreator.createTextButton(resetKeyString, skin, Color.WHITE, blackKeyBackground, UICreator.FontSize.SMALL);
        resetKeyButton.addListener(UICreator.createListener(resetKeyButton, this::changeKeyMapping));
        part4.add(resetKeyButton).padRight(keysPadding).width(60).height(60);

        part4.add(UICreator.createLabel("PAUSE", skin, UICreator.FontSize.SMALL)).padRight(keysPadding);
        pauseKeyButton = UICreator.createTextButton(pauseKeyString, skin, Color.WHITE, blackKeyBackground, UICreator.FontSize.SMALL);
        pauseKeyButton.addListener(UICreator.createListener(pauseKeyButton, this::changeKeyMapping));
        part4.add(pauseKeyButton).padRight(keysPadding).width(60).height(60);
        table.add(part4);
        table.row();
    }

    /** Change the appropriate key mapping */
    private void changeKeyMapping(TextButton button) {
        if (button == mapKeyButton) {
            editMapKeyEnable = !editMapKeyEnable;
            editResetKeyEnable = false;
            editPauseKeyEnable = false;
        } else if (button == resetKeyButton) {
            editResetKeyEnable = !editResetKeyEnable;
            editMapKeyEnable = false;
            editPauseKeyEnable = false;
        } else if (button == pauseKeyButton) {
            editPauseKeyEnable = !editPauseKeyEnable;
            editResetKeyEnable = false;
            editMapKeyEnable = false;
        }
        updateKeyButtonAppearance();
    }

    private void updateKeyButtonAppearance() {
        UICreator.setTextButtonStyle(mapKeyButton, skin, UICreator.FontSize.SMALL, Color.WHITE, editMapKeyEnable ? goldKeyBackground : blackKeyBackground);
        UICreator.setTextButtonStyle(resetKeyButton, skin, UICreator.FontSize.SMALL, Color.WHITE, editResetKeyEnable ? goldKeyBackground : blackKeyBackground);
        UICreator.setTextButtonStyle(pauseKeyButton, skin, UICreator.FontSize.SMALL, Color.WHITE, editPauseKeyEnable ? goldKeyBackground : blackKeyBackground);
    }

    /** Set accessibility mode */
    private void changeAccessibilityMode() {
        // TODO
        accessibilityModeActive = !accessibilityModeActive;
        accessibilityButton.setText(accessibilityModeActive ? "On" : "Off");
        canvas.setAccessibility(accessibilityModeActive); // Update game canvas to accessible
    }


    /** Draw the status of this player mode. */
    private void draw() {
        stage.act();
        stage.draw();
    }

    /** Called when the Screen should render itself. */
    public void render(float delta) {
        InputController input = InputController.getInstance();
        input.readInput();
        if (active) {
            draw();
            if (exitPressed || InputController.getInstance().didExit()) {
                resetExitPressed();
                resetEditKeys();
                listener.exitScreen(this, previousMode);
            }
        }
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
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    /** Reset the settings menu and exit pressed state */
    private void setExitPressed() { exitPressed = true; }
    private void resetExitPressed() { exitPressed = false; }
    private void resetEditKeys() {
        editResetKeyEnable = false;
        editMapKeyEnable = false;
        editPauseKeyEnable = false;
    }

    /** Called when the Screen is paused. */
    public void pause() {}
    /** Called when the Screen is resumed from a paused state. */
    public void resume() {}
    /** Called when this screen should release all resources. */
    public void dispose() { }

    /**
     * Called when a key was pressed
     *
     * @param keycode one of the constants in {@link Input.Keys}
     * @return whether the input was processed
     */
    @Override
    public boolean keyDown(int keycode) {
        String key = Input.Keys.toString(keycode);
        String currentMapKey = mapKeyButton.getText().toString();
        String currentResetKey = resetKeyButton.getText().toString();
        String currentPauseKey = pauseKeyButton.getText().toString();
        String displayText = key.equals("Escape") ? "Esc" : key;
        if (editMapKeyEnable && !displayText.equals(currentResetKey) && !displayText.equals(currentPauseKey)) {
            controlSettings.get("mouse keyboard").get("map").set(key); // update the local copy
            mapKeyButton.getLabel().setText(displayText);
            editMapKeyEnable = false;
        } else if (editResetKeyEnable && !displayText.equals(currentMapKey) && !displayText.equals(currentPauseKey) ) {
            controlSettings.get("mouse keyboard").get("reset").set(key); // update the local copy
            resetKeyButton.getLabel().setText(displayText);
            editResetKeyEnable = false;
        } else if (editPauseKeyEnable && !displayText.equals(currentMapKey) && !displayText.equals(currentResetKey) ) {
            controlSettings.get("mouse keyboard").get("pause").set(key); // update the local copy
            pauseKeyButton.getLabel().setText(displayText);
            editPauseKeyEnable = false;
        }
        updateKeyButtonAppearance();
        FileHandle file = Gdx.files.local("input_settings.json"); // update json copy
        String jsonString = controlSettings.prettyPrint(JsonWriter.OutputType.json, 1);
        file.writeString(jsonString, false);
        return false;
    }

    /**
     * Called when a key was released (Unsupported)
     *
     * @param keycode one of the constants in {@link Input.Keys}
     * @return whether the input was processed
     */
    @Override
    public boolean keyUp(int keycode) { return false; }

    /**
     * Called when a key was typed (Unsupported)
     *
     * @param character The character
     * @return whether the input was processed
     */
    @Override
    public boolean keyTyped(char character) { return false; }

    /**
     * Called when the screen was touched or a mouse button was pressed. (Unsupported)
     *
     * @param screenX The x coordinate, origin is in the upper left corner
     * @param screenY The y coordinate, origin is in the upper left corner
     * @param pointer the pointer for the event.
     * @param button  the button
     * @return whether the input was processed
     */
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }

    /**
     * Called when a finger was lifted or a mouse button was released. (Unsupported)
     *
     * @param screenX
     * @param screenY
     * @param pointer the pointer for the event.
     * @param button  the button
     * @return whether the input was processed
     */
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

    /**
     * Called when a finger or the mouse was dragged. (Unsupported)
     *
     * @param screenX
     * @param screenY
     * @param pointer the pointer for the event.
     * @return whether the input was processed
     */
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

    /**
     * Called when the mouse was moved without any buttons being pressed. Will not be called on iOS. (Unsupported)
     *
     * @param screenX
     * @param screenY
     * @return whether the input was processed
     */
    @Override
    public boolean mouseMoved(int screenX, int screenY) { return false; }

    /**
     * Called when the mouse wheel was scrolled. Will not be called on iOS. (Unsupported)
     *
     * @param amountX the horizontal scroll amount, negative or positive depending on the direction the wheel was scrolled.
     * @param amountY the vertical scroll amount, negative or positive depending on the direction the wheel was scrolled.
     * @return whether the input was processed.
     */
    @Override
    public boolean scrolled(float amountX, float amountY) { return false; }
}
