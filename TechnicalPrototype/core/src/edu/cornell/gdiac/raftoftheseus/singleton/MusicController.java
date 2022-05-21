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
    private boolean levelComplete = false;

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
        tradeTime = set.getLong("trade_time", 500L);

        // Get music presets
        JsonValue mscpresets = directory.getEntry("thread_music_settings", JsonValue.class);
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
     * @param level is the JsonValue that contains text references to all sounds
     */
    public void setMusicPreset(int level){
        JsonValue indicator = musicPresets.get(4);
        int preset = indicator.getInt(Integer.toString(level));
        if (this.musicPreset != preset){
            this.musicPreset = preset;
            setMusic();
        }
    }

    /**
     * Plays a music file at specified volume with reference index.
     * Precondition: volume > 0 and < musicVolume
     * @param index
     */
    private void playMusic(String index, float volume, boolean looping){
        Music m = music.get(index).getMusic();
        if (!m.isPlaying()) {
            m.play();
            m.setVolume(volume);
            m.setLooping(looping);
        }
    }

    /**
     * Plays a music file at musicVolume with reference index at musicVolume. Looping is true.
     * @param index
     */
    private void playMusic(String index){
        playMusic(index, musicVolume, true);
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
     * Starts the music for the menu
     */
    public void startMenuMusic(){
        musicPreset = 0;
        setMusic();
        playMusic("menu");
    }

    /**
     * Starts the music for a level, fails silently if proper preset is not loaded.
     */
    public void startLevelMusic(int level){
        setMusicPreset(level);
        resetThreads();
        setMusicVolume(0, "shark", "siren", "explore");
        playMusic("shark", 0, true);
        playMusic("siren", 0, true);
        playMusic("explore");
        music.get("explore").setFadeIn(true);
    }

    /**
     * Fades out both all music threads;
     */
    public void completeMusic(){
        if(!levelComplete) {
            levelComplete = true;
            for(DynamicMusic dm : music.values()){
                dm.FadeOut();
            }
        }
    }

    /**
     * Fade siren music in.
     * @param fadeIn
     */
    public void tradeMusic(boolean fadeIn, String name){
        print(name, music.get(name).isFadeIn());
        if(fadeIn){
            if(!music.get(name).isFadeIn()){
                music.get(name).FadeIn();
            }
        } else {
            if(music.get(name).isFadeIn()){
                music.get(name).FadeOut();
            }
        }
    }


    private void haltThreads(){
        for(DynamicMusic dm : music.values()){
            dm.stopThread();
        }
    }

    /**
     * Stops all music.
     */
    public void resetThreads(){
        for(DynamicMusic m : music.values()){
            m.resetThread();
        }
    }

    public static float getMusicVolume() {
        return musicVolume;
    }

    public static long getTradeTime() {
        return tradeTime;
    }

    /**
     * Resumes all music for restarting the level.
     */
    public void resumeMusic(){
        resetThreads();
        for(DynamicMusic m : music.values()){
            m.getMusic().setPosition(0);
            m.getMusic().play();
        }
    }

    /**
     * Pauses all music for restarting or completing the level.
     */
    public void pauseMusic(){
        haltThreads();
        for(DynamicMusic m : music.values()){
            m.getMusic().pause();
        }
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

    public void print(String name, boolean val){
        System.out.println(name + ": " +  val);
    }
}
