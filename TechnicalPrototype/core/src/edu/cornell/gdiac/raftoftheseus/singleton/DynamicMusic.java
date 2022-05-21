package edu.cornell.gdiac.raftoftheseus.singleton;

import com.badlogic.gdx.audio.Music;
import edu.cornell.gdiac.raftoftheseus.model.util.Timer;

/**
 * Wrapper class to allow control of music dynamics based on an FSM.
 *
 * Postcondition: When the thread stops running, the music will either be at the maximum volume or minimum
 * and doneFade = true;
 */
public class DynamicMusic implements Runnable {
    // the music
    private Music music;
    // identifiers
    private String index;

    /** Whether this music is in the process or has already faded in/out. */
    private boolean fadeIn;
    /** Whether this music is done fading in or out. */
    private boolean doneFade = true;
    /** Whether to stop this thread. */
    private boolean exit;
    /** The thread to run the dynamic changes on. */
    Thread t;

    /** Timestamp */
    private Timer timeStamp;

    public DynamicMusic(String index, Music m)
    {
        music = m;
        this.index = index;
        this.fadeIn = false;
        exit = false;
        timeStamp = new Timer();
    }

    /**
     * Fade in this music.
     * Changes the looping boolean and starts the new thread if necessary.
     */
    public void FadeIn() {
        this.fadeIn = true;
        if(doneFade){
            start();
        }
    }

    /**
     * Fade out this music.
     * Changes the looping boolean and starts the new thread if necessary.
     */
    public void FadeOut(){
        this.fadeIn = false;
        if(doneFade){
            start();
        }
    }

    /**
     * @return Whether this music is done fading or not.
     */
    public boolean isDoneFade(){
        return doneFade;
    }

    /**
     * Method to start the thread to fade in or out the music.
     */
    private void start(){
        doneFade = false;
        t = new Thread(this, index);
        t.start();
    }

    /**
     * Method to either increase or decrease music volume depending on booleans set.
     */
    @Override
    public void run() {
        while(!doneFade && !exit){
            if(fadeIn) fadeIn();
            else fadeOut();
        }
    }

    /**
     * Fades in this music file while it can still increase its volume.
     */
    private void fadeIn(){
        print("fading in");
        long timeStamp = System.currentTimeMillis();
        float percentage = (float) (System.currentTimeMillis() - timeStamp) / MusicController.getTradeTime();
        float maxVolume = MusicController.getMusicVolume();
        // Loop unless boolean changes
        while(canIncrease(percentage, maxVolume) && canFadeIn()){
            music.setVolume(percentage * maxVolume);
            percentage = (float) (System.currentTimeMillis() - timeStamp) / MusicController.getTradeTime();
        }
        // Fade is finished only if exit is not called and fadeIn is still true
        if(!canIncrease(percentage, maxVolume) && canFadeIn()){
            music.setVolume(maxVolume);
            doneFade = true;
        }
    }

    /**
     * Fades out this music file while it can still decrease its volume.
     */
    private void fadeOut(){
        long timeStamp = System.currentTimeMillis();
        float percentage = (float) (System.currentTimeMillis() - timeStamp) / MusicController.getTradeTime();
        float minVolume = 0;
        float maxVolume = music.getVolume();
        // Loop unless boolean changes
        while(canDecrease(percentage, maxVolume, minVolume) && canFadeOut()){
            music.setVolume((1 - percentage) * maxVolume);
            percentage = (float) (System.currentTimeMillis() - timeStamp) / MusicController.getTradeTime();
        }
        // Fade is finished
        if(!canDecrease(percentage, maxVolume, minVolume) && canFadeOut()) {
            music.setVolume(minVolume);
            doneFade = true;
        }
    }

    /** @return whether fading in is possible in its given state */
    private boolean canFadeIn(){
        return !exit && fadeIn;
    }
    /** @return whether fading out is possible in its given state */
    private boolean canFadeOut() {
        return !exit && !fadeIn;
    }
    /** @return whether the music can further increase its volume. */
    private boolean canIncrease(float percentage, float maxVolume){
        return percentage * maxVolume <= maxVolume;
    }
    /** @return whether the music can further decrease its volume. */
    private boolean canDecrease(float percentage, float maxVolume, float minVolume){
        return (1 - percentage) * maxVolume >= minVolume;
    }

    /** Set FadeIn to be true for core music to allow dynamic changes. */
    public void setFadeIn(boolean fadeIn){ this.fadeIn = fadeIn; }

    /** To check what state this Dynamic music is in. */
    public boolean isFadeIn(){ return fadeIn; }

    /** To stop the thread when changing screens. */
    public void stopThread() { exit = true; }

    /** To reset the thread to allow running. */
    public void resetThread() { exit = false; }

    /** To access the music file to allow playing */
    public Music getMusic(){ return music; }

    /** To print out the volume for debugging. */
    public void print(String name){ System.out.println(name + " " + " volume: " + music.getVolume()); }
}
