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

import orc.ast.Definition;
import orc.ast.EnvBinder;
import orc.ast.OrcProcess;
import orc.parser.OrcLexer;
import orc.parser.OrcParser;
import orc.runtime.Environment;
import orc.runtime.OrcEngine;
import orc.runtime.sites.Let;
import orc.runtime.sites.Signal;
import orc.runtime.sites.Zero;
import orc.runtime.values.Tuple;

/**
 * Main class for Orc. Parses Orc file and executes it.
 * @author wcook
 */
public class Orc {

	/**
	 * 
	 * Orc toplevel main function. Command line arguments are forwarded to Config for parsing.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config cfg = new Config();
		
		// Read configuration options from the environment and the command line
		cfg.processEnvVars();
		cfg.processArgs(args);
		
		try {			
				OrcEngine engine = new OrcEngine(cfg.maxPubs());
				Environment env = null;
				
				// Bind essential sites
				env = new Environment("let", new Let(), env);
				env = new Environment("zero", new Zero(), env);
				env = new Environment("Signal", new Signal(), env);
			
				// Load bindings from configuration options
				for (File f : cfg.getBindings())
				{
					OrcLexer lexer = new OrcLexer(new FileInputStream(f));
					OrcParser parser = new OrcParser(lexer);
					for(EnvBinder b : parser.declRule())
					{
						env = b.bind(env);
					}
				}
			
				// Add a tuple binding for the arguments forwarded from the command line
				env = new Environment("argv", new Tuple(cfg.getArgv()), env);
				
				// Parse the goal expression
				OrcLexer lexer = new OrcLexer(cfg.getInstream());
				OrcParser parser = new OrcParser(lexer);
				OrcProcess p = parser.startRule();
				
		        List<String> binders = new ArrayList<String>();
		        List<String> vals = new ArrayList<String>();
		        OrcProcess q = p.resolveNames(binders,vals);
		        
		        List<orc.ast.Definition> nodefs = new ArrayList<Definition>();
		        
		        engine.debugMode = cfg.debugMode();    
		        engine.run(q.compile(cfg.getTarget(), nodefs), env);
		
			} catch (Exception e) {
				System.err.println("exception: " + e);
				if (cfg.debugMode())
					e.printStackTrace();
			} catch (Error e) {
				System.err.println(e.toString());
				if (cfg.debugMode())
					e.printStackTrace();
			}
			System.exit(0);

	}
	
}


