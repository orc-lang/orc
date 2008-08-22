/**
 * Created on February 8 2007
 */
package orc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import orc.trace.NullTracer;
import orc.trace.OutputStreamTracer;
import orc.trace.PrintStreamTracer;
import orc.trace.Tracer;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Class for processing configuration options. Such options could be provided via command line 
 * arguments or obtained though environment variables, and could also be read from other sources. 
 * 
 * Note that this class does not set up the runtime environment (for example, by instantiating
 * site bindings or defining closures); it only collects the files that will provide those
 * bindings.
 * 
 * @author dkitchin, quark
 *
 */
public class Config {

	private Boolean debug = false;
	private Boolean typecheck = false;
	private Tracer tracer = new NullTracer();
	private List<String> includes = new LinkedList<String>();
	private Integer maxpub = null;
	private Reader instream = new InputStreamReader(System.in);
	private Integer numKilimThreads = 1;
	private Integer numSiteThreads = 2;
	private Boolean noPrelude = false;
	private String filename = "<stdin>";
	
	public void processArgs(String[] args) {
		CmdLineParser parser = new CmdLineParser(this); 
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e1) {
			System.err.println(e1.getMessage());
			System.err.println("Usage: java -jar orc.jar [options] [file]");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}
	
	@Option(name="-help",usage="Show command-line argument usage")
	public void printUsage(boolean _) throws CmdLineException{
		throw new CmdLineException("");
	}
	
	@Option(name="-debug",usage="Enable debugging output")
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	@Option(name="-typecheck",usage="Enable typechecking")
	public void setTypeChecking(boolean typecheck) {
		this.typecheck = typecheck;
	}
		
	@Option(name="-noprelude",usage="Do not implicitly include standard library (prelude).")
	public void setNoPrelude(boolean noPrelude) {
		this.noPrelude = noPrelude;
	}
	
	@Option(name="-trace",usage="Specify a filename for tracing. The special filename \"-\" will write a human-readable trace to stdout.")
	public void setTraceFile(File file) throws CmdLineException {
		if (file.getPath().equals("-")) {
			tracer = new PrintStreamTracer(System.err);
		} else {
			try {
				tracer = new OutputStreamTracer(new FileOutputStream(file));
			} catch (FileNotFoundException e) {
				throw new CmdLineException("Could not find trace file '"+file+"'");
			} catch (IOException e) {
				throw new CmdLineException("Error opening trace file '"+file+"'");
			}
		}
	}
	
	@Option(name="-i",usage="Include this file from the package orc.inc;" +
			" may appear multiple times.")
	public void addInclude(String include) {
		this.includes.add(include);
	}
	
	@Option(name="-pub",usage="Stop after publishing this many values")
	public void setMaxpub(int maxpub) {
		this.maxpub = maxpub;
	}
	
	@Argument(metaVar="file", usage="Input file. Omit to use STDIN.")
	public void setInputFile(File file) throws CmdLineException {
		try {
			instream = new FileReader(file);
			filename = file.getPath();
		} catch (FileNotFoundException e) {
			throw new CmdLineException("Could not find input file '"+file+"'");
		}
	}
	
	public void processEnvVars() 
	{ 
		// TODO: implement environment variable processing of configuration options
	}
	
	public Boolean debugMode()
	{
		return debug;
	}
	
	public Boolean getNoPrelude() {
		return noPrelude;
	}
	
	public Integer maxPubs()
	{
		return maxpub;
	}
	
	public Reader getInstream()
	{
		return instream;
	}
	
	public Tracer getTracer() {
		return tracer;
	}
	
	public List<String> getIncludes()
	{
		return includes;
	}
	
	public Integer getNumKilimThreads() {
		return numKilimThreads;
	}
	
	public Integer getNumSiteThreads() {
		return numSiteThreads;
	}
	
	public boolean typeCheckingMode() {
		return typecheck;
	}
	
	
	public String getFilename() {
		return filename;
	}
}