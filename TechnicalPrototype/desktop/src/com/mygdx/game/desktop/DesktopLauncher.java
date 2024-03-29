package com.mygdx.game.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import edu.cornell.gdiac.raftoftheseus.GDXRoot;
import com.badlogic.gdx.Files;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
//		config.width = 1280;
//		config.height = 720;
//		config.fullscreen = true;
		config.width = LwjglApplicationConfiguration.getDesktopDisplayMode().width;
		config.height = LwjglApplicationConfiguration.getDesktopDisplayMode().height;
		config.foregroundFPS = 60; // cap fps
		config.vSyncEnabled = false; // prevent physics from bugging out due to lower framerate
		config.resizable = false;
		config.title = "Raft of Theseus";
		config.addIcon("images/icon.png", Files.FileType.Internal);
		config.addIcon("images/icon2.png", Files.FileType.Internal);
		new LwjglApplication(new GDXRoot(), config);
	}
}
