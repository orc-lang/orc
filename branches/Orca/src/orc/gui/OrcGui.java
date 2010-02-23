//
// OrcGui.java -- Java class OrcGui
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.gui;

import static javax.swing.SwingUtilities.invokeLater;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import orc.Config;
import orc.OrcCompiler;
import orc.ast.oil.expression.Expression;
import orc.error.SourceLocation;
import orc.error.compiletime.CompileMessageRecorder;
import orc.error.runtime.TokenException;
import orc.progress.ProgressMonitorListener;
import orc.runtime.OrcEngine;
import orc.runtime.values.Value;

import org.kohsuke.args4j.CmdLineException;

/**
 * A basic GUI interface for Orc.
 * This outputs to its own window instead of the console.
 * It also provides pause/resume/stop buttons in the menubar.
 * You still need to start the GUI from the command line.
 * 
 * @author quark
 */
public class OrcGui implements Runnable {
	protected Config config;
	protected OrcEngine engine;

	protected Action pause = new AbstractAction("Pause") {
		{
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
		}

		public void actionPerformed(final ActionEvent e) {
			engine.pause();
			setEnabled(false);
			resume.setEnabled(true);
		}
	};
	protected Action resume = new AbstractAction("Resume") {
		{
			putValue(MNEMONIC_KEY, KeyEvent.VK_R);
			setEnabled(false);
		}

		public void actionPerformed(final ActionEvent e) {
			engine.unpause();
			setEnabled(false);
			pause.setEnabled(true);
		}
	};
	protected Action stop = new AbstractAction("Stop") {
		{
			putValue(MNEMONIC_KEY, KeyEvent.VK_S);
		}

		public void actionPerformed(final ActionEvent e) {
			engine.terminate();
			setEnabled(false);
			pause.setEnabled(false);
			resume.setEnabled(false);
		}
	};
	private StyledDocument document;
	private JScrollBar scrollBar;

	public OrcGui(final Config config) {
		this.config = config;
	}

	public static void main(final String[] args) {
		final Config config = new Config();
		if (args.length > 0) {
			config.processArgs(args);
			new Thread(new OrcGui(config)).start();
		} else {
			invokeLater(new OpenDialog(config));
		}
	}

	protected static void error(final String title, final String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}

	protected JFrame createFrame() {
		final JFrame frame = new JFrame(config.getInputFilename());
		frame.setPreferredSize(new Dimension(640, 480));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		return frame;
	}

	protected JScrollPane createScrollPane() {
		return new JScrollPane();
	}

	protected JMenuBar createMenuBar() {
		final JMenuBar bar = new JMenuBar();
		bar.add(new JButton(pause));
		bar.add(new JButton(resume));
		bar.add(new JButton(stop));
		return bar;
	}

	public void run() {

		final ProgressMonitorListener progress = new ProgressMonitorListener(null, "Compiling " + config.getInputFilename(), "");
		config.setProgressListener(progress);
		final CompileMessageRecorder messageRecorder = new CompileMessageRecorder() {
			public void beginProcessing(final File file) {
			}

			public void endProcessing(final File file) {
			}

			public Severity getMaxSeverity() {
				return null;
			}

			public void recordMessage(final Severity severity, final int code, final String message, final SourceLocation location, final Object astNode, final Throwable exception) {
				error("Compilation Error", message);
			}

			public void recordMessage(final Severity severity, final int code, final String message, final SourceLocation location, final Throwable exception) {
				recordMessage(severity, code, message, location, null, exception);
			}

			public void recordMessage(final Severity severity, final int code, final String message, final SourceLocation location, final Object astNode) {
				recordMessage(severity, code, message, location, astNode, null);
			}

			public void recordMessage(final Severity severity, final int code, final String message) {
				recordMessage(severity, code, message, null, null, null);
			}
		};
		config.setMessageRecorder(messageRecorder);

		final OrcCompiler compiler = new OrcCompiler(config);
		Expression ex;
		try {
			ex = compiler.call();
		} catch (final IOException e) {
			error("IO Error", e.getMessage());
			return;
		}
		if (ex == null) {
			return;
		}

		// initialize document
		final JTextPane pane = new JTextPane();
		pane.setEditable(false);
		document = pane.getStyledDocument();

		// initialize document styles
		final Style plain = document.addStyle("plain", StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE));
		Style s;
		s = document.addStyle("print", plain);
		StyleConstants.setFontFamily(s, "SansSerif");

