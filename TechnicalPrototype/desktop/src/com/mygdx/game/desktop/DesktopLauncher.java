package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import edu.cornell.gdiac.raftoftheseus.GDXRoot;
import com.badlogic.gdx.Files;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1000;
		config.height = 900;
		config.title = "Raft of Theseus";
		config.addIcon("images/icon.png", Files.FileType.Internal);
		new LwjglApplication(new GDXRoot(), config);
	}
}
