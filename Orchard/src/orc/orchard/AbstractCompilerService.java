//
// AbstractCompilerService.java -- Java class AbstractCompilerService
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import orc.ast.oil.xml.OrcXML;
import orc.compile.StandardOrcCompiler;
import orc.compile.parse.OrcStringInputContext;
import orc.error.compiletime.CompileLogger;
import orc.error.compiletime.CompileLogger.Severity;
import orc.orchard.OrchardCompileLogger.CompileMessage;
import orc.orchard.errors.InvalidProgramException;
import orc.progress.NullProgressMonitor$;
import orc.script.OrcBindings;

/**
 * Implementation of a compiler service.
 * @author quark
 */
public abstract class AbstractCompilerService implements orc.orchard.api.CompilerServiceInterface {
	protected static Logger logger = Logger.getLogger("orc.orchard.compile");
	private static StandardOrcCompiler compiler;

	protected AbstractCompilerService() {
		super();
	}

	private static List orchardIncludePath = new java.util.ArrayList<String>(0);
	private static List orchardAdditionalIncludes = Arrays.asList("orchard.inc");

	@Override
	public String compile(final String devKey, final String program) throws InvalidProgramException {
		logger.info("compile(" + devKey + ", " + program + ")");
		if (program == null) {
			throw new InvalidProgramException("Null program!");
		}
		try {
			final OrcBindings options = new OrcBindings(new java.util.HashMap<String, Object>(OrchardProperties.getMap()));
			// Disable file resources for includes
			options.includePath_$eq(orchardIncludePath);
			// Include sites specifically for orchard services
			options.additionalIncludes_$eq(orchardAdditionalIncludes);
			final List<CompileMessage> compileMsgs = new LinkedList<CompileMessage>();
			final CompileLogger cl = new OrchardCompileLogger(compileMsgs);
			final orc.ast.oil.nameless.Expression result = getCompiler().apply(new OrcStringInputContext(program), options, cl, NullProgressMonitor$.MODULE$);
			if (cl.getMaxSeverity().compareTo(Severity.WARNING) > 0) {
				if (compileMsgs.isEmpty()) {
					throw new InvalidProgramException("Compilation failed");
				} else {
					//FIXME:Report multiple messages in a less ugly manner -- maybe in a JobEvent style
					final StringBuilder sb = new StringBuilder();
					for (final CompileMessage msg : compileMsgs) {
						sb.append(msg.longMessage());
						sb.append("\n");
					}
					sb.deleteCharAt(sb.length() - 1); // Remove trailing newline
					throw new InvalidProgramException(sb.toString());
				}
				//FIXME:Report warnings
			} else {
				final String oilString = OrcXML.astToXml(result).toString();
				//System.err.println(oilString);
				return oilString;
			}
		} catch (final IOException e) {
			throw new InvalidProgramException("IO error: " + e.getMessage());
		}
	}

	protected StandardOrcCompiler getCompiler() {
		synchronized (this) {
			if (compiler == null) {
				compiler = new StandardOrcCompiler();
			}
		}
		return compiler;
	}

}