		s = document.addStyle("publish", plain);
		StyleConstants.setFontFamily(plain, "Monospaced");
		StyleConstants.setBold(s, true);

		s = document.addStyle("error", plain);
		StyleConstants.setFontFamily(plain, "Monospaced");
		StyleConstants.setForeground(s, Color.RED);
		StyleConstants.setBold(s, true);

		// initialize frame
		final JFrame frame = createFrame();
		final JScrollPane scrollPane = createScrollPane();
		scrollPane.setViewportView(pane);
		scrollBar = scrollPane.getVerticalScrollBar();
		frame.setJMenuBar(createMenuBar());
		frame.getContentPane().add(scrollPane);

		// Configure the engine
		engine = new OrcEngine(config) {
			private void output(final String style, final String message) {
				invokeLater(new Runnable() {
					public void run() {
						try {
							document.insertString(document.getLength(), message, document.getStyle(style));
						} catch (final BadLocationException e) {
							throw new AssertionError(e);
						}
						scrollBar.setValue(scrollBar.getMaximum());
					}
				});
			}

			@Override
			public void print(final String string, final boolean newline) {
				output("print", string + (newline ? "\n" : ""));
			}

			@Override
			public void onPublish(final Object v) {
				output("publish", Value.write(v) + "\n");
			}

			@Override
			public void onTerminate() {
				invokeLater(new Runnable() {
					public void run() {
						pause.setEnabled(false);
						resume.setEnabled(false);
						stop.setEnabled(false);
					}
				});
			}

			@Override
			public void onError(final TokenException problem) {
				output("error", "Error: " + problem.getMessage() + "\n");
				output("error", "Backtrace:\n");
				final SourceLocation[] backtrace = problem.getBacktrace();
				for (final SourceLocation location : backtrace) {
					output("error", location + "\n");
				}
				output("error", "\n");
			}
		};

		progress.setProgress(1);
		if (progress.isCanceled()) {
			return;
		}

		// display the frame
		invokeLater(new Runnable() {
			public void run() {
				frame.pack();
				frame.setVisible(true);
			}
		});

		// Run the Orc program
		engine.run(ex);
	}

	/**
	 * Opens automatically when the program launches.
	 * Prompts the user to choose a file as well as
	 * specify configuration options.
	 * @author quark
	 */
	protected static final class OpenDialog extends JDialog implements Runnable {
		private final FileField inputFile = new FileField("Open Orc Script", true, new FileFilter() {
			@Override
			public boolean accept(final File f) {
				return f.isDirectory() || f.getName().endsWith(".orc") || f.getName().endsWith(".oil");
			}

			@Override
			public String getDescription() {
				return "Orc Scripts";
			}

		});
		private final JButton runButton = new JButton("Run");
		{
			runButton.setEnabled(false);
			inputFile.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					runButton.setEnabled(e.getActionCommand().length() > 0);
				}
			});
		}

		public OpenDialog(final Config config) {
			final JPanel content = new OneColumnPanel();
			content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setContentPane(content);
			setResizable(false);

			final JLabel inputFileLabel = new JLabel("Read Orc script from...");
			content.add(inputFileLabel);
			content.add(inputFile);

			final ConfigPanel configPanel = new ConfigPanel();
			configPanel.load(config);
			content.add(configPanel);

			final ButtonPanel buttons = new ButtonPanel();
			runButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent arg0) {
					try {
						config.setInputFile(inputFile.getFile());
						configPanel.save(config);
					} catch (final CmdLineException e) {
						JOptionPane.showMessageDialog(OpenDialog.this, e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					// Run the Orc program
					final OrcGui gui = new OrcGui(config);
					new Thread(gui).start();
					dispose();
				}
			});
			buttons.add(runButton);
			final JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent arg0) {
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
