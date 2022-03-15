package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class SoundController {
    /** Master volume for SFX */
    private float sfxVolume = 1.0f;
    /** Master volume for Music */
    private float musicVolume = 1.0f;
    /** Set screen distance to calculate sound decay */
    private float decayDistance = 400f;
    /** Current preset being used for sfx. */
    private int sfxPreset;
    /** Current preset being used for music. */
    private  int musicPreset;
    /** Structure to hold all sfx presets for future reference. */
    private ArrayMap<Integer, JsonValue> sfxPresets;
    /** Structure to hold all music presets for future reference. */
    private ArrayMap<Integer, JsonValue> musicPresets;
    /** ArrayMap to link sfx names to Sound instances. */
    private ArrayMap<String, Sound> sfx;
    /** ArrayMap to link music names to Music instances. */
    private ArrayMap<String, Music> music;
    /** Whether or not a music trade is in progress. */
    private enum TradeState {
        ENTER_DANGER,
        LEAVE_DANGER,
        STEADY
    }
    private TradeState STATE;

    /**
     * Gather the all sound and music assets for this controller.
     *
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     * Postcondition: Throws error if there is loading issue or sfx or music is null
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        if (!assertObjects(sfxPresets, musicPresets)) throw new NullPointerException("Constructor not called.");
        for(JsonValue s : directory.getEntry("sfx_presets", JsonValue.class)){
            sfxPresets.put(s.getInt("preset_number", 0), s);
        }
        for(JsonValue m : directory.getEntry("music_presets", JsonValue.class)){
            musicPresets.put(m.getInt("preset_number", 0), m);
        }
    }

    /**
     * Checks whether both a and b are not null.
     * @param a Any object that needs to be referenced.
     * @param b Same as a.
     */
    private boolean assertObjects(Object a, Object b){
        return a != null && b != null;
    }

    /**
     * Constructor that initializes sfx and music variables.
     */
    public SoundController(){
        sfxPresets = new ArrayMap<>();
        musicPresets = new ArrayMap<>();
        sfx = new ArrayMap<>();
        music = new ArrayMap<>();
        STATE = TradeState.STEADY;
    }

    /**
     * Loads sfx with the appropriate sound instances.
     */
    private void setSFXs(){

    }

    /**
     * Loads music with the appropriate music instances.
     */
    private void setMusic(){}

    /**
     * Sets the values in sfx and music based on provided ids.
     * Must be called every Controller change due to memory constraints on sounds.
     * @param sfxPreset is the JsonValue that contains text references to all sounds
     */
    public void setPresets(int sfxPreset, int musicPreset){
        STATE = TradeState.STEADY;
        if (this.sfxPreset != sfxPreset){
            this.sfxPreset = sfxPreset;
            setSFXs();
        }
        if (this.musicPreset != musicPreset){
            this.musicPreset = musicPreset;
            setMusic();
        }
    }

    /**
     * Set master musicVolume from settings.
     * @param musicVolume Float between 0-1
     */
    public void setMasterMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

    /**
     * Set master musicVolume from settings.
     * @param sfxVolume Float between 0-1
     */
    public void setMasterSfxVolume(float sfxVolume) {
        this.sfxVolume = sfxVolume;
    }

    /**
     * Plays sfx with the filename name at given volume sfxvol. Returns if not found.
     * @param name of sfx
     * @param sfxvol between 0 and 1.
     */
    private void playSFX(float sfxvol, String name){
        Sound s = sfx.get(name);
        if (s == null) return;
        long id = s.play();
        s.setVolume(id, sfxvol);
    }

    /**
     * Plays sfx with the filename name at full sfxVolume. Returns if not found.
     * @param name of sfx
     */
    public void playSFX(String name){
        playSFX(sfxVolume, name);
    }

    /**
     * Plays sfx with the filename at volume governed by distance.
     * @param name of sfx
     * @param distance of source from player
     */
    public void playSFX(String name, float distance){
        // Play at full volume
        if (distance < decayDistance){
            playSFX(name);
        }
        // Calculate new volume with v2 = v1 * r1/r2
        playSFX(sfxVolume * decayDistance / distance, name);
    }

    /**
     * Plays the explore music of the preset. Returns if not found.
     */
    private void playMusic(String index){
        Music m = music.get(index);
        if (m == null) return;
        if (!m.isPlaying()) {
            m.play();
            m.setVolume(musicVolume);
        }
    }

    private void playBackgroundMusic(){
        playMusic("background");
    }

    private void playExploreMusic(){
        playMusic("explore");
    }

    private void playDangerMusic(){
        playMusic("danger");
    }

    public void startMusic(){
        playExploreMusic();
        playBackgroundMusic();
    }

    /**
     * Trades explore/danger music for soundtrack switching.
     * Returns if either name does not exist || name1 is not playing || name2 is playing
     * @param index1 either "explore" or "danger"
     * @param index2 same as index1 but reversed.
     */
    private boolean tradeMusic(String index1, String index2){
        // Gets music and check precondition
        Music m1 = music.get(index1);
        Music m2 = music.get(index2);
        if (!assertObjects(m1, m2) || !m1.isPlaying() || m2.isPlaying()) return false;
        playMusic(index2);
        return true;
    }

    /**
     * Trades music with name1 out and name2 in for soundtrack switching if reverse is false.
     * Should be called only when the player situation CHANGES from EXPLORE to DANGER (2 enemies nearby).
     * @param reverse Whether danger is faded out for explore music
     */
    public void tradeMusic(boolean reverse){
        if (STATE != TradeState.STEADY) return;
        if (reverse){
            if (tradeMusic("danger", "explore")){
                STATE = TradeState.LEAVE_DANGER;
            }
        }
        else{
            if (tradeMusic("explore", "danger")){
                STATE = TradeState.ENTER_DANGER;
            }
        }
    }

    private void setMusicVolume(){

    }

    private void setSfxVolume(){

    }

    public void updateMusic(){
        if(STATE == TradeState.ENTER_DANGER){
            setMusicVolume();
            setMusicVolume();
        }
        if(STATE == TradeState.LEAVE_DANGER){
            setMusicVolume();
            setMusicVolume();
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
        for(Music m : music.values()){
            m.stop();
        }
    }

    /**
     * Stops all sfx and music.
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
}
