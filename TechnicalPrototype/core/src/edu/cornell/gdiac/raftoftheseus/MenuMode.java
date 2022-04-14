package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;
import org.w3c.dom.Text;

/**
 * Class that provides the menu screen for the state of the game.
 * */
public class MenuMode implements Screen {

    /** An enum representing which screen is currently active within the menu system */
    private static enum MenuScreen {
        TITLE, // title screen, with buttons leading to the other screens
        LEVEL_SELECT, // level select screen, with buttons that lead to levels
        SETTINGS, // settings screen, where the player can change game settings
        CREDITS // credits screen
    }
    /** Which screen is active */
    private MenuScreen currentScreen;

    // https://stackoverflow.com/questions/31794636/clickable-buttons-using-libgdx
    private Stage stage;
    private Skin skin;

    /** Background texture for menu */
    private Texture menuBackground;
    /** Font for drawing text */
    private BitmapFont displayFont;
    /** Background texture for menu */
    private Texture levelSelectBackground;
    /** Background texture for credits */
    private Texture seaBackground;
    /** Title texture */
    private Texture title;
    /** Texture for the levels to select. */
    private Texture[] levels;

    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    // Load constants
    public static void setConstants(JsonValue objParams){
        JsonValue menuParams = objParams.getChild("menu");
        STANDARD_WIDTH = menuParams.getInt("width", 800);
        STANDARD_HEIGHT = menuParams.getInt("height", 700);
        BUTTON_SCALE = menuParams.getFloat("button scale", 1.25f);
        PADDING_X = menuParams.getInt("padding x", 100);
        PADDING_Y = menuParams.getInt("padding y", 100);
        NUM_COLS = menuParams.getInt("level columns", 5);
        LEVEL_COUNT = menuParams.getInt("level count", 9);
    }

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
    private static int colPadding = 25;
    /** Scaling factor. */
    private float scale;
    /** The height of the canvas window (necessary since sprite origin != screen origin) */
    private int heightY;

    /** Whether the player has pressed a level select button */
    private boolean isLevelPressed;
    /** Level selected */
    private int selectedLevel;
    /** Whether this player mode is still active */
    private boolean active;
    /** Level count **/
    private static int LEVEL_COUNT = 9;
    /** Exit code for displaying settings */
    public static int EXIT_SETTINGS = 8;
    /** Whether the settings button was pressed on the main menu */
    private boolean settingsPressed;

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
        isLevelPressed = false;
        selectedLevel = -1;
        settingsPressed = false;

