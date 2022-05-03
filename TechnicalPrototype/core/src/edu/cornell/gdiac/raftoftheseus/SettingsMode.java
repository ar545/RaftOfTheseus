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
    /** Texture for the WASD icon */
    private Texture wasdIcon;
    /** Texture for the arrows icon */
    private Texture arrrowsIcon;
    /** Texture for the key (pt 1) */
    private Texture keysIcon1;
    /** Texture for the key (pt 2) */
    private Texture keysIcon2;

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
        skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
    }

    /**
     * Populates this mode from given the directory
     *
     * @param directory Reference to the asset directory
     */
    public void populate(AssetDirectory directory) {
        background = directory.getEntry("sea_background", Texture.class);
        wasdIcon = directory.getEntry("settings_wasd", Texture.class);
        arrrowsIcon = directory.getEntry("settings_arrows", Texture.class);
        keysIcon1 = directory.getEntry("settings_keys_1", Texture.class);
        keysIcon2 = directory.getEntry("settings_keys_2", Texture.class);
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
        TextButton menuButton = UICreator.createTextButton("BACK", skin, Color.GOLD);
        menuButton.addListener(UICreator.createListener(menuButton, Color.GRAY, Color.GOLD, this::setExitPressed));
        part1.add(menuButton).expandX().align(Align.left).padRight(1500).padTop(10);
        table.add(part1);
        table.row();

        Table part2 = new Table();
        part2.add(UICreator.createLabel("SETTINGS", skin, 0.6f)).expandX().align(Align.center);
        table.add(part2).padTop(-50);
        table.row();

        Table part3 = new Table();
        part3.add(UICreator.createLabel("VOLUME", skin, 0.5f)).padLeft(80).expandX().align(Align.left);
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

        part3.add(UICreator.createLabel("KEYBOARD SCHEME", skin, 0.5f)).padLeft(80).expandX().align(Align.left);
        part3.row();

        Image wasdImage = new Image(wasdIcon);
        Image arrowsImage = new Image(arrrowsIcon);
        part3.add(wasdImage).size(212, 130).expandX().padLeft(100).align(Align.left);
        part3.add(arrowsImage).size(212, 130).align(Align.left).padLeft(-200);
        part3.row();

        part3.add(UICreator.createLabel("KEYBOARD SHORTCUTS", skin, 0.5f)).padLeft(80);
        part3.row();

        Image keysImage1 = new Image(keysIcon1);
        part3.add(keysImage1).size(412, 78).padLeft(100).align(Align.left).expandX();
        Image keysImage2 = new Image(keysIcon2);
        part3.add(keysImage2).size(412, 78).expandX().align(Align.left).padLeft(-80);;
        table.add(part3);
        table.row();
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
