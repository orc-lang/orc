package orc.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FileChooserUI;
import javax.swing.text.AbstractWriter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import orc.Config;
import orc.Orc;
import orc.ast.oil.Compiler;
import orc.ast.oil.Expr;
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

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import static javax.swing.SwingUtilities.invokeLater;

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
			Expr ex = Orc.compile(config.getInstream(), config,
					new SubProgressListener(progress, 0, 0.8));
			if (progress.isCanceled()) return;
			progress.setNote("Creating DAG");
			n = Compiler.compile(ex, new Pub());
			progress.setProgress(0.95);
			if (progress.isCanceled()) return;
		} catch (CompilationException e) {
			error("Compilation Error", e.getMessage());
			return;
		} catch (IOException e) {
			error("Error Reading File", e.getMessage());
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
		private JTextField fileName = new JTextField();
		private JButton runButton = new JButton("Run");
		{
			fileName.setEditable(false);
			runButton.setEnabled(false);
		}
		public OpenDialog(final Config config) {
			JPanel content = new OneColumnPanel();
			content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setContentPane(content);
			setResizable(false);
			
			JPanel file = new JPanel(new BorderLayout(5, 0));
			file.add(fileName, BorderLayout.CENTER);
			fileName.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					chooseFile();
				}
			});
			JButton choose = new JButton("...");
			choose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					chooseFile();
				}
			});
			file.add(choose, BorderLayout.EAST);
			JLabel fileLabel = new JLabel("Orc script");
			content.add(fileLabel);
			content.add(file);
			content.add(Box.createVerticalStrut(10));
			
			final ConfigPanel configPanel = new ConfigPanel();
			configPanel.load(config);
			content.add(configPanel);
			
			ButtonPanel buttons = new ButtonPanel();
			runButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						config.setInputFile(new File(fileName.getText()));
					} catch (CmdLineException e) {
						JOptionPane.showMessageDialog(OpenDialog.this,
								"The file " + fileName.getText() + " could not be opened.",
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
		
		private void chooseFile() {
			File wd;
			if (fileName.getText().equals("")) {
				wd = new File(System.getProperty("user.dir"));
			} else {
				wd = new File(fileName.getText()).getParentFile();
			}
			JFileChooser chooser = new JFileChooser(wd);
			chooser.setDialogTitle("Choose an Orc Script");
			chooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory()
					|| f.getName().endsWith(".orc");
				}

				@Override
				public String getDescription() {
					return "Orc Scripts";
				}
				
			});
			int status = chooser.showOpenDialog(OpenDialog.this);
			if (status == JFileChooser.APPROVE_OPTION) {
				fileName.setText(chooser.getSelectedFile().getPath());
			}
			runButton.setEnabled(fileName.getText().length() > 0);
		}
		
		public void run() {
			pack();
			setVisible(true);
		}
	}
}
