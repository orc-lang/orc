package orc;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
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
public class OrcGUI {
	private OrcGUI() {}
	
	public static void main(String[] args) {
		// Read configuration options from the environment and the command line
		final Config cfg = new Config();
		cfg.processArgs(args);	
		run(cfg, JFrame.EXIT_ON_CLOSE);
	}
	
	static void error(String title, String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	private static class OutputHandler {
		private StyledDocument doc;
		private JScrollBar scrollBar;
		public OutputHandler(StyledDocument doc, JScrollBar scrollBar) {
			this.doc = doc;
			this.scrollBar = scrollBar;
		}
		public void output(final String style, final String message) {
			invokeLater(new Runnable() {
				public void run() {
					try {
						doc.insertString(doc.getLength(), message, doc.getStyle(style));
					} catch (BadLocationException e) {
						throw new AssertionError(e);
					}
					scrollBar.setValue(scrollBar.getMaximum());
				}
			});
		}
	}
	
	static void run(Config cfg, int onclose) {
		// initialize document
		JTextPane pane = new JTextPane();
		pane.setEditable(false);
		StyledDocument doc = pane.getStyledDocument();
		
		//Initialize some styles.
		Style plain = doc.addStyle("plain",
			StyleContext.getDefaultStyleContext()
				.getStyle(StyleContext.DEFAULT_STYLE));
		Style s;
		s = doc.addStyle("print", plain);
		StyleConstants.setFontFamily(s, "SansSerif");
		
		s = doc.addStyle("publish", plain);
		StyleConstants.setFontFamily(plain, "Monospaced");
		StyleConstants.setBold(s, true);
		
		s = doc.addStyle("error", plain);
		StyleConstants.setFontFamily(plain, "Monospaced");
		StyleConstants.setForeground(s, Color.RED);
		StyleConstants.setBold(s, true);
		
		// initialize frame
		final JFrame frame = new JFrame(cfg.getFilename());
		frame.setPreferredSize(new Dimension(640, 480));
		frame.setDefaultCloseOperation(onclose);
        JScrollPane scrollPane = new JScrollPane(pane);
		frame.add(scrollPane);
		invokeLater(new Runnable() {
			public void run() {
				frame.pack();
				frame.setVisible(true);
			}
		});
	
		final OutputHandler output = new OutputHandler(doc, scrollPane.getVerticalScrollBar());
		
		// Configure the runtime engine
		OrcEngine engine = new OrcEngine(cfg) {
			@Override
			public void print(String string, boolean newline) {
				output.output("print", string + (newline?"\n":""));
			}

			@Override
			public void publish(Object v) {
				output.output("publish", Value.write(v)+"\n");
			}

			@Override
			public void tokenError(TokenException problem) {
				output.output("error", "Error: " + problem.getMessage() + "\n");
				output.output("error", "Backtrace:\n");
				SourceLocation[] backtrace = problem.getBacktrace();
				for (SourceLocation location : backtrace) {
					output.output("error", location+"\n");
				}
				output.output("error", "\n");
			}
			
		};
		Node n;
		try {
			Expr ex = Orc.compile(cfg.getInstream(), cfg);
			n = Compiler.compile(ex, new Pub());
		} catch (CompilationException e) {
			error("Compilation Error", e.getMessage());
			return;
		} catch (IOException e) {
			error("Error Reading File", e.getMessage());
			return;
		}
		
		// Run the Orc program
		engine.run(n);
	}
}
