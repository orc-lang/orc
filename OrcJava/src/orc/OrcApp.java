package orc;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

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
public class OrcApp extends OrcGui {
	public OrcApp(Config config) {
		super(config);
	}
	
	@Override
	protected JFrame createFrame() {
		JFrame frame = super.createFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		return frame;
	}
	
	@Override
	protected JScrollPane createScrollPane() {
		JScrollPane scrollPane = super.createScrollPane();
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        return scrollPane;
	}
	
	public static void main(String[] args) {
		Application app = Application.getApplication();
		app.setEnabledPreferencesMenu(true);
		final Config defaultConfig = new Config();
		app.addApplicationListener(new ApplicationAdapter() {
			/** Open files are handled by starting a new engine running the file. */
			@Override
			public void handleOpenFile(final ApplicationEvent event) {
				try {
					final Config cfg = defaultConfig.clone();
					// Read configuration options from the environment and the command line
					cfg.setInputFile(new File(event.getFilename()));
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

			@Override
			public void handleAbout(ApplicationEvent event) {
				final AboutDialog dialog = new AboutDialog();
				invokeLater(new Runnable() {
					public void run() {
						dialog.pack();
						dialog.setVisible(true);
					}
				});
				// setHandled to keep default dialog from being shown
				event.setHandled(true);
			}

			@Override
			public void handlePreferences(ApplicationEvent event) {
				final PreferencesDialog dialog = new PreferencesDialog(defaultConfig);
				invokeLater(new Runnable() {
					public void run() {
						dialog.pack();
						dialog.setVisible(true);
					}
				});
			}
		});
		try {
			// prevent app from exiting while waiting for events
			new Object().wait();
		} catch (InterruptedException e) {
			// do nothing
		}
	}
	
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
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		}
	}
	
	private static class PreferencesDialog extends JDialog {
		public PreferencesDialog(final Config config) {
			super((JFrame)null, "Preferences");
			final JCheckBox typeChecking = new JCheckBox("Type checking enabled");
			typeChecking.setSelected(config.getTypeChecking());
			
			final JCheckBox noPrelude = new JCheckBox("Prelude disabled");
			noPrelude.setSelected(config.getNoPrelude());
			
			final JTextField includePath = new JTextField();
			includePath.setText(config.getIncludePath());
			
			JButton saveButton = new JButton("Save");
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					config.setTypeChecking(typeChecking.isSelected());
					config.setNoPrelude(noPrelude.isSelected());
					config.setIncludePath(includePath.getText());
					//config.setFullTraceFile(null);
					dispose();
				}
			});
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					dispose();
				}
			});
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
			buttons.add(Box.createHorizontalGlue());
			buttons.add(saveButton);
			buttons.add(Box.createHorizontalStrut(5));
			buttons.add(cancelButton);
			
			JPanel fields = new JPanel();
			fields.setLayout(new BoxLayout(fields, BoxLayout.PAGE_AXIS));
			fields.add(new JLabel("Include path - separate entries with "+
					System.getProperty("path.separator")));
			fields.add(includePath);
			fields.add(typeChecking);
			fields.add(noPrelude);
			
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.add(fields, BorderLayout.PAGE_START);
			contentPane.add(buttons, BorderLayout.PAGE_END);
			contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setContentPane(contentPane);
			setResizable(false);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		}
	}
}
