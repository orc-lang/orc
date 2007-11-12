/*
 * Created on Jun 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package orc;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

//import org.exist.storage.BrokerPool;
//import org.exist.storage.DBBroker;
//import org.exist.storage.NativeBroker;
//import org.exist.util.Configuration;

import orc.ast.extended.*;
import orc.ast.simple.arg.FreeVar;
import orc.ast.simple.arg.Var;
import orc.orcx.OrcX;
import orc.parser.OrcLexer;
import orc.parser.OrcParser;
import orc.runtime.Environment;
import orc.runtime.OrcEngine;
import orc.runtime.values.Constant;
import orc.runtime.values.Tuple;
import orc.runtime.values.Value;

/**
 * Main class for Orc. Parses Orc file and executes it.
 * @author wcook, dkitchin
 */
public class Orc {

	/**
	 * 
	 * Orc toplevel main function. Command line arguments are forwarded to Config for parsing.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Read configuration options from the environment and the command line
		Config cfg = new Config();
		cfg.processEnvVars();
		cfg.processArgs(args);	
		
		// Try to run Orc with these options
		try {	
				System.out.println("Configuring embedded languages...");
				
//				TODO: Replace null with a database broker for the persistent document set
				OrcX orcx = new OrcX();
			
				System.out.println("Parsing...");
				// Parse the goal expression
				OrcLexer lexer = new OrcLexer(cfg.getInstream());
				OrcParser parser = new OrcParser(lexer, orcx);
				//OrcParser parser = new OrcParser(lexer);
				orc.ast.extended.Expression e = parser.startRule();
				
				System.out.println("Importing declarations...");
				// Load declarations from files specified by the configuration options
				LinkedList<Declaration> decls = new LinkedList<Declaration>();
				for (File f : cfg.getBindings())
				{
					OrcLexer flexer = new OrcLexer(new FileInputStream(f));
					OrcParser fparser = new OrcParser(flexer);
					decls.addAll(fparser.decls());
				}
				
				// Add the declarations to the parse tree
				Collections.reverse(decls);
				for (Declaration d : decls)
				{
					e = new Declare(d, e);
				}
				
				System.out.println("Simplifying the abstract syntax tree...");
				// Simplify the AST
				orc.ast.simple.Expression es = e.simplify();
				
				
				// Bind a variable for argv, the tuple of command line parameters
				Var argvar = new Var();
				es.subst(argvar, new FreeVar("argv"));
				
				System.out.println("Compiling to an execution graph...");
				// Compile the AST, directing the output towards the configured target
				orc.runtime.nodes.Node target = cfg.getTarget();
				orc.runtime.nodes.Node n = es.compile(target);
		        
				
				// Create the initial environment
				Environment env = null;
				List<Value> argv = new LinkedList<Value>();
				for (Object o : cfg.getArgv())
				{ 
					argv.add(new Constant(o));
				}
				env = new Environment(argvar, new Tuple(argv), env);
		        
				// Configure the runtime engine
				OrcEngine engine = new OrcEngine(cfg.maxPubs());
		        engine.debugMode = cfg.debugMode();
		        
		        
		        System.out.println("Running...");
		        // Run the Orc program
		        engine.run(n, env);
		        
		
			} catch (Exception e) {
				System.err.println("exception: " + e);
				if (cfg.debugMode())
					e.printStackTrace();
			} catch (Error e) {
				System.err.println(e.toString());
				if (cfg.debugMode())
					e.printStackTrace();
			}
			OrcX.terminate();
			System.exit(0);
	}
	
}


