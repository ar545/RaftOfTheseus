package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.raftoftheseus.singleton.InputController;
import edu.cornell.gdiac.util.ScreenListener;
import org.lwjgl.Sys;

import java.util.Arrays;

import static edu.cornell.gdiac.raftoftheseus.GDXRoot.MENU_TO_SETTINGS;
import static edu.cornell.gdiac.raftoftheseus.GDXRoot.NUM_LEVELS;

/**
 * Class that provides the menu screen for the state of the game.
 * */
public class MenuMode implements Screen {

    /** An enum representing which screen is currently active within the menu system */
    public static enum MenuScreen {
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
    private ScrollPane scrollPane;
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
    public static void setConstants(JsonValue objParams, int numLevels, int levelsPerPage){
        STANDARD_WIDTH = objParams.getInt(0);
        STANDARD_HEIGHT = objParams.getInt(1);
        NUM_COLS = objParams.getInt(2);
        colPadding = objParams.getInt("column padding", 25);
        LEVEL_COUNT = numLevels;
        LEVELS_PER_PAGE = levelsPerPage;
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
    /** Number of levels per page */
    private static int LEVELS_PER_PAGE;
    /** Current page */
    private int currentPage;
    /** Whether the play button was pressed on the main menu */
    private boolean playPressed;
    /** Whether the settings button was pressed on the main menu */
    private Boolean settingsPressed;

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
        currentPage = 0;
        settingsPressed = false;
        playPressed = false;
        currentScreen = MenuScreen.TITLE;
        skin = new Skin(Gdx.files.internal("skins/default/uiskin.json"));
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

    // BUTTON INITIALIZATION
    private TextButton backButton;
    private TextButton nextPageButton;
    private TextButton prevPageButton;
    private Table backTable;
    private Table scrollButtonTable;
    private Array<TextButton> titleButtons = new Array<>();
    private Array<Table> levelTables = new Array<>();
    private TextureRegionDrawable[] buttonDrawables = new TextureRegionDrawable[4];
    private TextButtonStyle[] buttonStyles = new TextButtonStyle[4];
    private TextButtonStyle lockButtonStyle = new TextButtonStyle();
    private Array<Table> creditTables = new Array<>();

    /**
     * Creates all the necessary buttons for the menu to prevent to need for reconstruction every time.
     */
    public void initButtons(){
        initMenuButtons();
        initLevelTables();
        initCreditTables();
    }

    /**
     * Creates all necessary buttons for the menu screen.
     */
    private void initMenuButtons(){
        backButton = UICreator.createTextButton("BACK", skin, Color.WHITE);
        backButton.addListener(UICreator.createListener(backButton, Color.GOLD, Color.WHITE,
                "button_enter", "button_click", this::changeScreenTo, MenuScreen.TITLE));

        nextPageButton = UICreator.createTextButton("NEXT", skin, Color.WHITE);
        nextPageButton.addListener(UICreator.createListener(nextPageButton, Color.GOLD, Color.WHITE, this::scrollPage));

        prevPageButton = UICreator.createTextButton("PREV", skin, Color.WHITE);
        prevPageButton.addListener(UICreator.createListener(prevPageButton, Color.GOLD, Color.WHITE, this::scrollPage));

        nextPageButton.setVisible(currentPage == 0);
        prevPageButton.setVisible(currentPage == 2);

        backTable = new Table();
        backTable.add(backButton).expandX().align(Align.left).padRight(1500).padTop(10);

        // instantiate the "back" button, which is used in multiple menus
        Array<String> holder = new Array<>(new String[]{"START", "LEVELS", "SETTINGS", "CREDITS"});
        for(String n : holder){
            titleButtons.add(UICreator.createTextButton(n, skin));
        }
        //Add listeners to buttons
        titleButtons.get(0).addListener(UICreator.createListener(titleButtons.get(0), "raft_sail_open", this::setPlayState));
        titleButtons.get(1).addListener(UICreator.createListener(titleButtons.get(1), "map_open", this::changeScreenTo, MenuScreen.LEVEL_SELECT));
        titleButtons.get(2).addListener(UICreator.createListener(titleButtons.get(2), this::setSettingsState));
        titleButtons.get(3).addListener(UICreator.createListener(titleButtons.get(3), this::changeScreenTo, MenuScreen.CREDITS));
    }

    /**
     * Creates all necessary tables for the level select screen (except levels themselves).
     */
    private void initLevelTables(){
        Table part2 = new Table();
        part2.row();
        part2.add(UICreator.createLabel("SELECT A LEVEL", skin, 0.85f)).expandX().align(Align.center).padTop(-50);
        levelTables.add(backTable, part2);

        // Button styles
        for (int i = 0; i < 4; i++) {
            buttonDrawables[i] = new TextureRegionDrawable(new TextureRegion(levelButtonImages[i]));
            buttonStyles[i] = new TextButtonStyle();
            buttonStyles[i].up = buttonDrawables[i];
            buttonStyles[i].down = buttonDrawables[i].tint(Color.GRAY);
            buttonStyles[i].font = skin.getFont("default-font");
        }
        TextureRegionDrawable lockButtonDrawable =  new TextureRegionDrawable(new TextureRegion(levelButtonImages[4]));
        lockButtonStyle.up = lockButtonDrawable;
        lockButtonStyle.down = lockButtonDrawable;
        lockButtonStyle.font = skin.getFont("default-font");
    }

    /**
     * Creates all the credit visuals for the game.
     */
    private void initCreditTables(){
        Table tb2 = new Table();
        tb2.add(UICreator.createLabel("CREDITS", skin, 0.6f)).expandX().align(Align.center);

        Table tb3 = new Table();
        tb3.align(Align.center);

        Table part3L = new Table();
        part3L.add(UICreator.createLabel("PROGRAMMER", skin, 0.5f)).expandX().align(Align.center);
        part3L.row();

        Table part3R = new Table();
        part3R.add(UICreator.createLabel("DESIGNER", skin, 0.5f)).expandX().align(Align.center);
        part3R.row();

        Array<String> members = new Array<String>(new String[]{"Amy Huang", "Demian Yutin", "Howard Fu",
                "Jaden O'Brien", "Jason Tung", "Leo Zhao", "Gloria Shi", "Noah Braun", "Spencer Pettee", ""});
        int i = 0;
        for(String name : members){
            Label l = new Label(name, skin);
            l.setFontScale(0.4f);
            if (i <= 5) {
                part3L.add(l).expandX().align(Align.center);
                if (i != 5) part3L.row();
            } else {
                part3R.add(l).expandX().align(Align.center);
                if (i != members.size - 1) part3R.row();
            }
            i++;
        }

        tb3.add(part3L).expandX().align(Align.center).padRight(180).padLeft(-100);
        tb3.add(part3R).expandX().align(Align.center).padTop(-180);
        creditTables.add(backTable, tb2, tb3);
    }

    /**
     * Sets the ScreenListener for this mode
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    /** @param saveData the save data to be set. */
    public void setSaveData(JsonValue saveData) { this.saveData = saveData; }

    /** @return level selected by the player */
    public int getSelectedLevel() { return selectedLevel; }

    /** @return true if the player has selected a level */
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
        buildMenu();
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        active = false;
        stage = null;
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

    /**
     * Creates the UI screen based on the given MenuScreen state by population the menuTable.
     */
    private void buildMenu(){
        Table menuTable = new Table(skin);
        menuTable.setFillParent(true);
        menuTable.align(Align.top);
        int i = 0;
        switch(currentScreen) {
            case TITLE:
                for(TextButton tb : titleButtons){
                    if(i == 0){
                        menuTable.add(tb).padTop(canvas.getHeight() / 2).expandX().align(Align.left).padLeft(130);
                    } else {
                        menuTable.add(tb).expandX().align(Align.left).padLeft(130);
                    }
                    if (i != titleButtons.size - 1) menuTable.row();
                    i++;
                }
                break;
            case LEVEL_SELECT:
                for(Table t : levelTables){
                    menuTable.add(t);
                    menuTable.row();
                }
                scrollPane = new ScrollPane(addLevelIslands());
                scrollPane.setFlickScroll(false);
                menuTable.add(scrollPane).size(stage.getWidth(), stage.getHeight()*0.7f).padTop(-100);
                scrollButtonTable = new Table();
                scrollButtonTable.add(prevPageButton).padRight(stage.getWidth() / 2 - 200);
                scrollButtonTable.add(nextPageButton).padLeft(stage.getWidth() / 2 - 200);
                scrollButtonTable.align(Align.center);
                menuTable.row();
                menuTable.add(scrollButtonTable).padTop(-50);
                menuTable.row();
                break;
            case CREDITS:
                for(Table t : creditTables){
                    if(i != 1) menuTable.add(t);
                    else menuTable.add(t).padTop(-50);
                    if(i != creditTables.size -1) menuTable.row();
                    i++;
                }
                break;
        }
        stage.addActor(menuTable);
    }

    /** Adds the 3rd table to the levelTables array to menu population. */
    private Table addLevelIslands(){
        // TODO - redo this to just have 0 - 9 and recalculate with + 10 for every page other than the first page
        // page * index -> page 0 * 0 = 0, page 0  * levels per page + number 1 = 1,
        // formula: page * levels per page + number
        int[] levelCounts = new int[] { 0, 9, 1, 8, 2, 7, 3, 6, 4, 5, 10, 19, 11, 18, 12, 17, 13, 16, 14, 15};
        int padding = 170;
        int[][] buttonPadding = new int[][] {
                new int[] {0, 3},
                new int[] {1, 2},
                new int[] {2, 1},
                new int[] {1, 2},
                new int[] {2, 0},
                new int[] {0, 4},
                new int[] {0, 3},
                new int[] {1, 2},
                new int[] {2, 1},
                new int[] {3, 0},
        };
        for (int i = 0; i < buttonPadding.length; i++) {
            buttonPadding[i][0] = buttonPadding[i][0] * padding;
            buttonPadding[i][1] = buttonPadding[i][1] * padding;
        }
        Table scrollTable = new Table();
        Table pageTable = new Table();
        pageTable.align(Align.left);

        levelButtons = new TextButton[LEVEL_COUNT];
        int levelNumber = 0;
        for (int i = 0; i < LEVEL_COUNT; i ++) {
            levelNumber = levelCounts[i];
            // Create and add textbuttons to screen. Must update each pass to update star displays.
            JsonValue levelData = saveData.get("level_data").get(levelNumber);
            int score = levelData.get("score").asInt();
            boolean canPlay = saveData.get("debug").asBoolean() ||  levelData.get("unlocked").asBoolean();
            levelButtons[levelNumber] = new TextButton(String.valueOf(levelNumber), canPlay ? buttonStyles[score] : lockButtonStyle);
            levelButtons[levelNumber].getLabel().setFontScale(0.8f);
            levelButtons[levelNumber].addListener(UICreator.createListener(levelButtons[levelNumber], canPlay, this::selectlevel, levelNumber));
            //Add button to table
            int buttonPaddingLeft = buttonPadding[levelNumber % LEVELS_PER_PAGE][0];
            int buttonPaddingRight = buttonPadding[levelNumber % LEVELS_PER_PAGE][1];
            pageTable.add(levelButtons[levelNumber]).size(200).padLeft(buttonPaddingLeft).padRight(buttonPaddingRight);

            if ((i + 1) > 0 && (i + 1) % NUM_COLS == 0) {
                pageTable.row().width(stage.getWidth()).padTop(-80);
            }

            if ((i + 1) % LEVELS_PER_PAGE == 0) {
                scrollTable.add(pageTable)
                           .width(stage.getWidth() * 0.7f)
                           .padLeft(i > LEVELS_PER_PAGE ? 440 : 100)
                           .padRight(i > LEVELS_PER_PAGE ? 400 : 0);
                pageTable = new Table();
                pageTable.align(Align.left);
            }
        }

        return scrollTable;
    }

    /**
     * Scrolls to the appropriate page
     * */
    private void scrollPage() {
        float pageWidth = stage.getWidth() * 0.7f;
        currentPage = currentPage == 0 ? 2 : 0;
        System.out.println(currentPage);
        nextPageButton.setVisible(currentPage == 0);
        prevPageButton.setVisible(currentPage == 2);
        scrollButtonTable.align(currentPage == 0 ? Align.right : Align.left);
        scrollPane.scrollTo(currentPage * pageWidth, scrollPane.getHeight(), pageWidth, scrollPane.getHeight());
    }

    /**
     * Sets the selected level to the given id for communication with WorldController
     * @param id the level id
     */
    private void selectlevel(int id) {
        if (levelButtons != null && !isLevelPressed) {
            isLevelPressed = true;
            selectedLevel = id;
        }
    }

    /**
     * Method to set the different menu screens
     * @param targetScreen the screen we want
     */
    private void changeScreenTo(MenuScreen targetScreen) {
        stage.clear();
        currentScreen = targetScreen;
        buildMenu();
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

    /** Reset the play pressed state */
    private void setPlayState(){ playPressed = true; }
    private void resetPlayState() { playPressed = false; }
    /** Reset the level pressed state */
    private void setPressedState(){ isLevelPressed = true; }
    private void resetPressedState() { isLevelPressed = false; }
    /** Reset the settings pressed state */
    private void setSettingsState(){ settingsPressed = true; }
    private void resetSettingsState() { settingsPressed = false; }


    /** Called when the Screen is paused. */
    public void pause() {}
    /** Called when the Screen is resumed from a paused state. */
    public void resume() {}
    /** Called when this screen should release all resources. */
    public void dispose() {}
}