package edu.cornell.gdiac.raftoftheseus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class SoundController {
    /** Master volume for SFX */
    private float sfxVolume = 1.0f;
    /** Master volume for Music */
    private float musicVolume = 1.0f;
    /** Json to hold all sfx names for future reference. */
    private JsonValue sfxNames;
    /** Json to hold all music names for future reference. */
    private JsonValue musicNames;
    /** ArrayMap to link sfx names to Sound instances. */
    private ArrayMap<String, Sound> sfx;
    /** ArrayMap to link music names to Music instances. */
    private ArrayMap<String, Music> music;
    /** AssetDirectory instance to change loaded music and sound. */
    AssetDirectory dir;

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
        dir = directory;
        sfxNames = dir.getEntry("sfx", JsonValue.class);
        musicNames = dir.getEntry("music", JsonValue.class);
        if (!assertObjects(sfxNames, musicNames)) throw new NullPointerException("Sound and music not loaded properly.");
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
        sfx = new ArrayMap<>();
        music = new ArrayMap<>();
    }

    /**
     * Sets the values in sfx and music based on provided JsonValue.
     * Must be called every Controller change due to memory constraints on sounds.
     * @param sounds is the JsonValue that contains text references to all sounds
     */
    public void setSounds(JsonValue sounds){
        sounds.getString("wood", "");
    }

    /**
     * Set master musicVolume from settings.
     * @param musicVolume Float between 0-1
     */
    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

    /**
     * Set master musicVolume from settings.
     * @param sfxVolume Float between 0-1
     */
    public void setSfxVolume(float sfxVolume) {
        this.sfxVolume = sfxVolume;
    }

    /**
     * Plays sfx with the filename name. Returns if not found.
     * @param name of sfx
     */
    public void playSound(String name){
        Sound s = sfx.get(name);
        if (s == null) return;
        long id = s.play();
        s.setVolume(id, sfxVolume);
    }

    /**
     * Plays music with the filename name. Returns if not found.
     * @param name of music
     */
    public void playMusic(String name){
        Music m = music.get(name);
        if (m == null) return;
        if (!m.isPlaying()) {
            m.play();
            m.setVolume(musicVolume);
        }
    }

    /**
     * Trades music with name1 out and name2 in for soundtrack switching.
     * Changes volume linearly.
     * Returns if either name does not exist || name1 is not playing || name2 is playing
     * @param name1 background music (explore/battle)
     * @param name2 background music (battle/explore) different version of name1
     */
    public void tradeMusic(String name1, String name2){
        // Gets music and check precondition
        Music m1 = music.get(name1);
        Music m2 = music.get(name2);
        if (!assertObjects(m1, m2) || !m1.isPlaying() || m2.isPlaying()) return;

        playMusic(name2);
        // Decrease volume of m1 and increase volume of m2, run on separate thread.

    }

    /**
     * Trades music with name1 out and name2 in for soundtrack switching if reverse is false.
     * @param name1 background music (explore/battle)
     * @param name2 background music (battle/explore) different version of name1
     * @param reverse true if name2 is faded out.
     */
    public void tradeMusic(String name1, String name2, boolean reverse){
        if (reverse){
            tradeMusic(name2, name1);
        }
        else{
            tradeMusic(name1, name2);
        }
    }
}
