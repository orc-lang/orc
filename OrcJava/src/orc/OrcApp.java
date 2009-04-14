package orc;

import java.io.File;
import java.util.concurrent.Executor;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.kohsuke.args4j.CmdLineException;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * First attempt at a Mac OS X App interface for Orc.
 * This has not been tested and probably doesn't work.
 * @author quark
 */
public class OrcApp {
	public static void main(String[] args) throws InterruptedException {
		Application app = Application.getApplication();
		app.addApplicationListener(new ApplicationAdapter() {
			/** Open files are handled by starting a new engine running the file. */
			@Override
			public void handleOpenFile(final ApplicationEvent event) {
				try {
					// Read configuration options from the environment and the command line
					final Config cfg = new Config();
					cfg.setInputFile(new File(event.getFilename()));
					new Thread() {
						public void run() {
							OrcGUI.run(cfg, JFrame.HIDE_ON_CLOSE);
						}
					}.start();
				} catch (CmdLineException e) {
					// should never happen
					throw new AssertionError(e);
				}
			}

			/** Quit events are always accepted. */
			@Override
			public void handleQuit(ApplicationEvent event) {
				event.setHandled(true);
			}
		});
		// prevent app from exiting while waiting for events
		new Object().wait();
	}
}
