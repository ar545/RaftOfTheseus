package edu.cornell.gdiac.raftoftheseus.singleton;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class MusicController {

    /** Master volume for Music */
    private static float musicVolume;
    /** Speed taken to transition music in miliseconds. */
    private static long tradeTime;
    /** Current preset being used for music. */
    private int musicPreset;
    /** Structure to hold all music presets for future reference. */
    private ArrayMap<Integer, JsonValue> musicPresets;
    /** ArrayMap to link music names to Music instances. */
    private ArrayMap<String, DynamicMusic> music;
    /** The asset directory for getting new music. */
    private AssetDirectory directory;
    /** The singleton instance of the input controller */
    private static MusicController theController = null;
    /** Whether the player was in danger */
    private boolean wasInDanger = false;
    /** Whether the level is complete. */
    private boolean levelComplete = true;

    /**
     * Constructor for MusicController
     */
    public MusicController(){
        musicPresets = new ArrayMap<>();
        music = new ArrayMap<>();
    }

    /**
     *
     * @param directory
     */
    public void gatherAssets(AssetDirectory directory) {
        this.directory = directory;
        // Settings values
        JsonValue set = directory.getEntry("sound_settings", JsonValue.class);
        musicVolume = set.getFloat("music_volume", 1.0f);
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
     * Set master musicVolume from settings.
     * @param musicVolume Float between 0-1
     */
    public void setMasterMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
        for(String s : music.keys()) {
            if(music.get(s).isFadeIn())  setMusicVolume(musicVolume,s);
        }
    }

    /**
     * @return music volume being played right now.
     */
    public float getMasterMusicVolume(){ return musicVolume; }

    /**
     * Sets the music according to the current preset stored.
     */
    private void setMusic(){
        music.clear();
        JsonValue mscpreset = musicPresets.get(musicPreset);
        for(JsonValue m : mscpreset){
            Music mtemp = directory.getEntry(m.asString(), Music.class);
            music.put(m.name(), new DynamicMusic(m.name(), mtemp));
        }
    }

    /**
     * Sets the values in sfx and music based on provided ids.
     * Must be called every WorldController or MenuController change due to memory constraints on sounds.
     * @param musicPreset is the JsonValue that contains text references to all sounds
     */
    public void setMusicPreset(int musicPreset){
        if (this.musicPreset != musicPreset){
            this.musicPreset = musicPreset;
            setMusic();
        }
    }

    /**
     * Plays a music file at specified volume with reference index.
     * Precondition: volume > 0 and < musicVolume
     * @param index
     */
    private void playMusic(String index, float volume){
        Music m = music.get(index).getMusic();
        if (!m.isPlaying()) {
            m.play();
            m.setVolume(volume);
        }
    }

    /**
     * Plays a music file at musicVolume with reference index at musicVolume.
     * @param index
     */
    private void playMusic(String index){
        playMusic(index, musicVolume);
    }

    /**
     * Step music position at pos
     * @param pos the new position to start.
     */
    private void setMusicLocation(String index, float pos){
        music.get(index).getMusic().setPosition(pos);
    }

    /**
     * Takes the music with key index and decreases its volume by dv.
     * @param volume between 0-1
     */
    private void setMusicVolume(float volume, String... indices){
        for(String s : indices){
            music.get(s).getMusic().setVolume(volume);
        }
    }

    /**
     * For looping menu music only.
     * @param name
     */
    public void loopMusic(String name){
        Music m = music.get(name).getMusic();
        m.play();
        m.setLooping(true);
        m.setVolume(musicVolume);
    }

    /**
     * Starts the music for the menu
     */
    public void startMenuMusic(){
        setMusicPreset(0);
        loopMusic("background");
    }

    /**
     * Starts the music for a level, fails silently if proper preset is not loaded.
     */
    public void startLevelMusic(int preset){
        if(preset < 1) throw new RuntimeException("Invalid level music preset.");
        setMusicPreset(preset);
        setMusicVolume(0, "enemy", "siren", "explore");
        playMusic("enemy", 0);
        playMusic("siren", 0);
        playMusic("explore");
    }

    /**
     * Fades out both explore and danger music in separate threads.
     */
    public void completeMusic(){
        if(!levelComplete) {
            levelComplete = true;
            for(DynamicMusic dm : music.values()){
                dm.FadeOut();
            }
        }
    }

    /** Updates the Music based on booleans */
    public void updateMusic(boolean shark, boolean siren){
        if(levelComplete) return;
        if(siren){
            if(!music.get("enemy").isFadeIn()){
                music.get("enemy").FadeIn();
            }
            if(!music.get("siren").isFadeIn()){
                music.get("siren").FadeIn();
            }
        } else if (!siren){
            if(music.get("siren").isFadeIn()){
                music.get("siren").FadeOut();
            }
        }
        if(shark){
            if(!music.get("enemy").isFadeIn()){
                music.get("enemy").FadeIn();
            }
        }
        if(!shark && !siren){
            if(music.get("enemy").isFadeIn()){
                music.get("enemy").FadeOut();
            }
        }
    }


    private void haltThreads(){
        for(DynamicMusic dm : music.values()){
            dm.stopThread();
        }
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
        for(DynamicMusic m : music.values()){
            m.getMusic().stop();
        }
    }

    public void dispose(){
        for(DynamicMusic m : music.values()){
            m.getMusic().dispose();
        }
        music.clear();
    }
}
