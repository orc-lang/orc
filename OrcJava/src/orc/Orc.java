/*
 * Created on Jun 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package orc;
import java.io.FileInputStream;
import java.io.InputStream;

import orc.ast.OrcProcess;
import orc.parser.OrcLexer;
import orc.parser.OrcParser;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;

/**
 * Main class for Orc. Parses Orc file and executes it
 * @author wcook
 */
public class Orc {

	/**
	 * Standard made program. Arguments are -debug are Orc file name (or standard input).
	 * @param args
	 */
	public static void main(String[] args) {
		OrcEngine engine = new OrcEngine();
		try {
			int i = 0;
			if (args.length > i && args[i].equals("-debug")) {
				i++;
				engine.debugMode = true;
			}
			InputStream in;
			if (args.length == i)
				in = System.in;
			else
				in = new FileInputStream(args[i]);
			OrcLexer lexer = new OrcLexer(in);
			OrcParser parser = new OrcParser(lexer);
			OrcProcess p = parser.startRule();

			engine.run(p.compile(new PrintResult()));

		} catch (Exception e) {
			System.err.println("exception: " + e);
			if (engine.debugMode)
				e.printStackTrace();
		} catch (Error e) {
			System.err.println(e.toString());
			if (engine.debugMode)
				e.printStackTrace();
		}
	}
}

/**
 * A special node that prints its output.
 * Equivalent to
 * <pre>
 *    P >x> println(x)
 * </pre>
 * @author wcook
 */
class PrintResult extends Node {
	public void process(Token t, OrcEngine engine) {
		Object val = t.getResult();
		System.out.println(val.toString());
		System.out.flush();
	}
}
