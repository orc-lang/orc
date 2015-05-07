package orc.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.Executor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeListener;

import orc.Config;

import org.kohsuke.args4j.CmdLineException;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * A basic Mac OS X App interface for Orc.
 * Supports drag-and-drop, Preferences, and About.
 * 
 * <p>Refer to http://developer.apple.com/documentation/Java/Conceptual/Java14Development/00-Intro/JavaDevelopment.html
 * @author quark
 */
public class OrcApp extends OrcGui {
	public OrcApp(Config config) {
		super(config);
	}
	
	@Override
	protected JFrame createFrame() {
		JFrame frame = super.createFrame();
		// we don't want to exit since the same app may run
		// multiple Orc programs
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		// terminate the engine when the window is closed
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				engine.terminate();
			}
		});
		return frame;
	}
	
	@Override
	protected JScrollPane createScrollPane() {
		JScrollPane scrollPane = super.createScrollPane();
		// Mac OS X interface policy says to always show scrollbars
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        return scrollPane;
	}
	
	@Override
	protected JMenuBar createMenuBar() {
		// OS X can't handle buttons in the menu bar
		// so we have to use a menu instead
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("Run");
        menu.setMnemonic(KeyEvent.VK_R);
        menu.add(new JMenuItem(pause));
        menu.add(new JMenuItem(resume));
        menu.add(new JMenuItem(stop));
        bar.add(menu);
        return bar;
	}
	
	/**
	 * Main method; starts Apple event listeners and not much else.
	 * This doesn't expect to receive any command-line arguments.
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0) throw new AssertionError(
				"I didn't expect command-line arguments; run me by opening my bundle.");
		Application app = Application.getApplication();
		app.setEnabledPreferencesMenu(true);
		// this is where we store the default configuration settings
		// to be used when a script is opened.
		final Config defaultConfig = new Config();
		
		// create the about dialog ahead of time
		// so it shows quickly; this also keeps
		// the app from exiting at the end of main
		final AboutDialog about = new AboutDialog();
		about.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		invokeLater(new Runnable() {
			public void run() {
				// pack but don't show
				about.pack();
			}
		});
				
		// register our Apple event handlers
		app.addApplicationListener(new ApplicationAdapter() {
			/** Open files are handled by starting a new engine running the file. */
			@Override
			public void handleOpenFile(final ApplicationEvent event) {
				try {
					final Config cfg = defaultConfig.clone();
					cfg.setInputFile(new File(event.getFilename()));
					// start the engine in a new thread so we don't
					// block the event thread
					new Thread(new OrcApp(cfg)).start();
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

			/** About events are handled by starting our about dialog. */
			@Override
			public void handleAbout(ApplicationEvent event) {
				invokeLater(new Runnable() {
					public void run() {
						about.setVisible(true);
					}
				});
				// setHandled to keep default dialog from being shown
				event.setHandled(true);
			}

			/** Preferences events are handled by starting our preferences dialog. */
			@Override
			public void handlePreferences(ApplicationEvent event) {
				invokeLater(new PreferencesDialog(defaultConfig));
			}
		});
	}
	
	/**
	 * The "About" dialog.
	 * Shows our logo and copyright information.
	 * @author quark
	 */
	private static class AboutDialog extends JDialog {
		public AboutDialog() {
			// The following is based losely on JUnit's about dialog.
			// Here's the layout:
			//
			//     0          1
			//  |----------------------|
			//  |      |     title     | 0
			//  | icon |---------------|
			//  |      |    subtitle   | 1
			//  |      |---------------|
			//  |      |    copyright  | 2
			//  |----------------------|
			//  |        close         | 3
			//  |----------------------|
			//
			super((JFrame)null, "About");
			JButton close = new JButton("Close");
			close.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			getRootPane().setDefaultButton(close);
			JLabel title = new JLabel("Orc Runtime");
			Font font = title.getFont();
			title.setFont(font.deriveFont(Font.PLAIN, 36));
			JLabel version = new JLabel("Version "+System.getProperty("orc.version"));
			version.setFont(font.deriveFont(Font.PLAIN, 14));
			JLabel copyright = new JLabel("Copyright (c) 2008, The University of Texas at Austin");
			copyright.setFont(font.deriveFont(Font.PLAIN, 12));
			JLabel logo = new JLabel(new ImageIcon(
					OrcApp.class.getResource("about-logo.png"), "Orc logo"));
			
			JPanel contentPane = new JPanel(new GridBagLayout());
			
			GridBagConstraints logoC = new GridBagConstraints();
			logoC.gridx = 0; logoC.gridy = 0;
			logoC.gridwidth = 1; logoC.gridheight = 3;
			logoC.anchor = GridBagConstraints.CENTER;
			logoC.insets = new Insets(0, 0, 0, 10);
			contentPane.add(logo, logoC);
			
			GridBagConstraints titleC = new GridBagConstraints();
			titleC.gridx = 1; titleC.gridy = 0;
			titleC.gridwidth = 1; titleC.gridheight = 1;
			titleC.anchor = GridBagConstraints.CENTER;
			contentPane.add(title, titleC);
			
			GridBagConstraints versionC = new GridBagConstraints();
			versionC.gridx = 1; versionC.gridy = 1;
			versionC.gridwidth = 1; versionC.gridheight = 1;
			versionC.anchor = GridBagConstraints.CENTER;
			contentPane.add(version, versionC);
			
			GridBagConstraints copyrightC = new GridBagConstraints();
			copyrightC.gridx = 1; copyrightC.gridy = 2;
			copyrightC.gridwidth = 1; copyrightC.gridheight = 1;
			copyrightC.anchor = GridBagConstraints.CENTER;
			contentPane.add(copyright, copyrightC);
			
			GridBagConstraints closeC = new GridBagConstraints();
			closeC.gridx = 0; closeC.gridy = 3;
			closeC.gridwidth = 2; closeC.gridheight = 1;
			closeC.anchor = GridBagConstraints.CENTER;
			closeC.insets = new Insets(10, 0, 0, 0);
			contentPane.add(close, closeC);
			
			contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			setContentPane(contentPane);
			setResizable(false);
		}
	}
	
	/**
	 * Preferences dialog, used to set config properties that
	 * would normally be set via the command line.
	 * @author quark
	 */
	protected static final class PreferencesDialog extends JDialog implements Runnable {
		public PreferencesDialog(final Config config) {
			final ConfigPanel content = new ConfigPanel();
			content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setContentPane(content);
			setResizable(false);
			content.load(config);
			
			content.add(Box.createVerticalStrut(10));
			
			JLabel note = new JLabel("Note: changes will not apply to currently-running scripts.");
			note.setFont(note.getFont().deriveFont(Font.ITALIC, 14));
			note.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
			content.add(note);
			
			ButtonPanel buttons = new ButtonPanel();
			JButton saveButton = new JButton("Save");
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						content.save(config);
					} catch (CmdLineException e) {
						JOptionPane.showMessageDialog(PreferencesDialog.this,
								e.getLocalizedMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					dispose();
				}
			});
			buttons.add(saveButton);
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					dispose();
				}
			});
			buttons.add(cancelButton);
			content.add(buttons);
		}
		
		public void run() {
			pack();
			setVisible(true);
		}
	}
}
