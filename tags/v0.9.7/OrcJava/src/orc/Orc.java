package orc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import orc.ast.extended.Declare;
import orc.ast.extended.declaration.Declaration;
import orc.ast.oil.Compiler;
import orc.ast.oil.Expr;
import orc.ast.oil.UnguardedRecursionChecker;
import orc.ast.oil.xml.Oil;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.parser.OrcParser;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Pub;
import orc.type.Type;

/**
 * Main class for Orc. Parses Orc file and executes it.
 * 
 * <p>Run with the argument "-help" to get a list of command-line options.
 * 
 * @author wcook, dkitchin
 */
public class Orc {

	/**
	 * 
	 * Orc toplevel main function. Command line arguments are forwarded to Config for parsing.
	 */
	public static void main(String[] args) {
		
		// Read configuration options from the environment and the command line
		Config cfg = new Config();
		cfg.processArgs(args);	
		
		Node n;
		try {
			Expr ex = compile(cfg.getInstream(), cfg);
			if (cfg.getDumpOil()) {
				System.out.println(new Oil(ex).toXML());
				// don't run or compile to DAG
				return;
			} else {
				n = Compiler.compile(ex, new Pub());
			}
		} catch (CompilationException e) {
			System.err.println(e);
			return;
		} catch (IOException e) {
			System.err.println(e);
			return;
		}
        
		// Configure the runtime engine
		OrcEngine engine = new OrcEngine(cfg);
		
		// Run the Orc program
		engine.run(n);
	}
	
	public static Expr compile(Reader source, Config cfg) throws IOException, CompilationException {

		//System.out.println("Parsing...");
		// Parse the goal expression
		OrcParser parser = new OrcParser(cfg, source, cfg.getFilename());
		orc.ast.extended.Expression e = parser.parseProgram();
		
		//System.out.println(e);
		
		//System.out.println("Importing declarations...");
		LinkedList<Declaration> decls = new LinkedList<Declaration>();
		
		if (!cfg.getNoPrelude()) {
			// Load declarations from the default include file.
			String preludename = "prelude.inc";
			OrcParser fparser = new OrcParser(cfg, cfg.openInclude(preludename), preludename);
			decls.addAll(fparser.parseModule());
		}
		
		// Load declarations from files specified by the configuration options
		for (String f : cfg.getIncludes()) {
			OrcParser fparser = new OrcParser(cfg, cfg.openInclude(f), f);
			decls.addAll(fparser.parseModule());
		}
		
		// Add the declarations to the parse tree
		Collections.reverse(decls);
		for (Declaration d : decls)
		{
			e = new Declare(d, e);
		}
		
		//System.out.println("Simplifying the abstract syntax tree...");
		// Simplify the AST
		Expr ex = e.simplify().convert(new Env<Var>(), new Env<String>());
		// System.out.println(ex);
		
		// Optionally perform typechecking
		if (cfg.typeCheckingMode()) {
			
			Type rt = ex.typesynth(new Env<Type>(), new Env<Type>());
			System.out.println("... :: " + rt);
			System.out.println("Program typechecked successfully.");
			System.out.println();
	
		}
		
		UnguardedRecursionChecker.check(ex);
		
		return ex;
	}
	
	public static Node compile(Config cfg) throws CompilationException, IOException {
		return Compiler.compile(compile(cfg.getInstream(), cfg), new Pub());
	}

	/** @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws CompilationException 
	 * @deprecated */
	public static OrcInstance runEmbedded(String source) throws CompilationException, FileNotFoundException, IOException { 
		return runEmbedded(new File(source)); 
	}
	
	/** @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws CompilationException 
	 * @deprecated */
	public static OrcInstance runEmbedded(File source) throws CompilationException, FileNotFoundException, IOException { 
		return runEmbedded(new FileReader(source));
	}
	
	/** @throws IOException 
	 * @throws CompilationException 
	 * @deprecated */
	public static OrcInstance runEmbedded(Reader source) throws CompilationException, IOException { 
		return runEmbedded(source, new Config());
	}
	
	/**
	 * Compile an Orc source program from the given input stream.
	 * Start a new Orc engine running this program in a separate thread.
	 * Returns an OrcInstance object with information about the running instance.
	 * 
	 * @deprecated
	 * @param source
	 * @param cfg
	 * @throws IOException 
	 * @throws CompilationException 
	 */
	public static OrcInstance runEmbedded(Reader source, Config cfg) throws CompilationException, IOException {
		final BlockingQueue<Object> q = new LinkedBlockingQueue<Object>();
	
		// Try to run Orc with these options
		Node result = new Node() {
			@Override
			public void process(Token t) {
				q.add(t.getResult());
				t.die();
			}
		};
		Node n = Compiler.compile(compile(source, cfg), result);
        
		// Configure the runtime engine.
		OrcEngine engine = new OrcEngine(cfg);
		engine.debugMode = cfg.debugMode();

		// Create an OrcInstance object, to be run in its own thread
		OrcInstance inst = new OrcInstance(engine, n, q);

		// Run the Orc instance in its own thread
		Thread t = new Thread(inst);
		t.start();

		// Return the instance object.
		return inst;
	}
}
