package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.*;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;

import java.text.DecimalFormat;

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

    /** Standard window size (for scaling) */
    private static int STANDARD_WIDTH  = 800;
    /** Standard window height (for scaling) */
    private static int STANDARD_HEIGHT = 700;
    /** Scaling factor. */
    private float scale;
    /** The height of the canvas window (necessary since sprite origin != screen origin) */
    private int heightY;
    /** Exit button clicked */
    private boolean exitPressed;
    /** Previous screen (4 - start, 5 - menu, 6 - world) */
    private int previousMode;
    /** Music volume */
    private float musicVolume;
    /** Sound effects volume */
    private float soundEffectsVolume;
    /** Whether there is a back to menu button */
    private boolean isBackMenu;
    /** Menu pressed */
    private boolean menuPressed;
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
        menuPressed = false;
        SoundController.getInstance().startLevelMusic();
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

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to request to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    /** Sets is back to menu boolean */
    public void setIsBackMenu(boolean value) { this.isBackMenu = value; }

    /** Sets exit code to menu */
    public void setExitMenu(int value) { this.EXIT_MENU = value; }

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
        stage = new Stage();
        skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
        table = new Table(skin);
        buildSettings();
    }

    /**
     * Constructs the view
     */
    private void buildSettings() {
        Gdx.input.setInputProcessor(stage);

        stage.addActor(table);
        table.align(Align.top);
        table.setFillParent(true);

        skin.add("background", background);
        table.setBackground(skin.getDrawable("background"));

        TextButton menuButton = new TextButton("MENU", skin);
        menuButton.getLabel().setFontScale(0.35f);
        menuButton.getLabel().setColor(Color.GOLD);
        menuButton.addListener(new ClickListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                menuButton.getLabel().setColor(Color.GRAY);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                menuButton.getLabel().setColor(Color.GOLD);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                exitPressed = true;
            }
        });
        table.add(menuButton).expandX().align(Align.left).expandX().align(Align.left).padLeft(60).padTop(10);
        table.row();

        Label titleLabel = new Label("SETTINGS", skin);
        titleLabel.setFontScale(0.6f);
        table.add(titleLabel).expandX().align(Align.left).padLeft(100);
        table.row();

        Label volumeLabel = new Label("VOLUME", skin);
        volumeLabel.setFontScale(0.4f);
        table.add(volumeLabel).padLeft(100).expandX().align(Align.left);
        table.row();

        Label musicLabel = new Label("MUSIC", skin);
        musicLabel.setFontScale(0.35f);
        table.add(musicLabel).padLeft(120).expandX().align(Align.left);

        Drawable sliderKnobDrawable = new TextureRegionDrawable(new TextureRegion(sliderKnob));
        Drawable sliderBarDrawable = new TextureRegionDrawable(new TextureRegion(sliderBar));
        SliderStyle sliderStyle = new SliderStyle(sliderBarDrawable, sliderKnobDrawable);

        musicVolume = SoundController.getInstance().getMasterMusicVolume() * 1000f;
        Label musicValueLabel = new Label(String.valueOf((int) Math.floor(musicVolume)), skin);
        musicValueLabel.setFontScale(0.35f);

        musicSlider = new Slider(0, 100, 1, false, sliderStyle);
        musicSlider.setValue(musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                musicVolume = musicSlider.getValue();
                SoundController.getInstance().setMasterMusicVolume(musicVolume / 1000);
                musicValueLabel.setText((int) Math.floor(musicVolume));
            }
        });
        stage.addActor(musicSlider);
        table.add(musicSlider).expandX().align(Align.left).width(300);
        table.add(musicValueLabel).expandX().align(Align.left).width(100).padLeft(30);
        table.row();

        soundEffectsVolume = SoundController.getInstance().getMasterSFXVolume() * 100;
        Label soundEffectsLabel = new Label("SOUND EFFECTS", skin);
        soundEffectsLabel.setFontScale(0.35f);
        table.add(soundEffectsLabel).padLeft(120).align(Align.left);
        Label soundEffectsValueLabel = new Label(String.valueOf((int) Math.floor(soundEffectsVolume)), skin);
        soundEffectsValueLabel.setFontScale(0.35f);
        soundEffectsSlider = new Slider(0, 100, 1, false, sliderStyle);
        soundEffectsSlider.setValue(soundEffectsVolume);
        soundEffectsSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                soundEffectsVolume = soundEffectsSlider.getValue();
                SoundController.getInstance().setMasterSfxVolume(soundEffectsVolume / 100);
                soundEffectsValueLabel.setText((int) Math.floor(soundEffectsVolume));
            }
        });
        stage.addActor(soundEffectsSlider);
        table.add(soundEffectsSlider).expandX().align(Align.left).width(300);
        table.add(soundEffectsValueLabel).align(Align.left).width(100).padLeft(30);
        table.row();

        Label keyboardSchemeLabel = new Label("KEYBOARD SCHEME", skin);
        keyboardSchemeLabel.setFontScale(0.4f);
        table.add(keyboardSchemeLabel).padLeft(100).expandX().align(Align.left);
        table.row();

        Image wasdImage = new Image(wasdIcon);
        Image arrowsImage = new Image(arrrowsIcon);
        table.add(wasdImage).expandX().padLeft(120).align(Align.left);
        table.add(arrowsImage).align(Align.left).padLeft(-100);
        table.row();

        Label keyboardShortcutLabel = new Label("KEYBOARD SHORTCUTS", skin);
        keyboardShortcutLabel.setFontScale(0.4f);
        table.add(keyboardShortcutLabel).padLeft(100);
        table.row();

        Image keysImage1 = new Image(keysIcon1);
        table.add(keysImage1).padLeft(120).align(Align.left).expandX();
        Image keysImage2 = new Image(keysIcon2);
        table.add(keysImage2).expandX();
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
        stage.act();
        stage.draw();
    }

    /**
     * Called when the Screen should render itself.
     */
    public void render(float delta) {
        Gdx.input.setInputProcessor(stage);
        if (active) {
            draw();
            if (exitPressed || InputController.getInstance().didExit()) {
                listener.exitScreen(this, previousMode);
            } else if (menuPressed) {
                listener.exitScreen(this, EXIT_MENU);
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

    /** Reset the settings menu and exit pressed state */
    public void resetPressedState() {
        menuPressed = false;
        exitPressed = false;
    }
}
