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
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Pub;
import orc.runtime.nodes.result.WriteResult;

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

	private Node target = new Pub();
	private Boolean debug = false;
	private List<String> includes = new LinkedList<String>();
	private Integer maxpub = null;
	private Reader instream = new InputStreamReader(System.in);
	
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
		} catch (FileNotFoundException e) {
			throw new CmdLineException("Could not find input file '"+file+"'");
		}
	}
	
	public void processEnvVars() 
	{ 
		// TODO: implement environment variable processing of configuration options
	}
	
	public Node getTarget()
	{
		return target;
	}
	
	public Boolean debugMode()
	{
		return debug;
	}
	
	public Integer maxPubs()
	{
		return maxpub;
	}
	
	public Reader getInstream()
	{
		return instream;
	}
	
	public List<String> getIncludes()
	{
		return includes;
	}
	
}