package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.raftoftheseus.singleton.InputController;
import edu.cornell.gdiac.raftoftheseus.singleton.SfxController;
import edu.cornell.gdiac.util.ScreenListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.*;

public class SettingsMode implements Screen {

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
    /** Background for single key */
    private Texture singleKeyBackground;
    /** Background for short text key  */
    private Texture shortTextKeyBackground;
    /** Background for long text key */
    private Texture longTextKeyBackground;

    /** TextButton for accessibility */
    TextButton accessibilityButton;

    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

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
        accessibilityModeActive = true;
        skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
    }

    /**
     * Populates this mode from given the directory
     *
     * @param directory Reference to the asset directory
     */
    public void populate(AssetDirectory directory) {
        background = directory.getEntry("sea_background", Texture.class);
        singleKeyBackground = directory.getEntry("single_key_background", Texture.class);
        shortTextKeyBackground = directory.getEntry("short_text_key_background", Texture.class);
        longTextKeyBackground = directory.getEntry("long_text_key_background", Texture.class);
        sliderKnob = directory.getEntry("slider_knob", Texture.class);
        sliderBar = directory.getEntry("slider_bar", Texture.class);
    }

    /** Sets the ScreenListener for this mode, the ScreenListener will respond to request to quit. */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    /** Sets exit code to menu */
    public void setExitMenu(int value) { this.EXIT_MENU = value; }

    /** @param previousMode the previous mode value to be set */
    public void setPreviousMode(int previousMode) { this.previousMode = previousMode; }

    /** Called when this screen becomes the current screen. */
    public void show() {
        active = true;
        stage = new Stage();
        table = new Table(skin);
        buildSettings();
    }

    /** Called when this screen is no longer the current screen for a Game. */
    public void hide() {
        active = false;
    }

    public void initGUI(){

    }

    /** Constructs the view. */
    private void buildSettings() {
        Gdx.input.setInputProcessor(stage);

        stage.addActor(table);
        table.align(Align.top);
        table.setFillParent(true);

        skin.add("background", background);
        table.setBackground(skin.getDrawable("background"));

        Table part1 = new Table();
        part1.align(Align.left);
        TextButton menuButton = UICreator.createTextButton("BACK", skin, Color.WHITE);
        menuButton.addListener(UICreator.createListener(menuButton, Color.GOLD, Color.WHITE, this::setExitPressed));
        part1.add(menuButton).expandX().align(Align.left).padRight(1500).padTop(10);
        table.add(part1);
        table.row();

        Table part2 = new Table();
        part2.add(UICreator.createLabel("SETTINGS", skin, 0.85f)).expandX().align(Align.center);
        table.add(part2).padTop(-50);
        table.row();

        Table part3 = new Table();
        part3.add(UICreator.createLabel("VOLUME", skin, 0.6f)).padLeft(80).expandX().align(Align.left);
        part3.row();
        part3.add(UICreator.createLabel("MUSIC", skin, 0.4f)).padLeft(100).expandX().align(Align.left);

        Drawable sliderKnobDrawable = new TextureRegionDrawable(new TextureRegion(sliderKnob));
        Drawable sliderBarDrawable = new TextureRegionDrawable(new TextureRegion(sliderBar));
        SliderStyle sliderStyle = new SliderStyle(sliderBarDrawable, sliderKnobDrawable);

        musicVolume = SfxController.getInstance().getMasterMusicVolume() * 100f;
        Label musicValueLabel = new Label(String.valueOf((int) Math.floor(musicVolume)), skin);
        musicValueLabel.setFontScale(0.4f);

        musicSlider = new Slider(0, 100, 1, false, sliderStyle);
        musicSlider.setValue(musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                musicVolume = musicSlider.getValue();
                SfxController.getInstance().setMasterMusicVolume(musicVolume / 100);
                musicValueLabel.setText((int) Math.floor(musicVolume));
            }
        });
        stage.addActor(musicSlider);
        part3.add(musicSlider).expandX().align(Align.left).width(600).padLeft(-100);
        part3.add(musicValueLabel).expandX().align(Align.left).width(80).padLeft(60);
        part3.row();

        soundEffectsVolume = SfxController.getInstance().getMasterSFXVolume() * 100;
        part3.add(UICreator.createLabel("SOUND EFFECTS", skin, 0.4f)).padLeft(100).align(Align.left);

        Label soundEffectsValueLabel = UICreator.createLabel(String.valueOf((int) Math.floor(soundEffectsVolume)), skin, 0.4f);
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

        part3.add(UICreator.createLabel("ACCESSIBILITY", skin, 0.6f)).padLeft(80).expandX().align(Align.left);
        part3.row();

        Table part3a = new Table();
        part3a.add(UICreator.createLabel("ACCESSIBILITY MODE", skin, 0.4f));
        accessibilityButton = UICreator.createTextButton(accessibilityModeActive ? "ON" : "OFF", skin, 0.4f, Color.WHITE, shortTextKeyBackground);
        accessibilityButton.addListener(UICreator.createListener(accessibilityButton, Color.GRAY, Color.WHITE, this::changeAccessibilityMode));
        part3a.add(accessibilityButton).padLeft(30).width(100);
        part3.add(part3a).padLeft(-30);
        part3.row();

        part3.add(UICreator.createLabel("KEYBOARD SHORTCUTS", skin, 0.6f)).padLeft(80);
        part3.row();
        table.add(part3).padTop(-50);
        table.row();

        Table part4 = new Table();
        int keysPadding = 30;
        part4.add(UICreator.createLabel("MAP", skin, 0.4f)).padRight(keysPadding);
        TextButton mapKeyButton = UICreator.createTextButton("M", skin, 0.4f, Color.WHITE, singleKeyBackground);
        mapKeyButton.addListener(UICreator.createListener(mapKeyButton, Color.GRAY, Color.WHITE, this::changeKeyMapping));
        part4.add(mapKeyButton).padRight(keysPadding);

        part4.add(UICreator.createLabel("FIRE", skin, 0.4f)).padRight(keysPadding);
        TextButton fireKeyButton = UICreator.createTextButton("left mouse", skin, 0.4f, Color.WHITE, longTextKeyBackground);
        fireKeyButton.addListener(UICreator.createListener(fireKeyButton, Color.GRAY, Color.WHITE, this::changeKeyMapping));
        part4.add(fireKeyButton).padRight(keysPadding);

        part4.add(UICreator.createLabel("RESTART", skin, 0.4f)).padRight(keysPadding);
        TextButton restartKeyButton = UICreator.createTextButton("R", skin, 0.4f, Color.WHITE, singleKeyBackground);
        restartKeyButton.addListener(UICreator.createListener(restartKeyButton, Color.GRAY, Color.WHITE, this::changeKeyMapping));
        part4.add(restartKeyButton).padRight(keysPadding);

        part4.add(UICreator.createLabel("PAUSE", skin, 0.4f)).padRight(keysPadding);
        TextButton pauseKeyButton = UICreator.createTextButton("Esc", skin, 0.4f, Color.WHITE, shortTextKeyBackground);
        pauseKeyButton.addListener(UICreator.createListener(pauseKeyButton, Color.GRAY, Color.WHITE, this::changeKeyMapping));
        part4.add(pauseKeyButton).padRight(keysPadding);
        table.add(part4);
        table.row();
    }

    /** Change the appropriate key mapping */
    private void changeKeyMapping() {
        System.out.println("TODO : change key mapping");
    }

    /** Set accessibility mode */
    private void changeAccessibilityMode() {
        // TODO
        accessibilityModeActive = !accessibilityModeActive;
        accessibilityButton.setText(accessibilityModeActive ? "ON" : "OFF");
    }



    /** Draw the status of this player mode. */
    private void draw() {
        stage.act();
        stage.draw();
    }

    /** Called when the Screen should render itself. */
    public void render(float delta) {
        Gdx.input.setInputProcessor(stage);
        if (active) {
            draw();
            if (exitPressed || InputController.getInstance().didExit()) {
                resetExitPressed();
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
    }

    /** Reset the settings menu and exit pressed state */
    private void setExitPressed() { exitPressed = true; }
    private void resetExitPressed() { exitPressed = false; }

    /** Called when the Screen is paused. */
    public void pause() {}
    /** Called when the Screen is resumed from a paused state. */
    public void resume() {}
    /** Called when this screen should release all resources. */
    public void dispose() { }

}
