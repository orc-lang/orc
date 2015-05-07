//
// Config.java -- Java class Config
// Project OrcJava
//
// $Id$
//
// Created on February 8 2007
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import orc.error.compiletime.CompileMessageRecorder;
import orc.progress.NullProgressListener;
import orc.progress.ProgressListener;
import orc.runtime.TokenPool;
import orc.trace.NullTracer;
import orc.trace.OutputStreamTracer;
import orc.trace.Tracer;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StopOptionHandler;

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
	private int debugLevel = 0;
	private CompileMessageRecorder messageRecorder = new StdErrCompileMsgRecorder(this);
	private ProgressListener progressListener = NullProgressListener.singleton;
	private File oilOutputFile = new File("");
	private File traceOutputFile = new File("");
	private boolean hasOilInputFile = false;
	private Boolean typecheck = false;
	private Tracer tracer = new NullTracer();
	private LinkedList<String> includes = new LinkedList<String>();
	private String[] includePath = new String[] { "." };
	private Integer maxPubs = null;
	private String filename = "script";
	private InputStream instream;
	private int numKilimThreads = 1;
	private int numSiteThreads = 2;
	private Boolean noPrelude = false;
	private HashMap<String, Boolean> caps = new HashMap<String, Boolean>();
	private PrintStream stdout = System.out;
	private PrintStream stderr = System.err;
	private int tokenPoolSize = -1;
	private int stackSize = -1;
	private boolean hasInputFile = false;
	private String classPath = "";
	private ClassLoader classLoader = Config.class.getClassLoader();
	private boolean shortErrors = false;
	private boolean quietChecking = false;
	private boolean exceptionsOn = false;
	private boolean isolatedOn = false;
	private boolean atomicOn = false;
	

	/**
	 * Set properties based on command-line arguments.
	 */
	public void processArgs(final String[] args) {
		final CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			if (instream == null) {
				throw new CmdLineException("You must supply a filename to execute.");
			}
		} catch (final CmdLineException e1) {
			System.err.println(e1.getMessage());
			System.err.println("Usage: java -jar orc.jar [options] [file]");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}
	
	public String composeCmdLine() {
		StringBuffer cmdLine = new StringBuffer();
		//TODO: If possible, re-write to use arg4j annotations
		if (getTypeChecking()) {
			cmdLine.append("-typecheck ");
		}
		if (getNoPrelude()) {
			cmdLine.append("-noprelude ");
		}
		if (!".".equals(getIncludePath())) {
			cmdLine.append("-I \"");
			cmdLine.append(getIncludePath());
			cmdLine.append("\" ");
		}
		if (!"".equals(getClassPath())) {
			cmdLine.append("-cp \"");
			cmdLine.append(getClassPath());
			cmdLine.append("\" ");
		}
		if (hasOilOutputFile()) {
			cmdLine.append("-oilOut \"");
			cmdLine.append(getOilOutputFile().getPath());
			cmdLine.append("\" ");
		}
		if (getMaxPubs() != 0) {
			cmdLine.append("-pub ");
			cmdLine.append(getMaxPubs());
			cmdLine.append(" ");
		}
		if (getNumSiteThreads() != 2) {
			cmdLine.append("-numSiteThreads ");
			cmdLine.append(getNumSiteThreads());
			cmdLine.append(" ");
		}
		if (hasTraceOutputFile()) {
			cmdLine.append("-trace \"");
			cmdLine.append(getTraceOutputFile());
			cmdLine.append("\" ");
		}
		for (int i = 0; i < getDebugLevel(); i++) {
			cmdLine.append("-debug ");
		}
		if (this.hasInputFile()) {
			cmdLine.append("-- \"");
			cmdLine.append(getInputFilename());
			cmdLine.append("\"");
		}
		if (this.getExceptionsOn()){
			cmdLine.append("-exceptions ");
		}
		if (this.getAtomicOn()){
			cmdLine.append("-allowAtomic ");
		}
		if (this.getIsolatedOn()){
			cmdLine.append("-allowIsolated ");
		}
		return cmdLine.toString();
	}

	@Option(name = "-help", usage = "Show command-line argument usage")
	public void printUsage(final boolean _) throws CmdLineException {
		throw new CmdLineException("");
	}

	@Option(name = "-debug", usage = "Enable debugging output, which is disabled by default. Repeat this argument to increase verbosity.")
	public void setDebug(final boolean debug) {
		if (debug) {
			++debugLevel;
		} else {
			--debugLevel;
		}
	}

	public void setDebugLevel(final int debugLevel) {
		this.debugLevel = debugLevel;
	}
	
	@Option(name = "-typecheck", usage = "Enable typechecking, which is disabled by default.")
	public void setTypeChecking(final boolean typecheck) {
		this.typecheck = typecheck;
	}

	@Option(name = "-noprelude", usage = "Do not implicitly include standard library (prelude), which is included by default.")
	public void setNoPrelude(final boolean noPrelude) {
		this.noPrelude = noPrelude;
	}

	@Option(name = "-trace", usage = "Specify a filename for tracing. Default is not to trace.")
	public void setTraceOutputFile(final File file) throws CmdLineException {
		if (file.getPath().equals("")) {
			tracer = new NullTracer();
			traceOutputFile = file;
		} else {
			try {
				tracer = new OutputStreamTracer(new FileOutputStream(file));
				traceOutputFile = file;
			} catch (final FileNotFoundException e) {
				throw new CmdLineException("Could not find trace file '" + file + "'");
			} catch (final IOException e) {
				throw new CmdLineException("Error opening trace file '" + file + "'");
			}
		}
	}

	@Option(name = "-oilOut", usage = "Write the compiled OIL to the given filename.")
	public void setOilOutputFile(final File oilOut) {
		this.oilOutputFile = oilOut;
	}

	@Option(name = "-I", usage = "Set the include path for Orc includes (same syntax as CLASSPATH). Default is \".\", the current directory. Prelude files are always available for include regardless of this setting.")
	public void setIncludePath(String includePath) {
		includePath = includePath.trim();
		if (includePath.length() == 0) {
			this.includePath = new String[] {};
		} else {
			this.includePath = includePath.split(System.getProperty("path.separator"));
		}
	}

	@Option(name = "-cp", usage = "Set the class path for Orc sites (same syntax as CLASSPATH). This is only used for classes not found in the Java VM classpath.")
	public void setClassPath(String classPath) {
		classPath = classPath.trim();
		if (classPath.length() == 0) {
			this.classPath = "";
			classLoader = Config.class.getClassLoader();
			return;
		}
		final String[] classPathFiles = classPath.split(System.getProperty("path.separator"));
		final URL[] urls = new URL[classPathFiles.length];
		for (int i = 0; i < classPathFiles.length; ++i) {
			try {
				urls[i] = new URI("file", new File(classPathFiles[i]).getAbsolutePath(), null).toURL();
			} catch (final URISyntaxException e) {
				// impossible
				throw new AssertionError(e);
			} catch (final MalformedURLException e) {
				// impossible
				throw new AssertionError(e);
			}
		}
		this.classPath = classPath;
		classLoader = new URLClassLoader(urls, Config.class.getClassLoader());
	}

	@Option(name = "-pub", usage = "Terminate the program after this many values are published. Default=infinity.")
	public void setMaxPubs(final int maxPubs) {
		this.maxPubs = maxPubs;
	}

	@Option(name = "-numSiteThreads", usage = "Use up to this many threads for blocking site calls. Default=2.")
	public void setNumSiteThreads(final Integer v) {
		numSiteThreads = v;
	}

	@Option(name = "--", handler = StopOptionHandler.class, usage = "Use \"--\" to signal the end of options, e.g. so you can specify a filename starting with \"-\".")
	@Argument(metaVar = "file", usage = "Path to script to execute.")
	public void setInputFile(final File file) throws CmdLineException {
		try {
			instream = new FileInputStream(file);
			filename = file.getPath();
			hasOilInputFile = filename.endsWith(".oil");
			hasInputFile = true;
		} catch (final FileNotFoundException e) {
			throw new CmdLineException("Could not find input file '" + file + "'");
		}
	}
	
	@Option(name = "-exceptions", usage = "Enable exceptions (experimental), which is disabled by default.")
	public void setExceptionsOn(final boolean exceptionsOn) {
		this.exceptionsOn = exceptionsOn;
	}

	@Option(name = "-allowAtomic", usage = "Enable atomic expressions (experimental), which are disabled by default.")
	public void setAtomicOn(final boolean atomicOn) {
		this.atomicOn = atomicOn;
	}
	
	@Option(name = "-allowIsolated", usage = "Enable isolated expressions (experimental), which are disabled by default.")
	public void setIsolatedOn(final boolean isolatedOn) {
		this.isolatedOn = isolatedOn;
	}
	
	public void addInclude(final String include) {
		this.includes.add(include);
	}

	public boolean hasInputFile() {
		return hasInputFile;
	}

	/**
	 * The default is long errors.
	 * Short errors are currently used only for regression testing. 
	 */
	public void setShortErrors(final boolean b) {
		shortErrors = b;
	}
	
	public boolean getShortErrors() {
		return shortErrors;
	}
	
	/**
	 * If the typechecker runs, suppress all its output.
	 * Quiet checking is currently used only for regression testing.
	 * 
	 * @param b
	 */
	public void setQuietChecking(boolean b) {
		quietChecking = b;
	}

	public boolean getQuietChecking() {
		return quietChecking;
	}

	/**
	 * Set a custom tracer.
	 */
	public void setTracer(final Tracer tracer) {
		this.tracer = tracer;
	}

	public int getDebugLevel() {
		return debugLevel;
	}

	public CompileMessageRecorder getMessageRecorder() {
		return messageRecorder;
	}

	public void setMessageRecorder(final CompileMessageRecorder messageRecorder) {
		this.messageRecorder = messageRecorder;
	}

	public ProgressListener getProgressListener() {
		return progressListener;
	}

	public void setProgressListener(final ProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	public File getTraceOutputFile() {
		return traceOutputFile;
	}

	public boolean hasTraceOutputFile() {
		return !traceOutputFile.getPath().equals("");
	}

	public File getOilOutputFile() {
		return oilOutputFile;
	}

	public boolean hasOilOutputFile() {
		return !oilOutputFile.getPath().equals("");
	}

	public boolean hasOilInputFile() {
		return hasOilInputFile;
	}

	public Reader getOilReader() throws IOException {
		assert hasOilInputFile();
		return new InputStreamReader(new GZIPInputStream(instream));
	}

	public Writer getOilWriter() throws IOException {
		assert hasOilOutputFile();
		return new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(oilOutputFile)));
	}

	public Boolean getNoPrelude() {
		return noPrelude;
	}

	public int getMaxPubs() {
		return maxPubs == null ? 0 : maxPubs;
	}

	public Reader getReader() {
		return new InputStreamReader(instream);
	}

	public InputStream getInputStream() {
		return instream;
	}

	public Tracer getTracer() {
		return tracer;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public int getNumKilimThreads() {
		return numKilimThreads;
	}

	public int getNumSiteThreads() {
		return numSiteThreads;
	}

	public boolean getTypeChecking() {
		return typecheck;
	}

	public String getInputFilename() {
		return filename;
	}

	/**
	 * Current capabilities include:
	 * send mail
	 * import java
	 */
	public Boolean hasCapability(final String name) {
		return caps.get(name);
	}

	public void setCapability(final String name, final Boolean value) {
		caps.put(name, value);
	}

	public synchronized PrintStream getStdout() {
		return stdout;
	}

	public void setStdout(final PrintStream stdout) {
		this.stdout = stdout;
	}

	public synchronized PrintStream getStderr() {
		return stderr;
	}

	public void setStderr(final PrintStream stderr) {
		this.stderr = stderr;
	}

	public TokenPool getTokenPool() {
		return new TokenPool(tokenPoolSize);
	}

	public void setTokenPoolSize(final int tokenPoolSize) {
		this.tokenPoolSize = tokenPoolSize;
	}

	public int getStackSize() {
		return stackSize;
	}

	public void setStackSize(final int stackSize) {
		this.stackSize = stackSize;
	}

	public String getIncludePath() {
		if (includePath.length > 0) {
			final StringBuilder out = new StringBuilder();
			final String sep = System.getProperty("path.separator");
			out.append(includePath[0]);
			for (int i = 1; i < includePath.length; ++i) {
				out.append(sep);
				out.append(includePath[i]);
			}
			return out.toString();
		} else {
			return "";
		}
	}

	public String getClassPath() {
		return classPath;
	}
	
	public boolean getExceptionsOn() {
		return exceptionsOn;
	}
	
	public boolean getAtomicOn() {
		return atomicOn;
	}
	
	public boolean getIsolatedOn() {
		return isolatedOn;
	}

	/**
	 * Open an include file. Searches first the include path
	 * defined by {@link #setIncludePath(String)} and then the
	 * package orc.inc. This means you can store include files
	 * as either files or class resources (which will continue to
	 * work when Orc is deployed as a servlet or JAR).
	 * 
	 * @param name
	 *            of the include file relative to some directory of include path
	 *            or package orc.inc
	 *        relativeTo
	 *            The path that should be considered the "current directory" if
	 *            any include path is relative; typically this is the directory
	 *            containing the program file which declared the include.
	 *            If null, ignore all relative include paths.
	 * @return Reader to read the file.
	 * @throws FileNotFoundException
	 *             if the resource is not found.
	 */
	public final Reader openInclude(final String name, final String relativeTo) throws FileNotFoundException {
		for (final String ip : includePath) {
			File incPath = new File(ip);

			/* Prefix relative paths as necessary */
			if (!incPath.isAbsolute()) {
				if (relativeTo == null) {
					/* If relativeTo is null, ignore all relative include paths.
					 * This overrides the default File behavior,
					 * which is to resolve relative paths using the system-specific
					 * user.dir property. To recover that behavior,
					 * use that user.dir location as the relativeTo 
					 * argument explicitly. 
					 */
					continue;
				} else {
					// Otherwise use relativeTo as the prefix for the relative include path
					incPath = new File(relativeTo, ip);
				}
			}

			final File file = new File(incPath, name);

			if (!file.exists()) {
				continue;
			}
			return new FileReader(file);
		}
		final InputStream stream = Config.class.getResourceAsStream("/orc/inc/" + name);
		if (stream == null) {
			throw new FileNotFoundException("Include file '" + name + "' not found; check the include path.");
		}
		return new InputStreamReader(stream);
	}

	public final Class loadClass(final String name) throws ClassNotFoundException {
		return classLoader.loadClass(name);
	}

	@Override
	public Config clone() {
		Config out;
		try {
			out = (Config) super.clone();
		} catch (final CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
		out.includes = (LinkedList<String>) includes.clone();
		out.includePath = includePath.clone();
		out.caps = (HashMap<String, Boolean>) caps.clone();
		return out;
	}


}
