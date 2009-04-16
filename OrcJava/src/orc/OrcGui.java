package orc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import orc.ast.oil.Compiler;
import orc.ast.oil.Expr;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.runtime.TokenException;
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
 * First attempt at a GUI interface for Orc.
 * This outputs to its own window instead of the console.
 * @author quark
 */
public class OrcGui implements Runnable {
	protected Config config;
	protected OrcEngine engine;
	
	private Action pause = new AbstractAction("Pause") {
		{
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
		}
		public void actionPerformed(ActionEvent e) {
			engine.pause();
			setEnabled(false);
			resume.setEnabled(true);
		}
    };
	private Action resume = new AbstractAction("Resume") {
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
	private Action stop = new AbstractAction("Stop") {
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
		// Read configuration options from the environment and the command line
		final Config cfg = new Config();
		cfg.processArgs(args);	
		OrcGui gui = new OrcGui(cfg);
		gui.run();
	}
	
	protected void error(String title, String message) {
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
        /*
        JMenu menu = new JMenu("Run");
        menu.setMnemonic(KeyEvent.VK_R);
        menu.add(new JMenuItem(pause));
        menu.add(new JMenuItem(resume));
        menu.add(new JMenuItem(stop));
        bar.add(menu);
        */
        return bar;
	}
	
	public void run() {
		Node n;
		try {
			Expr ex = Orc.compile(config.getInstream(), config);
			n = Compiler.compile(ex, new Pub());
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
}
