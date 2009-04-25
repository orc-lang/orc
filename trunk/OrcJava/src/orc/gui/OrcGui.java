package orc.gui;

import static javax.swing.SwingUtilities.invokeLater;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

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
import orc.Orc;
import orc.ast.oil.Compiler;
import orc.ast.oil.Expr;
import orc.ast.oil.SiteResolver;
import orc.ast.oil.xml.Oil;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.runtime.TokenException;
import orc.progress.ProgressMonitorListener;
import orc.progress.SubProgressListener;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Pub;
import orc.runtime.values.Value;

import org.kohsuke.args4j.CmdLineException;

/**
 * A basic GUI interface for Orc.
 * This outputs to its own window instead of the console.
 * It also provides pause/resume/stop buttons in the menubar.
 * You still need to start the GUI from the command line.
 * @author quark
 */
public class OrcGui implements Runnable {
	protected Config config;
	protected OrcEngine engine;
	
	protected Action pause = new AbstractAction("Pause") {
		{
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
		}
		public void actionPerformed(ActionEvent e) {
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
		public void actionPerformed(ActionEvent e) {
			engine.unpause();
			setEnabled(false);
			pause.setEnabled(true);
		}
    };
	protected Action stop = new AbstractAction("Stop") {
    	{
			putValue(MNEMONIC_KEY, KeyEvent.VK_S);
    	}
		public void actionPerformed(ActionEvent e) {
			engine.terminate();
			setEnabled(false);
			pause.setEnabled(false);
			resume.setEnabled(false);
		}
    };
	private StyledDocument document;
	private JScrollBar scrollBar;
	public OrcGui(Config config) {
		this.config = config;
	}
	
	public static void main(String[] args) {
		Config config = new Config();
		if (args.length > 0) {
			config.processArgs(args);
			new Thread(new OrcGui(config)).start();
		} else {
			invokeLater(new OpenDialog(config));
		}
	}
	
	protected static void error(String title, String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	protected JFrame createFrame() {
		JFrame frame = new JFrame(config.getFilename());
		frame.setPreferredSize(new Dimension(640, 480));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		return frame;
	}
	
	protected JScrollPane createScrollPane() {
        return new JScrollPane();
	}
	
	protected JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.add(new JButton(pause));
        bar.add(new JButton(resume));
        bar.add(new JButton(stop));
        return bar;
	}
	
	public void run() {
		Node n;
		ProgressMonitorListener progress = new ProgressMonitorListener(
				null, "Compiling " + config.getFilename(), "");
		try {
			Expr ex;
			if (config.hasOilInputFile()) {
				progress.setNote("Loading XML");
				Oil oil = Oil.fromXML(config.getOilReader());
				progress.setProgress(0.2);
				if (progress.isCanceled()) return;
				progress.setNote("Converting to AST");
				ex = oil.unmarshal();
				progress.setProgress(0.5);
				if (progress.isCanceled()) return;
				progress.setNote("Loading sites");
				ex = SiteResolver.resolve(ex, config);
			} else {
				ex = Orc.compile(config.getReader(), config,
					new SubProgressListener(progress, 0, 0.7));
			}
			if (config.hasOilOutputFile()) {
				Writer out = config.getOilWriter();
				progress.setNote("Writing OIL");
				new Oil(ex).toXML(out);
				out.close();
			}
			progress.setProgress(0.8);
			if (progress.isCanceled()) return;
			progress.setNote("Creating DAG");
			n = Compiler.compile(ex, new Pub());
			progress.setProgress(0.95);
			if (progress.isCanceled()) return;
		} catch (CompilationException e) {
			progress.setProgress(1.0);
			error("Compilation Error", e.getMessage());
			return;
		} catch (IOException e) {
			progress.setProgress(1.0);
			error("IO Error", e.getMessage());
			return;
		}
		
		// initialize document
		JTextPane pane = new JTextPane();
		pane.setEditable(false);
		document = pane.getStyledDocument();
		
		// initialize document styles
		Style plain = document.addStyle("plain",
			StyleContext.getDefaultStyleContext()
				.getStyle(StyleContext.DEFAULT_STYLE));
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
        JScrollPane scrollPane = createScrollPane();
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
							document.insertString(document.getLength(), message,
									document.getStyle(style));
						} catch (BadLocationException e) {
							throw new AssertionError(e);
						}
						scrollBar.setValue(scrollBar.getMaximum());
					}
				});
			}
			
			@Override
			public void print(String string, boolean newline) {
				output("print", string + (newline?"\n":""));
			}
	
			@Override
			public void onPublish(Object v) {
				output("publish", Value.write(v)+"\n");
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
			public void onError(TokenException problem) {
				output("error", "Error: " + problem.getMessage() + "\n");
				output("error", "Backtrace:\n");
				SourceLocation[] backtrace = problem.getBacktrace();
				for (SourceLocation location : backtrace) {
					output("error", location+"\n");
				}
				output("error", "\n");
			}
		};
		
		progress.setProgress(1);
		if (progress.isCanceled()) return;
		
		// display the frame
		invokeLater(new Runnable() {
			public void run() {
				frame.pack();
				frame.setVisible(true);
			}
		});
		
		// Run the Orc program
		engine.run(n);
	}
	
	/**
	 * Opens automatically when the program launches.
	 * Prompts the user to choose a file as well as
	 * specify configuration options.
	 * @author quark
	 */
	protected static final class OpenDialog extends JDialog implements Runnable {
		private FileField inputFile = new FileField("Open Orc Script", true, new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory()
				|| f.getName().endsWith(".orc")
				|| f.getName().endsWith(".oil");
			}

			@Override
			public String getDescription() {
				return "Orc Scripts";
			}
			
		});
		private JButton runButton = new JButton("Run");
		{
			runButton.setEnabled(false);
			inputFile.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					runButton.setEnabled(e.getActionCommand().length() > 0);
				}
			});
		}
		public OpenDialog(final Config config) {
			JPanel content = new OneColumnPanel();
			content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setContentPane(content);
			setResizable(false);
			
			JLabel inputFileLabel = new JLabel("Read Orc script from...");
			content.add(inputFileLabel);
			content.add(inputFile);
			
			final ConfigPanel configPanel = new ConfigPanel();
			configPanel.load(config);
			content.add(configPanel);
			
			ButtonPanel buttons = new ButtonPanel();
			runButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						config.setInputFile(inputFile.getFile());
					} catch (CmdLineException e) {
						JOptionPane.showMessageDialog(OpenDialog.this,
								"The file " + inputFile.getFile() + " could not be opened.",
								"Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					configPanel.save(config);
					// Run the Orc program
					OrcGui gui = new OrcGui(config);
					new Thread(gui).start();
					dispose();
				}
			});
			buttons.add(runButton);
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
