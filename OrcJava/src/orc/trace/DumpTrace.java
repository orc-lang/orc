package orc.trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import orc.error.compiletime.ParsingException;
import orc.trace.query.EventCursor;
import orc.trace.query.FilteredEventCursor;
import orc.trace.query.InputStreamEventCursor;
import orc.trace.query.StrongBackwardEventCursor;
import orc.trace.query.WeakBackwardEventCursor;
import orc.trace.query.EventCursor.EndOfStream;
import orc.trace.query.parser.Parser;
import orc.trace.query.predicates.Predicate;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class DumpTrace {
	private EventCursor in;
	private Predicate filter = null;

	@Option(name="-help",usage="Show command-line argument usage")
	public void printUsage(boolean _) throws CmdLineException{
		throw new CmdLineException("");
	}
	
	@Option(name="-f",usage="Filter events according to the given predicate.\n" +
			"Example: -f '@=call, call.type=\"call\",\n" +
			"             F @=resume, resume.type=\"resume\",\n" +
			"               resume.thread=call.thread'")
	public void setFilter(String predicate) throws CmdLineException{
		Parser p = new Parser(new StringReader(predicate), "");
		try {
			this.filter = p.parseQuery();
		} catch (ParsingException e) {
			throw new CmdLineException("Error parsing -f ... at " + e.getMessage());
		} catch (IOException e) {
			throw new CmdLineException(e);
		}
	}
	
	@Argument(metaVar="file", required=true, usage="Input file. Omit to use STDIN.")
	public void setInputFile(File file) throws CmdLineException {
		try {
			in = new StrongBackwardEventCursor(
					new InputStreamEventCursor(
							new FileInputStream(file)));
		} catch (FileNotFoundException e) {
			throw new CmdLineException("Could not find input file '"+file+"'");
		} catch (IOException e) {
			throw new CmdLineException(e);
		}
	}

	public void processArgs(String[] args) {
		CmdLineParser parser = new CmdLineParser(this); 
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e1) {
			System.err.println(e1.getMessage());
			System.err.println("Usage: java -cp orc.jar orc.trace.DumpTrace [options] [file]");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}
	
	public void run() {
		assert(in != null);
		if (filter != null) {
			FilteredEventCursor in1 = new FilteredEventCursor(this.in, filter);
			try {
				while (true) {
					System.out.println(in1.frame().toString());
					in1 = in1.forward();
				}
			} catch (EndOfStream _) {}
		} else {
			EventCursor in1 = this.in;
			try {
				while (true) {
					System.out.println(in1.current().toString());
					in1 = in1.forward();
				}
			} catch (EndOfStream _) {}
		}
	}
	
	public static void main(String[] args) {
		DumpTrace x = new DumpTrace();
		x.processArgs(args);
		x.run();
	}
}