        currentScreen = MenuScreen.TITLE;
    }

    /**
     * Populates this mode from the given the directory.
     *
     * @param directory Reference to the asset directory.
     */
    public void populate(AssetDirectory directory) {
        menuBackground = directory.getEntry("menu_background", Texture.class);
        displayFont = directory.getEntry( "diogenes", BitmapFont.class);
        seaBackground = directory.getEntry("sea_background", Texture.class);
        levelSelectBackground = directory.getEntry("level_background", Texture.class);
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
        return isLevelPressed;
    }

    /**
     * Called when this screen becomes the current screen.
     */
    public void show() {
        active = true;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
        buildMenu();
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        active = false;
        stage = null;
        skin = null;
    }

    /**
     * Draw the status of this player mode.
     */
    private void draw() {
        colPadding = ((canvas.getWidth() - 2 * PADDING_X - NUM_COLS * levels[0].getWidth()) / (NUM_COLS - 1));

        canvas.begin();
        canvas.clear();
        canvas.drawBackground(currentScreen == MenuScreen.TITLE ? menuBackground : (currentScreen == MenuScreen.CREDITS) ? seaBackground : levelSelectBackground, true);
        canvas.end();

        stage.act();
        stage.draw();
    }

    /**
     * Called when the Screen should render itself.
     */
    public void render(float delta) {
        InputController input = InputController.getInstance();
        input.readInput();
        if (active) {
            draw();
            if (input.didSettings() || settingsPressed) {
                listener.exitScreen(this, EXIT_SETTINGS);
            } else if (isReady() && listener != null) {
                listener.exitScreen(this, 0);
            }
        }
    }

    private void buildMenu(){
        Table menuTable = new Table(skin);
        menuTable.setPosition(0, 0);
//        menuTable.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        menuTable.setFillParent(true);
        menuTable.align(Align.center);

        // instantiate the "back" button, which is used in multiple menus
        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                changeScreenTo(MenuScreen.TITLE);
            }
        });

        switch(currentScreen) {
            case TITLE:
                //Create buttons
                skin.add("font", displayFont);
                TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
                buttonStyle.font = skin.getFont("font");

                TextButton playButton = new TextButton("Play", buttonStyle);
                playButton.getLabel().setFontScale(0.5f);
                TextButton optionsButton = new TextButton("Options", buttonStyle);
                optionsButton.getLabel().setFontScale(0.5f);
                TextButton creditsButton = new TextButton("Credits", buttonStyle);
                creditsButton.getLabel().setFontScale(0.5f);
                TextButton exitButton = new TextButton("Exit", buttonStyle);
                exitButton.getLabel().setFontScale(0.5f);

                //Add listeners to buttons
                playButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        changeScreenTo(MenuScreen.LEVEL_SELECT);
                    }
                });
                optionsButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        settingsPressed = true;
                    }
                });
                creditsButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        changeScreenTo(MenuScreen.CREDITS);
                    }
                });
                exitButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Gdx.app.exit();
                    }
                });

                //Add buttons to table
                menuTable.add(playButton).padTop(450).expandX().align(Align.left).padLeft(100);
                menuTable.row();
                menuTable.add(optionsButton).expandX().align(Align.left).padLeft(100);
                menuTable.row();
                menuTable.add(creditsButton).expandX().align(Align.left).padLeft(100);
                menuTable.row();
                menuTable.add(exitButton).expandX().align(Align.left).padLeft(100);
                break;
            case LEVEL_SELECT:
                //Create buttons
                for (int i = 0; i < LEVEL_COUNT; i ++) {
                    ImageButton levelButton = new ImageButton(new TextureRegionDrawable(levels[i]));
                    int finalI = i;
                    levelButton.addListener(new ClickListener(){
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            selectlevel(finalI);
                        }
                    });
                    //Add button to table
                    if (i > 0 && i % NUM_COLS == 0)
                        menuTable.row().padTop(colPadding);
                    menuTable.add(levelButton).padLeft(i % NUM_COLS > 0 ? colPadding : 0);
                }
                menuTable.row();
                menuTable.add(backButton);
                break;
            case SETTINGS:
                //Create buttons
                TextButton fooButton = new TextButton("Foo", skin);
                fooButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        System.out.println("pressed foo");
                        // TODO change some settings here
                    }
                });
                //Add buttons to table
                menuTable.add(fooButton);
                menuTable.row();
                menuTable.add(backButton);
                break;
            case CREDITS:
                // TODO change font & structure
                skin.add("font", displayFont);
                Label.LabelStyle textStyle = new Label.LabelStyle();
                textStyle.font = skin.getFont("font");
                textStyle.fontColor = Color.WHITE;

                // BitmapFont font = new BitmapFont(Gdx.files.internal("Calibri.fnt"),Gdx.files.internal("Calibri.png"),false);
                Label creditsLabel = new Label("CREDITS", textStyle);
                creditsLabel.setFontScale(0.75f);
                menuTable.add(creditsLabel);
                menuTable.row();

                Label programmersLabel = new Label("PROGRAMMERS", textStyle);
                programmersLabel.setFontScale(0.55f);
                menuTable.add(programmersLabel).expandX().align(Align.left).padLeft(100);

                Label designersLabel = new Label("DESIGNERS", textStyle);
                designersLabel.setFontScale(0.55f);
                menuTable.add(designersLabel).expandX().align(Align.right).padRight(100);
                menuTable.row();

                Label amy = new Label("AMY HUANG", textStyle);
                amy.setFontScale(0.35f);
                menuTable.add(amy);

                Label gloria = new Label("GLORIA SHI", textStyle);
                amy.setFontScale(0.35f);
                menuTable.add(amy);
                menuTable.row();

                Label demian = new Label("DEMIAN YUTIN", textStyle);
                demian.setFontScale(0.35f);
                menuTable.add(demian);

                Label noah = new Label("NOAH BRAUN", textStyle);
                noah.setFontScale(0.35f);
                menuTable.add(noah);
                menuTable.row();

                Label howard = new Label("HOWARD FU", textStyle);
                howard.setFontScale(0.35f);
                menuTable.add(howard);

                Label spencer = new Label("SPENCEER PETTEE", textStyle);
                spencer.setFontScale(0.35f);
                menuTable.add(spencer);
                menuTable.row();

                Label jaden = new Label("JADEN O'BRIEN", textStyle);
                jaden.setFontScale(0.35f);
                menuTable.add(jaden);
                menuTable.row();

                Label jason = new Label("JASON TUNG", textStyle);
                jason.setFontScale(0.35f);
                menuTable.add(jason);
                menuTable.row();

                Label leo = new Label("LEO ZHAO", textStyle);
                leo.setFontScale(0.35f);
                menuTable.add(leo);
                menuTable.row();
                // System.out.println("Got to credits screen");
                menuTable.add(backButton);
                break;
        }

        stage.addActor(menuTable);
    }

    private void selectlevel(int id) {
        if (levels != null && !isLevelPressed) {
            isLevelPressed = true;
            selectedLevel = id;
        }
    }

    private void changeScreenTo(MenuScreen targetScreen) {
        stage.clear();
        currentScreen = targetScreen;
        buildMenu();
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

    /** Reset the level pressed state */
    public void resetPressedState(){ isLevelPressed = false; }

    /** Reset the settings pressed state */
    public void resetSettingsState() { settingsPressed = false; }

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
}