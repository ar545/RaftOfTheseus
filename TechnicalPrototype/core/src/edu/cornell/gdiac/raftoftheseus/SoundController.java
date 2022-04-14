package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

/**
 * Class to control starting, looping, and stopping sounds for the game.
 * Requires updating every time controllers or levels are changed due to memory constraints of music.
 * Usage is as follows: getInstance() -> gatherAssets() -> setPresets() -> playSFX(), playMusic() etc.
 * Changed loaded sounds with setPresets() as needed.
 */
public class SoundController {
    private boolean level_complete = false;
    /** Master volume for SFX */
    private float sfxVolume = 1.0f;
    /** Master volume for Music */
    private float musicVolume = 0.3f;
    /** Set screen distance to calculate sound decay */
    private float decayDistance = 400f;
    /** Speed taken to transition music in miliseconds. */
    private float tradeRate = 0.0001f;
    /** Speed taken to transition music in miliseconds. */
    private float tradeThreshold = 0.0001f;
    /** Speed taken to transition music in miliseconds. */
    private float fadeOutRate = 0.02f;
    /** Speed taken to transition music in miliseconds. */
    private long tradeTime = 1000L;
    /** Whether or not fading in is complete */
    private boolean fadeInDone = false;
    /** Whether or not fading in is complete */
    private boolean fadeOutDone = false;
    /** Current preset being used for music. */
    private  int musicPreset;
    /** Structure to hold all music presets for future reference. */
    private ArrayMap<Integer, JsonValue> musicPresets;
    /** ArrayMap to link sfx names to Sound instances. */
    private ArrayMap<String, Sound> sfx;
    /** ArrayMap to link music names to Music instances. */
    private ArrayMap<String, Music> music;
    /** Whether or not a music trade is in progress. */
    private enum MusicState {
        SAFE,
        ENTER_DANGER,
        DANGER,
        LEAVE_DANGER,
    }
    private MusicState STATE;
    /** The singleton instance of the input controller */
    private static SoundController theController = null;
    /** The asset directory for getting new music. */
    private AssetDirectory directory;
    /** Boolean indicating whether music trading is in progress. */
    Trader fadeIn;
    Trader fadeOut;

    /**
     * @return the singleton instance of the input controller
     */
    public static SoundController getInstance() {
        if (theController == null) {
            theController = new SoundController();
        }
        return theController;
    }

    /**
     * Gather the all sound and music assets for this controller.
     *
     * This method extracts the asset variables from the given asset directory.
     * It should only be called after the asset directory is completed.
     * It should be called before an sfx or music is initiated.
     * Postcondition: Throws error if there is loading issue or sfx or music is null.
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        if (musicPresets == null) throw new NullPointerException("Constructor not called.");
        this.directory = directory;
        // Set sound settings values
        JsonValue set = directory.getEntry("sound_settings", JsonValue.class);
        sfxVolume = set.getFloat("sfx_volume", 1.0f);
        musicVolume = set.getFloat("music_volume", 1.0f);
        tradeRate = set.getFloat("trade_rate", 0.0001f);
        tradeThreshold = set.getFloat("trade_threshold", 0.00001f);
        tradeTime = set.getLong("trade_time", 1000L);
        // Get sfx
        JsonValue sfxnames = set.get("sound_names");
        for(JsonValue s : sfxnames){
            sfx.put(s.name(), directory.getEntry(s.name(), Sound.class));
        }
        // Get music presets
        JsonValue mscpresets = directory.getEntry("music_presets", JsonValue.class);
        int i = 0;
        for(JsonValue m : mscpresets){
            musicPresets.put(i, m);
            i++;
        }
    }

    /**
     * Check is a defensive function that simplifies checking whether the obtained sfx file is null.
     * @param name Sfx key name
     */
    private Sound checkSound(String name){
        Sound a = sfx.get(name);
        if (a == null) throw new RuntimeException(name + "is not a valid sfx name.");
        return a;
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
     * Constructor that initializes sfx and music variables.
     */
    public SoundController(){
        musicPresets = new ArrayMap<>();
        sfx = new ArrayMap<>();
        music = new ArrayMap<>();
        STATE = MusicState.SAFE;
    }

    /* TODO SETTINGS */

    /**
     * Sets the music according to the current preset stored.
     */
    private void setMusic(){
        music.clear();
        JsonValue mscpreset = musicPresets.get(musicPreset);
        for(JsonValue m : mscpreset){
            music.put(m.name(), directory.getEntry(m.asString(), Music.class));
        }
    }

    /**
     * Sets the values in sfx and music based on provided ids.
     * Must be called every WorldController or MenuController change due to memory constraints on sounds.
     * @param musicPreset is the JsonValue that contains text references to all sounds
     */
    public void setMusicPreset(int musicPreset){
        STATE = MusicState.SAFE;
        if (this.musicPreset != musicPreset){
            this.musicPreset = musicPreset;
            setMusic();
        }
    }

    /**
     * Changes the volume of all music files playing
     * @param name The music to avoid volume changes.
     */
    private void adjustMusicVolume(String name){
        for(String s: music.keys()){
            if(!s.equals(name)) {
                music.get(s).setVolume(musicVolume);
            }
        }
    }

    /**
     * Set master musicVolume from settings.
     * @param musicVolume Float between 0-1
     */
    public void setMasterMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
        switch(STATE){
            case SAFE:
                adjustMusicVolume("danger"); return;
            case DANGER:
                adjustMusicVolume("explore");
        }
    }

