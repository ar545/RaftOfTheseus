package edu.cornell.gdiac.optimize.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import edu.cornell.gdiac.optimize.GDXRoot;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width  = 800;
        config.height = 600;
        config.resizable = false;
		config.title = "Raft of Theseus";
		config.addIcon("images/icon.png", Files.FileType.Internal);
		new LwjglApplication(new GDXRoot(), config);
	}
}
