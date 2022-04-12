package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.*;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;

public class SettingsMode implements Screen {

    private Stage stage;
    private Table table;
    private Skin skin;

    /** Background texture */
    private Texture background;
    /** Title texture */
    private Texture title;
    /** Texture for the exit button */
    private Texture exitButton;
    /** Font for drawing text */
    private BitmapFont displayFont;
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
    /** Exit code to display menu screen */
    private int EXIT_MENU;
    /** Menu pressed */
    private boolean menuPressed;

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
        title = directory.getEntry("settings_title", Texture.class);
        exitButton = directory.getEntry("exit_button", Texture.class);
        wasdIcon = directory.getEntry("settings_wasd", Texture.class);
        arrrowsIcon = directory.getEntry("settings_arrows", Texture.class);
        keysIcon = directory.getEntry("settings_keys", Texture.class);
        displayFont = directory.getEntry( "diogenes", BitmapFont.class);
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
        skin = new Skin();
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
        skin.add("title", title);
        skin.add("font", displayFont);

        table.setBackground(skin.getDrawable("background"));

        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = skin.getFont("font");
        buttonStyle.fontColor = Color.GOLD;

        if (isBackMenu) {
            TextButton menuButton = new TextButton("MENU", buttonStyle);
            menuButton.getLabel().setFontScale(0.5f);
            menuButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    menuPressed = true;
                }
            });
            table.add(menuButton).padLeft(100).padTop(10).expandX().align(Align.left);
        }

        TextButton exitButton = new TextButton("EXIT", buttonStyle);
        exitButton.getLabel().setFontScale(0.5f);
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                exitPressed = true;
            }
        });
        table.add(exitButton).padLeft(isBackMenu ? 0 : 100).padRight(isBackMenu ? -50 : 0).padTop(10).expandX().align(isBackMenu ? Align.right: Align.left);
        table.row();

        LabelStyle textStyle = new LabelStyle();
        textStyle.font = skin.getFont("font");
        textStyle.fontColor = Color.WHITE;

        Label titleLabel = new Label("SETTINGS", textStyle);
        titleLabel.setFontScale(0.75f);
        table.add(titleLabel);
        table.row();

        Label volumeLabel = new Label("VOLUME", textStyle);
        volumeLabel.setFontScale(0.45f);
        table.add(volumeLabel).padLeft(100).expandX().align(Align.left);
        table.row();

        Label musicLabel = new Label("MUSIC", textStyle);
        musicLabel.setFontScale(0.35f);
        table.add(musicLabel).padLeft(120).expandX().align(Align.left);

        Drawable sliderKnobDrawable = new TextureRegionDrawable(new TextureRegion(sliderKnob));
        Drawable sliderBarDrawable = new TextureRegionDrawable(new TextureRegion(sliderBar));
        sliderBarDrawable.setMinWidth(0);
        sliderBarDrawable.setMinHeight(10);
        SliderStyle sliderStyle = new SliderStyle(sliderBarDrawable, sliderKnobDrawable);

        musicVolume = SoundController.getInstance().getMasterMusicVolume() * 1000f;
        Label musicValueLabel = new Label(String.valueOf(musicVolume), textStyle);
        musicValueLabel.setFontScale(0.35f);

        musicSlider = new Slider(0, 100, 1, false, sliderStyle);
        musicSlider.setValue(musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                musicVolume = musicSlider.getValue();
                SoundController.getInstance().setMasterMusicVolume(musicVolume / 1000);
                musicValueLabel.setText(String.valueOf(musicVolume));
            }
        });
        stage.addActor(musicSlider);
        table.add(musicSlider).expandX().align(Align.left).width(370);
        table.add(musicValueLabel).expandX().align(Align.left).width(100).padLeft(30);
        table.row();

        soundEffectsVolume = SoundController.getInstance().getMasterSFXVolume() * 100f;
        Label soundEffectsLabel = new Label("SOUND EFFECTS", textStyle);
        soundEffectsLabel.setFontScale(0.35f);
        table.add(soundEffectsLabel).padLeft(120).align(Align.left);
        Label soundEffectsValueLabel = new Label(String.valueOf(soundEffectsVolume), textStyle);
        soundEffectsValueLabel.setFontScale(0.35f);
        soundEffectsSlider = new Slider(0, 100, 1, false, sliderStyle);
        soundEffectsSlider.setValue(soundEffectsVolume);
        soundEffectsSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                soundEffectsVolume = soundEffectsSlider.getValue();
                SoundController.getInstance().setMasterSfxVolume(soundEffectsVolume / 100);
                soundEffectsValueLabel.setText(String.valueOf(soundEffectsVolume));
            }
        });
        stage.addActor(soundEffectsSlider);
        table.add(soundEffectsSlider).expandX().align(Align.left).width(370);
        table.add(soundEffectsValueLabel).align(Align.left).width(100).padLeft(30);
        table.row();

        Label keyboardScehmeLabel = new Label("KEYBOARD SCHEME", textStyle);
        keyboardScehmeLabel.setFontScale(0.45f);
        table.add(keyboardScehmeLabel).padLeft(100).expandX().align(Align.left);
        table.row();

        Image wasdImage = new Image(wasdIcon);
        Image arrowsImage = new Image(arrrowsIcon);
        table.add(wasdImage).expandX().padLeft(120).align(Align.left);
        table.add(arrowsImage).align(Align.left).padLeft(-180);
        table.row();

        Label keyboardShortcutLabel = new Label("KEYBOARD SHORTCUTS", textStyle);
        keyboardShortcutLabel.setFontScale(0.45f);
        table.add(keyboardShortcutLabel).padLeft(100);
        table.row();

        Image keysImage = new Image(keysIcon);
        table.add(keysImage).padLeft(120).align(Align.left);
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
