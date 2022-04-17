package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class MusicController {

    /** Master volume for Music */
    private static float musicVolume = 0.3f;
    /** Set screen distance to calculate sound decay */
    private float decayDistance = 400f;
    /** Speed taken to transition music in miliseconds. */
    private float tradeRate = 0.0001f;
    /** Speed taken to transition music in miliseconds. */
    private float tradeThreshold = 0.0001f;
    /** Speed taken to transition music in miliseconds. */
    private float fadeOutRate = 0.02f;
    /** Speed taken to transition music in miliseconds. */
    private static long tradeTime = 1000L;
    /** Current preset being used for music. */
    private int musicPreset;
    /** Structure to hold all music presets for future reference. */
    private ArrayMap<Integer, JsonValue> musicPresets;
    /** ArrayMap to link music names to Music instances. */
    private ArrayMap<String, Music> music;
    /** The asset directory for getting new music. */
    private AssetDirectory directory;
    /** The singleton instance of the input controller */
    private static MusicController theController = null;
    /** Whether a music trade is in progress. */
    private enum MusicState {
        SAFE,
        ENTER_DANGER,
        DANGER,
        LEAVE_DANGER,
    }
    private MusicState STATE;
    /** Whether level is over. */
    private boolean level_complete = false;
    /** Whether the player was in danger */
    private boolean wasInDanger = false;

    /**
     * Constructor for MusicController
     */
    public MusicController(){
        musicPresets = new ArrayMap<>();
        music = new ArrayMap<>();
        STATE = MusicState.SAFE;
    }

    /**
     *
     * @param directory
     */
    public void gatherAssets(AssetDirectory directory) {
        if (musicPresets == null) throw new NullPointerException("Constructor not called.");
        this.directory = directory;
        // Setsettings values
        JsonValue set = directory.getEntry("sound_settings", JsonValue.class);
        musicVolume = set.getFloat("music_volume", 1.0f);
        tradeRate = set.getFloat("trade_rate", 0.0001f);
        tradeThreshold = set.getFloat("trade_threshold", 0.00001f);
        tradeTime = set.getLong("trade_time", 1000L);
        // Get music presets
        JsonValue mscpresets = directory.getEntry("music_settings", JsonValue.class);
        int i = 0;
        for(JsonValue m : mscpresets){
            musicPresets.put(i, m);
            i++;
        }
    }

    /**
     * @return the singleton instance of the input controller
     */
    public static MusicController getInstance() {
        if (theController == null) {
            theController = new MusicController();
        }
        return theController;
    }

    /**
     * Check is a defensive function that simplifies checking whether the obtained music file is null.
     * @param name Music key name
     */
    private Music checkMusic(String name){
        Music a = music.get(name);
        if (a == null) throw new RuntimeException(name + " is not a valid music name.");
        return a;
    }


    /**
     * Fades out both explore and danger music in separate threads. BUGGY.
     */
    public void levelComplete(){
//        fadeIn = new SfxController.Trader("fadeOut", "danger", false);
//        fadeOut = new SfxController.Trader("fadeOut", "explore", false);
    }


    private void haltThreads(){
//        if(fadeIn != null){
//            fadeIn.stop();
//        }
//        if(fadeOut != null) {
//            fadeOut.stop();
//        }
    }

    public static float getMusicVolume() {
        return musicVolume;
    }

    public static long getTradeTime() {
        return tradeTime;
    }

    /**
     * Stops all music.
     */
    public void haltMusic(){
        haltThreads();
        for(Music m : music.values()){
            m.stop();
        }
    }

    public void dispose(){
        for(Music m : music.values()){
            m.dispose();
        }
    }
}