    /**
     * Set master musicVolume from settings.
     * @param sfxVolume Float between 0-1
     */
    public void setMasterSfxVolume(float sfxVolume) {
        this.sfxVolume = sfxVolume;
    }

    /**
     * @return music volume being played right now.
     */
    public float getMasterMusicVolume(){ return musicVolume; }

    /**
     *
     * @return sound effects volume being used right now.
     */
    public float getMasterSFXVolume(){ return sfxVolume; }

    /* TODO SFX PLAYERS */

    /**
     * Plays sfx with the filename name at given volume sfxvol. Returns if not found.
     * @param pan between -1 and 1.
     * @param sfxvol between 0 and 1.
     * @param name of sfx.
     * @param loop whether this sfx will loop.
     */
    private long playSFX(float pan, float sfxvol, String name, boolean loop){
        Sound s = checkSound(name);
        long id;
        if (loop) {
            id = s.loop(sfxvol, 1.0f, pan);
        } else {
            id = s.play(sfxvol, 1, pan);
        }
        return id;
    }

    /**
     * Plays sfx with the filename name at full sfxVolume. Returns if not found.
     * @param name of sfx
     * @param loop whether this sfx will loop.
     */
    public long playSFX(String name, boolean loop){
        return playSFX(0f, sfxVolume, name, loop);
    }

    public long playSFX(String name){
        return playSFX(0f, sfxVolume, name, false);
    }

    /**
     * Plays sfx with the filename at volume governed by distance.
     * @param name of sfx
     * @param distance of source from player.
     * @param loop whether this sfx will loop.
     */
    public long playSFX(String name, Vector2 distance, boolean loop){
        // Play at full volume, no pan
        if (distance.len() < decayDistance){
            playSFX(name, loop);
        }
        // Calculate new volume with v2 = v1 * r1/r2 and pan with x component
        return playSFX(distance.nor().x,sfxVolume * decayDistance / distance.len(), name, loop);
    }

    public void stopSFX(String name, long id){
        Sound s = sfx.get(name);

    }

    /* TODO MUSIC PLAYERS */

    /**
     * @param name
     */
    private float getMusicVolume(String name){
        return checkMusic(name).getVolume();
    }

    /**
     * Plays a music file at specified volume with reference index.
     * Precondition: volume > 0 and < musicVolume
     * @param index
     */
    private Music playMusic(String index, float volume){
        Music m = checkMusic(index);
        if (!m.isPlaying()) {
            m.play();
            m.setVolume(volume);
        }
        return m;
    }

    /**
     * Plays a music file at musicVolume with reference index at musicVolume.
     * @param index
     */
    private void playMusic(String index){
        playMusic(index, musicVolume);
    }

    /**
     *
     * @param index
     * @param volume
     * @param pos
     */
    private void playMusic(String index, int volume, float pos){
        Music m = checkMusic(index);
        if (!m.isPlaying()) {
            m.setVolume(volume);
            m.setPosition(pos);
            m.play();
        }
    }

    /**
     * For looping menu music only.
     * @param name
     * @param m
     */
    public void loopMusic(String name, Music m){
        m.play();
        m.setLooping(true);
        m.setVolume(musicVolume);
        music.put(name, m);
    }

