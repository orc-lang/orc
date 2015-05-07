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
import java.io.PrintStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import orc.trace.MinimizeTracer;
import orc.trace.NullTracer;
import orc.trace.OutputStreamTracer;
import orc.trace.PrintStreamTracer;
import orc.trace.Tracer;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Class for processing configuration options. Such options could be provided
 * via command line arguments or obtained though environment variables, and
 * could also be read from other sources or hard-coded.
 * 
 * <p>This class uses annotations to map command-line arguments to calls to
 * setter methods. See {@link #processArgs(String[])}.
 * 
 * @author dkitchin, quark
 * 
 */
public class Config {

	private Boolean debug = false;
	private Boolean typecheck = false;
	private Tracer tracer = new NullTracer();
	private List<String> includes = new LinkedList<String>();
	private Integer maxPubs = null;
	private Reader instream = new InputStreamReader(System.in);
	private Integer numKilimThreads = 1;
	private Integer numSiteThreads = 2;
	private Boolean noPrelude = false;
	private String filename = "<stdin>";
	private HashMap<String, Boolean> caps = new HashMap<String, Boolean>();
	private PrintStream stdout = System.out;
	private PrintStream stderr = System.err;
	private int tokenPoolSize = -1;
	private int stackSize = -1;
	
	/**
	 * Set properties based on command-line arguments.
	 */
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
	
	@Option(name="-mintrace",usage="Specify a filename for minimal tracing. The special filename \"-\" will write a human-readable trace to stdout.")
	public void setMinimalTraceFile(File file) throws CmdLineException {
		setFullTraceFile(file);
		tracer = new MinimizeTracer(tracer);
	}
	
	@Option(name="-trace",usage="Specify a filename for full tracing. The special filename \"-\" will write a human-readable trace to stdout.")
	public void setFullTraceFile(File file) throws CmdLineException {
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
	public void setMaxPubs(int maxPubs) {
		this.maxPubs = maxPubs;
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
	
	/**
	 * Set a custom tracer.
	 */
	public void setTracer(Tracer tracer) {
		this.tracer = tracer;
	}
	
	public Boolean debugMode() {
		return debug;
	}
	
	public Boolean getNoPrelude() {
		return noPrelude;
	}
	
	public int getMaxPubs() {
		return maxPubs == null ? 0 : maxPubs;
	}
	
	public Reader getInstream() {
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
	
	/**
	 * Current capabilities include:
	 * send mail
	 * import java
	 */
	public Boolean hasCapability(String name) {
		return caps.get(name);
	}
	
	public void setCapability(String name, Boolean value) {
		caps.put(name, value);
	}
	
	public synchronized PrintStream getStdout() {
		return stdout;
	}
	
	public void setStdout(PrintStream stdout) {
		this.stdout = stdout;
	}

	public synchronized PrintStream getStderr() {
		return stderr;
	}
	
	public void setStderr(PrintStream stderr) {
		this.stderr = stderr;
	}

	public int getTokenPoolSize() {
		return tokenPoolSize;
	}
	
	public void setTokenPoolSize(int tokenPoolSize) {
		this.tokenPoolSize = tokenPoolSize;
	}

	public int getStackSize() {
		return stackSize;
	}
	
	public void setStackSize(int stackSize) {
		this.stackSize = stackSize;
	}
}