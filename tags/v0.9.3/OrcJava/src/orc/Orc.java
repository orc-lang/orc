/*
 * Created on Jun 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package orc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import orc.ast.extended.Declare;
import orc.ast.extended.declaration.Declaration;
import orc.ast.oil.Expr;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.parser.OrcParser;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.result.QueueResult;

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
		cfg.processEnvVars();
		cfg.processArgs(args);	
		
		Node n;
		try {
			n = compile(cfg.getInstream(), cfg.getTarget(), cfg);
		} catch (CompilationException e) {
			System.err.println(e);
			return;
		} catch (IOException e) {
			System.err.println(e);
			return;
		}
        
		// Configure the runtime engine
		OrcEngine engine = new OrcEngine(cfg);
		
		// Run Orc with these options
		System.out.println("Running...");
		// Run the Orc program
		engine.run(n);
	}
	
	public static orc.ast.simple.Expression compile(Reader source, Config cfg) throws CompilationException, IOException {
		return compile(source, cfg, true);
	}
	
	/**
	 * Open an include file. Include files are actually stored as class
	 * resources, so that they will continue to work when Orc is deployed as a
	 * servlet or JAR, so you can't use any old filename. Specifically, the
	 * filename is always interpreted relatively to the orc.inc package, and you
	 * can't use "." or ".." in paths.
	 * 
	 * <p>
	 * TODO: support relative names correctly.
	 * 
	 * @param name
	 *            of the include file relative to orc/inc (e.g. "prelude.inc")
	 * @return Reader to read the file.
	 * @throws FileNotFoundException
	 *             if the resource is not found.
	 */
	public static Reader openInclude(String name) throws FileNotFoundException {
		InputStream stream = Orc.class.getResourceAsStream("/orc/inc/"+name);
		if (stream == null) {
			throw new FileNotFoundException(
					"Include file '"
							+ name
							+ "' not found; did you remember to put it in the orc.inc package?");
		}
		return new InputStreamReader(stream);
	}
	
	public static orc.ast.simple.Expression compile(Reader source, Config cfg, boolean includeStdlib) throws IOException, CompilationException {

		//System.out.println("Parsing...");
		// Parse the goal expression
		OrcParser parser = new OrcParser(source);
		orc.ast.extended.Expression e = parser.parseProgram();
		
		//System.out.println(e);
		
		//System.out.println("Importing declarations...");
		LinkedList<Declaration> decls = new LinkedList<Declaration>();
		
		if (includeStdlib) {
			// Load declarations from the default include file.
			OrcParser fparser = new OrcParser(openInclude("prelude.inc"), "prelude.inc");
			decls.addAll(fparser.parseModule());
		}
		
		// Load declarations from files specified by the configuration options
		for (String f : cfg.getIncludes())
		{
			OrcParser fparser = new OrcParser(openInclude(f), f);
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
		return e.simplify();
	}
	
	protected static Node compile(Reader source, Node target, Config cfg) throws CompilationException, IOException {
		orc.ast.simple.Expression es = compile(source, cfg);
		
		//System.out.println("Compiling to an execution graph...");
		// Compile the AST, directing the output towards the configured target
		Expr ex = es.convert(new Env<Var>());
		Node en = ex.compile(target);
		return en;
	}

	/** @deprecated */
	public static OrcInstance runEmbedded(String source) { 
		return runEmbedded(new File(source)); 
	}
	
	/** @deprecated */
	public static OrcInstance runEmbedded(File source) { 
		try {
			return runEmbedded(new FileReader(source));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/** @deprecated */
	public static OrcInstance runEmbedded(Reader source) { 
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
	 */
	public static OrcInstance runEmbedded(Reader source, Config cfg) {
		
		BlockingQueue<Object> q = new LinkedBlockingQueue<Object>();
	
		// Try to run Orc with these options
		try {	
				Node n = compile(source, new QueueResult(q), cfg);
		        
				// Configure the runtime engine.
				OrcEngine engine = new OrcEngine(cfg);
		        engine.debugMode = cfg.debugMode();
		        
		        // Create an OrcInstance object, to be run in its own thread
		        Env env = new Env();
		        OrcInstance inst = new OrcInstance(engine, n, env, q);
		        
		        // Run the Orc instance in its own thread
		        Thread t = new Thread(inst);
		        t.start();
		        
		        // Return the instance object.
		        return inst;
			} catch (Exception e) {
				System.err.println("exception: " + e);
				if (cfg.debugMode())
					e.printStackTrace();
			} catch (Error e) {
				System.err.println(e.toString());
				if (cfg.debugMode())
					e.printStackTrace();
			}
			
		return null;
	}
	
	private static String tmpdir = System.getProperty("java.io.tmpdir");
	/**
	 * Create a new temporary directory and return the path to that directory.
	 * The directory will NOT be deleted automatically; you can use
	 * deleteDirectory to do that when you are done with it.
	 */
	public static File createTmpdir(String prefix) throws IOException {
        File out = new File(tmpdir, "orc-" + prefix + "-"
        		+ new Integer(Thread.currentThread().hashCode()).toString());
        if (!out.mkdir()) {
            throw new IOException("Unable to create temporary directory " + out.getPath());
        }
        return out;
	}
	
	/** Delete a directory recursively */
	public static boolean deleteDirectory(File directory) {
		boolean out = true;
		File[] fileArray = directory.listFiles();
		if (fileArray != null) {
			for (File f : fileArray) {
				out = deleteDirectory(f) && out;
			}
	    }
		return directory.delete() && out;
	}
}