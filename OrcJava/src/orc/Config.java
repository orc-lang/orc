/**
 * Created on February 8 2007
 */
package orc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import orc.runtime.TokenPool;
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
public class Config implements Cloneable {

	private Boolean debug = false;
	private Boolean dumpOil = false;
	private Boolean typecheck = false;
	private Tracer tracer = new NullTracer();
	private LinkedList<String> includes = new LinkedList<String>();
	private String[] includePath = new String[]{"."};
	private Integer maxPubs = null;
	private String filename = "<stdin>";
	private Reader instream = new InputStreamReader(System.in);
	private Integer numKilimThreads = 1;
	private Integer numSiteThreads = 2;
	private Boolean noPrelude = false;
	private HashMap<String, Boolean> caps = new HashMap<String, Boolean>();
	private PrintStream stdout = System.out;
	private PrintStream stderr = System.err;
	private int tokenPoolSize = -1;
	private int stackSize = -1;
	private boolean hasInputFile = false;
	
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
	
	@Option(name="-trace",usage="Specify a filename for tracing. The special filename \"-\" will write a human-readable trace to stderr.")
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
		
	@Option(name="-oil",usage="Intsead of running the program, just compile it and write the OIL XML to stdout.")
	public void setDumpOil(boolean dumpOil) {
		this.dumpOil = dumpOil;
	}
	
	@Option(name="-I",usage="Set the include path for Orc includes. Separate path entries by ':'. The default value is '.', the current directory.")
	public void setIncludePath(String includePath) {
		if (null == includePath) {
			this.includePath = new String[]{};
		} else {
			this.includePath = includePath.split(System.getProperty("path.separator"));
		}
	}
	
	@Option(name="-pub",usage="Stop after publishing this many values")
	public void setMaxPubs(int maxPubs) {
		this.maxPubs = maxPubs;
	}
	
	@Argument(metaVar="file", usage="Input file. Omit or use the special filename \"-\"  to read from stdin.")
	public void setInputFile(File file) throws CmdLineException {
		if (file == null
				|| file.getPath().equals("")
				|| file.getPath().equals("-"))
		{
			instream = new InputStreamReader(System.in);
			filename = "<stdin>";
			hasInputFile = false;
			return;
		}
		try {
			instream = new FileReader(file);
			filename = file.getPath();
			hasInputFile = true;
		} catch (FileNotFoundException e) {
			throw new CmdLineException("Could not find input file '"+file+"'");
		}
	}
	
	public void addInclude(String include) {
		this.includes.add(include);
	}
	
	public boolean hasInputFile() {
		return hasInputFile;
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
	
	public Boolean getDumpOil() {
		return dumpOil;
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
	
	public boolean getTypeChecking() {
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
	
	public TokenPool getTokenPool() {
		return new TokenPool(tokenPoolSize);
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
	
	public String getIncludePath() {
		if (includePath.length > 0) {
			StringBuilder out = new StringBuilder();
			String sep = System.getProperty("path.separator");
			out.append(includePath[0]);
			for (int i = 1; i < includePath.length; ++i) {
				out.append(sep);
				out.append(includePath[i]);
			}
			return out.toString();
		} else return "";
	}
	
	/**
	 * Open an include file. Searches first the include path
	 * defined by {@link #setIncludePath(String)} and then the
	 * package orc.inc. This means you can store include files
	 * as either files or class resources (which will continue to
	 * work when Orc is deployed as a servlet or JAR).
	 * 
	 * <p>
	 * TODO: should we support include paths relative to the current file?
	 * 
	 * @param name
	 *            of the include file relative to some directory of include path
	 *            or package orc.inc
	 * @return Reader to read the file.
	 * @throws FileNotFoundException
	 *             if the resource is not found.
	 */
	public final Reader openInclude(String name) throws FileNotFoundException {
		for (String parent : includePath) {
			File file = new File(parent, name);
			if (!file.exists()) continue;
			return new FileReader(file);
		}
		InputStream stream = Config.class.getResourceAsStream("/orc/inc/"+name);
		if (stream == null) {
			throw new FileNotFoundException("Include file '" + name + "' not found; check the include path.");
		}
		return new InputStreamReader(stream);
	}
	
	@Override
	public Config clone() {
		Config out;
		try {
			out = (Config)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
		out.includes = (LinkedList<String>) includes.clone();
		out.includePath = includePath.clone();
		out.caps = (HashMap<String, Boolean>) caps.clone();
		return out;
	}
}