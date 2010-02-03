//
// DumpTrace.java -- Java class DumpTrace
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import orc.trace.EventCursor.EndOfStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class DumpTrace {
	private EventCursor in;

	@Option(name = "-help", usage = "Show command-line argument usage")
	public void printUsage(final boolean _) throws CmdLineException {
		throw new CmdLineException("");
	}

	@Argument(metaVar = "file", required = true, usage = "Input file. Omit to use STDIN.")
	public void setInputFile(final File file) throws CmdLineException {
		try {
			in = new BackwardEventCursor(new InputStreamEventCursor(new FileInputStream(file)));
		} catch (final FileNotFoundException e) {
			throw new CmdLineException("Could not find input file '" + file + "'");
		} catch (final IOException e) {
			throw new CmdLineException(e);
		}
	}

	public void processArgs(final String[] args) {
		final CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (final CmdLineException e1) {
			System.err.println(e1.getMessage());
			System.err.println("Usage: java -cp orc.jar orc.trace.DumpTrace [options] [file]");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}

	public void run() {
		assert in != null;
		EventCursor in1 = in;
		try {
			while (true) {
				System.out.println(Terms.printToString(in1.current()));
				in1 = in1.forward();
			}
		} catch (final EndOfStream _) {
		}
	}

	public static void main(final String[] args) {
		final DumpTrace x = new DumpTrace();
		x.processArgs(args);
		x.run();
	}
}
