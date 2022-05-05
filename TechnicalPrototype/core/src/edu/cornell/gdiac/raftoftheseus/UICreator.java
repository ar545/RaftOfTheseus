package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import edu.cornell.gdiac.raftoftheseus.MenuMode;
import edu.cornell.gdiac.raftoftheseus.singleton.SfxController;
import org.lwjgl.Sys;
import org.w3c.dom.Text;

//import javax.xml.soap.Text;
import java.util.function.Consumer;

public class UICreator {

    /** The standard size for fonts */
    private static float FONT_SIZE = 0.3f;
    /** The color for inactive buttons */
    private static Color inactiveColor = Color.WHITE;
    /** The color for hovered buttons */
    private static Color activeColor = Color.GOLD;

    public UICreator(){}

    // TEXTBUTTON CREATORS

    public static TextButton createTextButton(String name, Skin skin, float fontSize, Color c){
        TextButton button = new TextButton(name, skin);
        button.getLabel().setFontScale(fontSize);
        button.getLabel().setColor(c);
        return button;
    }

    public static TextButton createTextButton(String name, Skin skin, float fontSize, Color c, Texture background) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(background));
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = drawable;
        buttonStyle.down = drawable.tint(Color.GRAY);
        buttonStyle.font = skin.getFont("default-font");
        TextButton button = new TextButton(name, buttonStyle);
        button.getLabel().setFontScale(fontSize);
        button.getLabel().setColor(c);
        return button;
    }

    public static void setTextButtonStyle(TextButton button, Skin skin, float fontSize, Color c, Texture background) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(background));
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = drawable;
        buttonStyle.down = drawable.tint(Color.GRAY);
        buttonStyle.font = skin.getFont("default-font");
        button.setStyle(buttonStyle);
        button.getLabel().setFontScale(fontSize);
        button.getLabel().setColor(c);
    }

    public static TextButton createTextButton(String name, Skin skin, float fontSize){
        return createTextButton(name, skin, fontSize, Color.WHITE);
    }

    public static TextButton createTextButton(String name, Skin skin, Color c){
        return createTextButton(name, skin, FONT_SIZE, c);
    }

    public static TextButton createTextButton(String name, Skin skin){
        return createTextButton(name, skin, FONT_SIZE);
    }

    // LABEL CREATORS

    public static Label createLabel(String name, Skin skin, float fontSize){
        Label label = new Label(name, skin);
        label.setFontScale(fontSize);
        return label;
    }

    // LISTENER CREATORS

    public static ClickListener createListener(TextButton button, Color ActC, Color inActC, String enter_sfx, String clickSFX,
                                             Consumer<MenuMode.MenuScreen> function, MenuMode.MenuScreen screen){
        return new ClickListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SfxController.getInstance().playSFX(enter_sfx);
                super.enter(event, x, y, pointer, fromActor);
                button.getLabel().setColor(ActC);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                button.getLabel().setColor(inActC);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                SfxController.getInstance().playSFX(clickSFX);
                function.accept(screen);
            }
        };
    }

    public static ClickListener createListener(TextButton button, String clickSFX, Consumer<MenuMode.MenuScreen> function, MenuMode.MenuScreen screen){
        return createListener(button, activeColor, inactiveColor, "button_enter", clickSFX, function, screen);
    }

    public static ClickListener createListener(TextButton button, Consumer<MenuMode.MenuScreen> function, MenuMode.MenuScreen screen){
        return createListener(button, "button_click", function, screen);
    }

    public static ClickListener createListener(TextButton button, Color ActC, Color inActC,
                                             String enter_sfx, String clickSFX, Runnable function){
        return new ClickListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) SfxController.getInstance().playSFX(enter_sfx);
                super.enter(event, x, y, pointer, fromActor);
                button.getLabel().setColor(ActC);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                button.getLabel().setColor(inActC);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                SfxController.getInstance().playSFX(clickSFX);
                function.run();
            }
        };
    }

    public static ClickListener createListener(TextButton button, String clickSFX, Runnable function){
        return createListener(button, activeColor, inactiveColor, "button_enter", clickSFX, function);
    }

    public static ClickListener createListener(TextButton button, Runnable function){
        return createListener(button, "button_click", function);
    }

    public static  ClickListener createListener(TextButton button, Color c1, Color c2, Runnable function){
        return createListener(button, c1, c2, "button_enter", "button_click", function);
    }

    // SETTINGS BUTTONS
    public static ClickListener createListener(TextButton button, Consumer<TextButton> function) {

        return new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (!button.getText().toString().equals("left mouse")) {
                    if (pointer == -1) SfxController.getInstance().playSFX("button_enter");
                    button.getLabel().setColor(Color.LIGHT_GRAY);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                if (!button.getText().toString().equals("left mouse")) {
                    button.getLabel().setColor(Color.WHITE);
                }
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (!button.getText().toString().equals("left mouse")) {
                    SfxController.getInstance().playSFX("button_click");
                    function.accept(button);
                }
            }
        };
    }

    // LEVEL BUTTONS
    public static ClickListener createListener(TextButton button, boolean canPlay, Consumer<Integer> function, int level){
        return new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) SfxController.getInstance().playSFX("button_island");
                super.enter(event, x, y, pointer, fromActor);
                if (canPlay) {
                    button.getLabel().setColor(Color.LIGHT_GRAY);
                    button.setColor(Color.LIGHT_GRAY);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                button.setColor(Color.WHITE);
                button.getLabel().setColor(Color.WHITE);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (canPlay) {
                    button.setColor(Color.LIGHT_GRAY);
                    SfxController.getInstance().playSFX("raft_sail_open");
                    function.accept(level);
                }
            }
        };
    }

    // WorldController Buttons
    public static ClickListener createListener(TextButton button, Consumer<Boolean> function, boolean didFail){
        return new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) SfxController.getInstance().playSFX("button_enter");
                super.enter(event, x, y, pointer, fromActor);
                button.getLabel().setColor(Color.LIGHT_GRAY);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                button.getLabel().setColor(Color.WHITE);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                SfxController.getInstance().playSFX("button_click");
                super.clicked(event, x, y);
                function.accept(didFail);
            }
        };
    }
}
