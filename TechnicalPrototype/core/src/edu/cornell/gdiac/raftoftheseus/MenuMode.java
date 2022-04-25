package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.raftoftheseus.singleton.InputController;
import edu.cornell.gdiac.raftoftheseus.singleton.SfxController;
import edu.cornell.gdiac.util.ScreenListener;

import static edu.cornell.gdiac.raftoftheseus.GDXRoot.MENU_TO_SETTINGS;

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
    /** Background texture for credits */
    private Texture seaBackground;
    /** Textures for level buttons */
    private Texture[] levelButtonImages;
    /** Texture for the levels to select. */
    private TextButton[] levelButtons;
    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;
    /** Game level save data */
    private static JsonValue saveData;

    /** Loads constants from screen */
    public static void setConstants(JsonValue objParams){
        STANDARD_WIDTH = objParams.getInt(0);
        STANDARD_HEIGHT = objParams.getInt(1);
        NUM_COLS = objParams.getInt(2);
        colPadding = objParams.getInt("column padding", 25);
        LEVEL_COUNT = objParams.getInt("level count", 9);
    }

    /** Standard window size (for scaling) */
    private static int STANDARD_WIDTH;
    /** Standard window height (for scaling) */
    private static int STANDARD_HEIGHT;
    /** Number of levels in each row */
    private static int NUM_COLS;
    /** Padding between columns */
    private static int colPadding;
    /** Scaling factor. */
    private float scale;
    /** The height of the canvas window (necessary since sprite origin != screen origin) */
    private int heightY;
    /** The color for inactive buttons */
    private Color inactiveColor = Color.WHITE;
    /** The color for hovered buttons */
    private Color activeColor = Color.GOLD;

    /** Whether the player has pressed a level select button */
    private boolean isLevelPressed;
    /** Level selected */
    private int selectedLevel;
    /** Whether this player mode is still active */
    private boolean active;
    /** Level count **/
    private static int LEVEL_COUNT;
    /** Whether the play button was pressed on the main menu */
    private boolean playPressed;
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
        selectedLevel = 0;
        settingsPressed = false;
        playPressed = false;
        currentScreen = MenuScreen.TITLE;
    }

    /**
     * Populates this mode from the given the directory.
     *
     * @param directory Reference to the asset directory.
     */
    public void populate(AssetDirectory directory) {
        menuBackground = directory.getEntry("menu_background", Texture.class);
        seaBackground = directory.getEntry("sea_background", Texture.class);
        levelButtonImages = new Texture[5];
        for (int i = 0; i < 4; i++) {
            levelButtonImages[i] = directory.getEntry("level_star_" + i, Texture.class);
        }
        levelButtonImages[4] = directory.getEntry("level_locked", Texture.class);
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    /** Sets the save data */
    public void setSaveData(JsonValue saveData) { this.saveData = saveData; }

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
        canvas.begin();
        canvas.clear();

        switch (currentScreen) {
            case TITLE:
                canvas.drawBackground(menuBackground, true);
                break;
            default:
                canvas.drawBackground(seaBackground, true);
                break;
        }
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
            if (settingsPressed) {
                resetSettingsState();
                listener.exitScreen(this, MENU_TO_SETTINGS);
            } else if ((isReady() && listener != null) || playPressed) {
                resetPressedState();
                resetPlayState();
                currentScreen = MenuScreen.TITLE;
                listener.exitScreen(this, 0);
            }
        }
    }

    private void buildMenu(){
        Table menuTable = new Table(skin);
        menuTable.setFillParent(true);
        menuTable.align(Align.top);

        // instantiate the "back" button, which is used in multiple menus
        TextButton menuButton = new TextButton("BACK", skin);
        menuButton.getLabel().setFontScale(0.35f);
        menuButton.getLabel().setColor(Color.GOLD);
        menuButton.addListener(new ClickListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
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
                SfxController.getInstance().playSFX("button_click");
                changeScreenTo(MenuScreen.TITLE);
            }
        });

        switch(currentScreen) {
            case TITLE:
                // Create buttons
                TextButton startButton = new TextButton("START", skin);
                startButton.getLabel().setFontScale(0.35f);
                TextButton levelsButton = new TextButton("LEVELS", skin);
                levelsButton.getLabel().setFontScale(0.35f);
                TextButton settingsButton = new TextButton("SETTINGS", skin);
                settingsButton.getLabel().setFontScale(0.35f);
                TextButton creditsButton = new TextButton("CREDITS", skin);
                creditsButton.getLabel().setFontScale(0.35f);

                //Add listeners to buttons
                startButton.addListener(new ClickListener(){
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                        super.enter(event, x, y, pointer, fromActor);
                        startButton.getLabel().setColor(activeColor);
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        super.exit(event, x, y, pointer, toActor);
                        startButton.getLabel().setColor(inactiveColor);
                    }

                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        SfxController.getInstance().playSFX("raft_sail_open");
                        playPressed = true;
                    }
                });
                levelsButton.addListener(new ClickListener(){
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                        super.enter(event, x, y, pointer, fromActor);
                        levelsButton.getLabel().setColor(activeColor);
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        super.exit(event, x, y, pointer, toActor);
                        levelsButton.getLabel().setColor(inactiveColor);
                    }

                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        SfxController.getInstance().playSFX("map_open");
                        changeScreenTo(MenuScreen.LEVEL_SELECT);
                    }
                });
                settingsButton.addListener(new ClickListener(){
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                        super.enter(event, x, y, pointer, fromActor);
                        settingsButton.getLabel().setColor(activeColor);
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        super.exit(event, x, y, pointer, toActor);
                        settingsButton.getLabel().setColor(inactiveColor);
                    }

                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        SfxController.getInstance().playSFX("button_click");
                        settingsPressed = true;
                    }
                });
                creditsButton.addListener(new ClickListener(){
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if(pointer == -1) SfxController.getInstance().playSFX("button_enter");
                        super.enter(event, x, y, pointer, fromActor);
                        creditsButton.getLabel().setColor(activeColor);
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        super.exit(event, x, y, pointer, toActor);
                        creditsButton.getLabel().setColor(inactiveColor);
                    }

                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        SfxController.getInstance().playSFX("button_click");
                        changeScreenTo(MenuScreen.CREDITS);
                    }
                });

                //Add buttons to table
                menuTable.add(startButton).padTop(canvas.getHeight() / 2).expandX().align(Align.left).padLeft(100);
                menuTable.row();
                menuTable.add(levelsButton).expandX().align(Align.left).padLeft(100);
                menuTable.row();
                menuTable.add(settingsButton).expandX().align(Align.left).padLeft(100);
                menuTable.row();
                menuTable.add(creditsButton).expandX().align(Align.left).padLeft(100);
                break;
            case LEVEL_SELECT:
                Table part1 = new Table();
                part1.align(Align.left);
                part1.add(menuButton).expandX().align(Align.left).padRight(1100).padTop(10);
                part1.row();
                menuTable.add(part1);
                menuTable.row();

                Table part2 = new Table();
                Label levelSelectLabel = new Label("SELECT A LEVEL", skin);
                levelSelectLabel.setFontScale(0.6f);
                part2.add(levelSelectLabel).expandX().align(Align.center);
                menuTable.add(part2);
                menuTable.row();

                Table part3 = new Table();

                // Create buttons
                TextureRegionDrawable[] buttonDrawables = new TextureRegionDrawable[4];
                TextButtonStyle[] buttonStyles = new TextButtonStyle[4];
                for (int i = 0; i < 4; i++) {
                    buttonDrawables[i] = new TextureRegionDrawable(new TextureRegion(levelButtonImages[i]));
                    buttonStyles[i] = new TextButtonStyle();
                    buttonStyles[i].up = buttonDrawables[i];
                    buttonStyles[i].down = buttonDrawables[i].tint(Color.GRAY);
                    buttonStyles[i].font = skin.getFont("default-font");
                }

                TextButtonStyle lockButtonStyle = new TextButtonStyle();
                TextureRegionDrawable lockButtonDrawable =  new TextureRegionDrawable(new TextureRegion(levelButtonImages[4]));
                lockButtonStyle.up = lockButtonDrawable;
                lockButtonStyle.down = lockButtonDrawable;
                lockButtonStyle.font = skin.getFont("default-font");

                levelButtons = new TextButton[LEVEL_COUNT];

                for (int i = 0; i < LEVEL_COUNT; i ++) {
                    JsonValue levelData = saveData.get("level_data").get(i);
                    int score = levelData.get("score").asInt();
                    boolean debug = saveData.get("debug").asBoolean();
                    boolean unlocked = levelData.get("unlocked").asBoolean();
                    levelButtons[i] = new TextButton(String.valueOf(i), debug || unlocked ? buttonStyles[score] : lockButtonStyle);
                    TextButton currentButton = levelButtons[i];
                    currentButton.getLabel().setFontScale(0.5f);
                    int finalI = i;
                    currentButton.addListener(new ClickListener(){
                        @Override
                        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                            if(pointer == -1) SfxController.getInstance().playSFX("button_island");
                            super.enter(event, x, y, pointer, fromActor);
                            if (debug || unlocked) {
                                currentButton.setColor(Color.LIGHT_GRAY);
                            }
                        }

                        @Override
                        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                            super.exit(event, x, y, pointer, toActor);
                            currentButton.setColor(Color.WHITE);
                        }

                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (debug || unlocked) {
                                currentButton.setColor(Color.LIGHT_GRAY);
                                SfxController.getInstance().playSFX("raft_sail_open");
                                selectlevel(finalI);
                            }
                        }
                    });
                    //Add button to table
                    if (i > 0 && i % NUM_COLS == 0)
                        part3.row().padTop(colPadding);
                    part3.add(currentButton).size(150).padLeft(i % NUM_COLS > 0 ? colPadding * 2 : 0);
                }
                menuTable.add(part3);
                menuTable.row();
                break;
            case CREDITS:
                Table creditsPart1 = new Table();
                creditsPart1.add(menuButton).expandX().align(Align.left).padRight(1100).padTop(10);
                menuTable.add(creditsPart1);
                menuTable.row();

                Table creditsPart2 = new Table();
                Label creditsLabel = new Label("CREDITS", skin);
                creditsLabel.setFontScale(0.5f);
                creditsPart2.add(creditsLabel).expandX().align(Align.center);
                menuTable.add(creditsPart2).padTop(-50);
                menuTable.row();

                Table creditsPart3 = new Table();
                creditsPart3.align(Align.center);

                Table creditsPart3Left = new Table();
                Label programmersLabel = new Label("PROGRAMMER", skin);
                programmersLabel.setFontScale(0.35f);
                creditsPart3Left.add(programmersLabel).expandX().align(Align.center);
                creditsPart3Left.row();

                Label amy = new Label("Amy Huang", skin);
                amy.setFontScale(0.3f);
                Label demian = new Label("Demian Yutin", skin);
                demian.setFontScale(0.3f);
                Label howard = new Label("Howard Fu", skin);
                howard.setFontScale(0.3f);
                Label jaden = new Label("Jaden O'Brien", skin);
                jaden.setFontScale(0.3f);
                Label jason = new Label("Jason Tung", skin);
                jason.setFontScale(0.3f);
                Label leo = new Label("Leo Zhao", skin);
                leo.setFontScale(0.3f);
                creditsPart3Left.add(amy).expandX().align(Align.center);
                creditsPart3Left.row();
                creditsPart3Left.add(demian).expandX().align(Align.center);
                creditsPart3Left.row();
                creditsPart3Left.add(howard).expandX().align(Align.center);
                creditsPart3Left.row();
                creditsPart3Left.add(jaden).expandX().align(Align.center);
                creditsPart3Left.row();
                creditsPart3Left.add(jason).expandX().align(Align.center);
                creditsPart3Left.row();
                creditsPart3Left.add(leo).expandX().align(Align.center);

                Table creditsPart3Right = new Table();
                Label designersLabel = new Label("DESIGNER", skin);
                designersLabel.setFontScale(0.38f);
                creditsPart3Right.add(designersLabel).expandX().align(Align.center);
                creditsPart3Right.row();

                Label gloria = new Label("Gloria Shi", skin);
                gloria.setFontScale(0.3f);
                Label noah = new Label("Noah Braun", skin);
                noah.setFontScale(0.3f);
                Label spencer = new Label("Spencer Pettee", skin);
                spencer.setFontScale(0.3f);

                creditsPart3Right.add(gloria).expandX().align(Align.center);
                creditsPart3Right.row();
                creditsPart3Right.add(noah).expandX().align(Align.center);
                creditsPart3Right.row();
                creditsPart3Right.add(spencer).expandX().align(Align.center);
                creditsPart3Right.row();
                creditsPart3Right.add(new Label("", skin));
                creditsPart3Right.row();

                creditsPart3.add(creditsPart3Left).expandX().align(Align.center).padRight(180).padLeft(-100);
                creditsPart3.add(creditsPart3Right).expandX().align(Align.center).padTop(20);
                menuTable.add(creditsPart3);
                break;
        }

        stage.addActor(menuTable);
    }

    private void selectlevel(int id) {
        if (levelButtons != null && !isLevelPressed) {
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

    /** Reset the play pressed state */
    private void resetPlayState() { playPressed = false; }

    /** Reset the level pressed state */
    private void resetPressedState() { isLevelPressed = false; }

    /** Reset the settings pressed state */
    private void resetSettingsState() { settingsPressed = false; }

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