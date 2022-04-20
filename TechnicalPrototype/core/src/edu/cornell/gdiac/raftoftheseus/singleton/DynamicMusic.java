package edu.cornell.gdiac.raftoftheseus.singleton;

import com.badlogic.gdx.audio.Music;

public class DynamicMusic implements Runnable {
    // the music
    private Music music;
    // identifiers
    private String index;
    // booleans
    private boolean fadeIn;
    private boolean doneFade;
    private boolean exit;
    Thread t;

    public DynamicMusic(String index, Music m)
    {
        music = m;
        this.index = index;
        this.fadeIn = false;
        exit = false;
    }

    /**
     * @param fadeIn Whether this music should fade in or not
     */
    public void setFadeIn(boolean fadeIn) {
        this.fadeIn = fadeIn;
    }

    /**
     * @return Whether this music is done fading or not.
     */
    public boolean getDoneFade(){
        return doneFade;
    }

    /**
     * Method to start the thread to fade in or out the music.
     */
    public void start(){
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
     * Fades in this music file.
     */
    private void fadeIn(){
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
            doneFade = true;
            music.setVolume(maxVolume);
        }
    }

    /**
     * Fades out this music file.
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

    /** To stop the thread when changing screens. */
    public void stopThread() { exit = true; }

    /** To print out the volume for debugging. */
    public void print(String name){ System.out.println(name + " " + " volume: " + music.getVolume()); }
}