    /**
     * Starts the music for a level, fails silently if proper preset is not loaded.
     */
    public void startLevelMusic(){
        if (musicPresets == null) throw new NullPointerException("Constructor not called.");
        System.out.println("start was called");
        STATE = MusicState.SAFE;
        setMusicVolume(0, "danger", "explore", "background");
        playMusic("danger", 0);
        playMusic("explore");
        playMusic("background");
        level_complete = false;
    }


    // TODO DYNAMIC MUSIC

    /**
     * Trades explore/danger music for soundtrack switching.
     * Returns if either name does not exist || name1 is not playing || name2 is playing
     * @param index1 either "explore" or "danger"
     * @param index2 same as index1 but reversed.
     */
    private boolean tradeMusic(String index1, String index2){
        // Gets music and check precondition
        Music m1 = checkMusic(index1);
        Music m2 = checkMusic(index2);
        if ((!m1.isPlaying() && m2.isPlaying())) return false;
        if(m1.isPlaying() && !m2.isPlaying()){
            playMusic(index2, 0, m1.getPosition());
        }
        // Otherwise both are still playing, in state of flux
        return true;
    }

    /**
     * Trades music with name1 out and name2 in for soundtrack switching if reverse is false.
     * Should be called only when the player situation CHANGES from EXPLORE to DANGER (2 enemies nearby).
     * @param reverse Whether danger is faded out for explore music
     */
    public void tradeMusic(boolean reverse){
        if(level_complete) return;
        if (STATE != MusicState.SAFE && reverse){
            if (tradeMusic("danger", "explore")){
                STATE = MusicState.LEAVE_DANGER;
            }
        }
        else if (STATE != MusicState.DANGER && !reverse){
            if (tradeMusic("explore", "danger")){
                STATE = MusicState.ENTER_DANGER;
            }
        }
    }


    /**
     * Takes the music with key index and decreases its volume by dv.
     * @param dv between 0-1
     * @param indices key of music
     */
    private void changeMusicVolume(float dv, String... indices){
        for(String s : indices){
            Music m = checkMusic(s);
            m.setVolume(m.getVolume()-dv);
        }
    }

    /**
     * Takes the music with key index and decreases its volume by dv.
     * @param volume between 0-1
     */
    private void setMusicVolume(float volume, String... indices){
        for(String s : indices){
            checkMusic(s).setVolume(volume);
        }
    }

    public void setLevelComplete(){
        level_complete = true;
    }

    /**
     * Incrementally fades out music from main gameplay loop.
     */
    public void fadeMusic() {
        if (level_complete) {
            if (getMusicVolume("explore") > tradeThreshold) {
                changeMusicVolume(fadeOutRate, "explore");
            }
            if (getMusicVolume("danger") > tradeThreshold) {
                changeMusicVolume(fadeOutRate, "danger");
            }
        }
    }

    /**
     * Fades out both explore and danger music in separate threads. BUGGY.
     */
    public void levelComplete(){
        fadeIn = new Trader("fadeOut", "danger", false);
        fadeOut = new Trader("fadeOut", "explore", false);
    }

    /**
     * Method to call every update loop to transition the music.
     */
    public void updateMusic(){
        if(level_complete) return;
        if(STATE == MusicState.ENTER_DANGER){
            if(music.get("explore").getVolume() < tradeThreshold){
                setMusicVolume(musicVolume, "danger");
                setMusicVolume(0, "explore");
                STATE = MusicState.DANGER;
            } else {
                changeMusicVolume(tradeRate, "explore");
                changeMusicVolume(-tradeRate, "danger");
            }
        }
        else if(STATE == MusicState.LEAVE_DANGER){
            if (music.get("danger").getVolume() < tradeThreshold){
                setMusicVolume(musicVolume, "explore");
                setMusicVolume(0, "danger");
                STATE = MusicState.SAFE;
            } else {
                changeMusicVolume(tradeRate, "danger");
                changeMusicVolume(-tradeRate, "explore");
            }
        }
    }

    private void setFadeInDone(boolean f){
        fadeInDone = f;
    }

    private void setFadeOutDone(boolean f){
        fadeOutDone = f;
    }

    private boolean FadeDone(){
        if(fadeInDone && fadeOutDone){
            setFadeInDone(false);
            setFadeOutDone(false);
            return true;
        }
        return false;
    }

