package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class SoundController {
    /** Master volume for SFX */
    private float sfxVolume = 1.0f;
    /** Master volume for Music */
    private float musicVolume = 1.0f;
    /** Set screen distance to calculate sound decay */
    private float decayDistance = 400f;
    /** Rate at which music is transitioned. */
    private float tradeRate = 0.01f;
    /** When transition is finished. */
    private float tradeThreshold = 0.05f;
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
    private enum MusicState {
        ENTER_DANGER,
        LEAVE_DANGER,
        STEADY
    }
    private MusicState STATE;

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
        if (!assertObjects(sfxPresets, musicPresets)) throw new NullPointerException("Constructor not called.");
        JsonValue dir = directory.getEntry("assets", JsonValue.class);

        // Set values
        sfxVolume = dir.getFloat("sfx_volume", 1.0f);
        musicVolume = dir.getFloat("music_volume", 1.0f);
        tradeRate = dir.getFloat("trade_rate", 0.01f);
        tradeThreshold = dir.getFloat("trade_threshold", 0.05f);
        for(JsonValue s : dir.get("sfx_presets")){
            sfxPresets.put(s.getInt("preset_number", 0), s);
        }
        for(JsonValue m : directory.getEntry("assets", JsonValue.class).get("music_presets")){
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
     * @param demo whether this SoundController is loaded for the Technical Prototype
     */
    public SoundController(boolean demo){
        sfxPresets = new ArrayMap<>();
        musicPresets = new ArrayMap<>();
        sfx = new ArrayMap<>();
        music = new ArrayMap<>();
        STATE = MusicState.STEADY;
        if (demo) {
            music.put("menu", Gdx.audio.newMusic(Gdx.files.internal("core/assets/sounds/Msc_Menu_RoTTheme.ogg")));
            music.put("explore", Gdx.audio.newMusic(Gdx.files.internal("core/assets/sounds/Msc_Explore_Longing.ogg")));
            music.put("danger", Gdx.audio.newMusic(Gdx.files.internal("core/assets/sounds/Msc_Danger_Longing.ogg")));
        }
    }

    /**
     * TODO Loads sfx with the appropriate sound instances.
     */
    private void setSFXs(){
        sfx.clear();
    }

    /**
     * TODO Loads music with the appropriate music instances.
     */
    private void setMusic(){
        music.clear();
    }

    /**
     * Sets the values in sfx and music based on provided ids.
     * Must be called every Controller change due to memory constraints on sounds.
     * @param sfxPreset is the JsonValue that contains text references to all sounds
     */
    public void setPresets(int sfxPreset, int musicPreset){
        STATE = MusicState.STEADY;
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
     * TODO Store id
     * Plays sfx with the filename name at given volume sfxvol. Returns if not found.
     * @param pan between -1 and 1.
     * @param sfxvol between 0 and 1.
     * @param name of sfx.
     * @param loop whether this sfx will loop.
     */
    private void playSFX(float pan, float sfxvol, String name, boolean loop){
        Sound s = sfx.get(name);
        if (s == null) return;
        long id;
        if (loop) {
            id = s.loop(sfxvol, 1.0f, pan);
        } else {
            id = s.play(sfxvol, 1, pan);
        }
    }

    /**
     * Plays sfx with the filename name at full sfxVolume. Returns if not found.
     * @param name of sfx
     * @param loop whether this sfx will loop.
     */
    public void playSFX(String name, boolean loop){
        playSFX(0f, sfxVolume, name, loop);
    }

    /**
     * Plays sfx with the filename at volume governed by distance.
     * @param name of sfx
     * @param distance of source from player.
     * @param loop whether this sfx will loop.
     */
    public void playSFX(String name, Vector2 distance, boolean loop){
        // Play at full volume, no pan
        if (distance.len() < decayDistance){
            playSFX(name, loop);
        }
        // Calculate new volume with v2 = v1 * r1/r2 and pan with x component
        playSFX(distance.nor().x,sfxVolume * decayDistance / distance.len(), name, loop);
    }

    /**
     * Plays a music file at specified volume with reference index.
     * Precondition: volume > 0 and < musicVolume
     * @param index
     */
    private Music playMusic(String index, float volume){
        Music m = music.get(index);
        if (m != null && !m.isPlaying()) {
            m.play();
            m.setVolume(volume);
        }
        return m;
    }

    /**
     * Plays a music file at musicVolume with reference index starting at position in seconds and 0 volume.
     * @param index
     */
    private void playMusic(String index, float volume, float position){
        Music m = playMusic(index, volume);
        if (m == null) return;
        m.setPosition(position);
    }

    /**
     * Plays a music file at musicVolume with reference index at musicVolume.
     * @param index
     */
    private void playMusic(String index){
        playMusic(index, musicVolume);
    }

    /**
     * Starts the music for a level, fails silently if proper preset is not loaded.
     */
    public void startLevelMusic(){
        playMusic("explore");
        playMusic("background");
    }

    /**
     * Starts the music for the menu, fails silently if proper preset is not loaded.
     */
    public void startMenuMusic(){
        playMusic("demo");
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
        if (!assertObjects(m1, m2) || (!m1.isPlaying() && m2.isPlaying())) return false;
        if( m1.isPlaying() && !m2.isPlaying()){
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
        // if (STATE != MusicState.STEADY) return;
        if (reverse){
            if (tradeMusic("danger", "explore")){
                STATE = MusicState.LEAVE_DANGER;
            }
        }
        else{
            if (tradeMusic("explore", "danger")){
                STATE = MusicState.ENTER_DANGER;
            }
        }
    }

    private void stopMusic(String index){
        Music m = music.get(index);
        if (m != null && m.isPlaying()) {
            m.stop();
        }
    }

    /**
     * Takes the music with key index and decreases its volume by dv.
     * @param index key of music
     * @param dv between 0-1
     */
    private float changeMusicVolume(String index, float dv){
        Music m = music.get(index);
        if (m != null && m.isPlaying()) {
            float nv = m.getVolume()-dv;
            m.setVolume(nv);
            return nv;
        }
        // Should never reach here
        throw new RuntimeException("Music file for " + index + " not found!");
    }

    /**
     * Takes the music with key index and decreases its volume by dv.
     * @param index key of music
     * @param volume between 0-1
     */
    private void setMusicVolume(String index, float volume){
        Music m = music.get(index);
        if (m != null && m.isPlaying()) {
            m.setVolume(volume);
        }
    }

    /**
     * Method to call every update loop to transition the music.
     */
    public void updateMusic(){
        float v;
        if(STATE == MusicState.ENTER_DANGER){
            v = changeMusicVolume("explore", tradeRate);
            changeMusicVolume("danger", -tradeRate);
            if (v < tradeThreshold){
                stopMusic("explore");
                setMusicVolume("danger", musicVolume);
                STATE = MusicState.STEADY;
            }
        }
        else if(STATE == MusicState.LEAVE_DANGER){
            v = changeMusicVolume("danger", tradeRate);
            changeMusicVolume("explore", -tradeRate);
            if (v < tradeThreshold){
                stopMusic("danger");
                setMusicVolume("explore", musicVolume);
                STATE = MusicState.STEADY;
            }
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