    /**
     * Trades explore/danger music for soundtrack switching.
     * Returns if either name does not exist || name1 is not playing || name2 is playing
     * @param index either "explore" or "danger"
     */
    private void fadeIn(String index){
        if (music.get(index) == null) throw new RuntimeException();
        fadeIn = new Trader("fadeIn", index, true);
    }

    /**
     * Trades explore/danger music for soundtrack switching.
     * Returns if either name does not exist || name1 is not playing || name2 is playing
     * @param index either "explore" or "danger"
     */
    private void fadeOut(String index){
        if (music.get(index) == null) throw new RuntimeException();
        fadeOut = new Trader("fadeOut", index, false);
    }

    /**
     * Trades music with name1 out and name2 in for soundtrack switching if reverse is false.
     * Should be called only when the player situation CHANGES from EXPLORE to DANGER (2 enemies nearby).
     * @param reverse Whether danger is faded out for explore music
     */
    public void tradeThreadMusic(boolean reverse){
        switch (STATE){
            case SAFE:
                if (!reverse){
                    STATE = MusicState.ENTER_DANGER;
                    fadeIn("danger");
                    fadeOut("explore");
                } return;
            case DANGER:
                if(reverse){
                    STATE = MusicState.LEAVE_DANGER;
                    fadeIn("explore");
                    fadeOut("danger");
                }
        }
    }

    /**
     * Function to check whether fading is finished.
     */
    public void updateMusicThread(){
        switch (STATE) {
            case LEAVE_DANGER:
                if (FadeDone()) {
                    STATE = MusicState.SAFE;
                }
                return;
            case ENTER_DANGER:
                if (FadeDone()) {
                    STATE = MusicState.DANGER;
                }
                return;
        }
    }

    class Trader implements Runnable {

        // to stop the thread
        private boolean exit;
        private String index;
        private boolean fadeIn;
        Thread t;

        Trader(String threadname, String index, boolean fadeIn)
        {
            t = new Thread(this, threadname);
            this.index = index;
            this.fadeIn = fadeIn;
            exit = false;
            t.start();
        }

        // execution of thread starts from run() method
        public void run()
        {
            long timeStamp = System.currentTimeMillis();
            float percentage = (float) (System.currentTimeMillis() - timeStamp) / tradeTime;
            if(fadeIn){
                float maxVolume = musicVolume;
                while(percentage * maxVolume <= maxVolume && !exit){
                    music.get(index).setVolume(percentage * maxVolume);
                    print(index);
                    percentage = (float) (System.currentTimeMillis() - timeStamp) / tradeTime;
                }
                music.get(index).setVolume(percentage * maxVolume);
                setFadeInDone(true);
            } else {
                float minVolume = 0;
                float maxVolume = music.get(index).getVolume();
                while((1 - percentage) * maxVolume >= minVolume && !exit){
                    print(index);
                    music.get(index).setVolume((1 - percentage) * maxVolume);
                    percentage = (float) (System.currentTimeMillis() - timeStamp) / tradeTime;
                }
                music.get(index).setVolume((1 - percentage) * maxVolume);
                setFadeOutDone(true);
            }
        }

        // for stopping the thread
        public void stop()
        {
            exit = true;
        }
    }



    // TODO STOPPERS AND DISPOSE

    private void haltThreads(){
        if(fadeIn != null){
            fadeIn.stop();
        }
        if(fadeOut != null) {
            fadeOut.stop();
        }
    }


    /**
     * Stops all sound.
     */
    private void haltSFX(){
        for(Sound s : sfx.values()){
            s.stop();
        }
    }

    /**
     * Stops all music.
     */
    private void haltMusic(){
        haltThreads();
        for(Music m : music.values()){
            m.stop();
        }
    }

    /**
     * Stops all sfx and music and restores the state of the SoundController to normal.
     */
    public void haltSounds(){
        haltSFX();
        haltMusic();
    }

    /**
     * Disposes all sfx and music. Call when the game is closed.
     */
    public void dispose(){
        for(Sound s : sfx.values()){
            s.dispose();
        }
        for(Music m : music.values()){
            m.dispose();
        }
    }

    public void print(String name){
        System.out.println(name + " " + " volume: " + music.get(name).getVolume());
    }
}
