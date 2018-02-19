//
// AbstractCompilerService.java -- Java class AbstractCompilerService
// Project Orchard
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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

import orc.ast.oil.nameless.Expression;
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
 * 
 * @author quark
 */
public abstract class AbstractCompilerService implements orc.orchard.api.CompilerServiceInterface {
    protected static Logger logger = Logger.getLogger("orc.orchard.compile");
    private static StandardOrcCompiler compiler;

    protected AbstractCompilerService() {
        super();
    }

    private static List<String> orchardIncludePath = new java.util.ArrayList<String>(0);
    private static List<String> orchardAdditionalIncludes = Arrays.asList("orchard.inc");

    @Override
    public String compile(final String devKey, final String program) throws InvalidProgramException {
        logger.finer("Orchard compiler: compile(" + devKey + ", " + program + ")");
        if (program == null) {
            throw new InvalidProgramException("Null program!");
        }
        try {
            final OrcBindings options = new OrcBindings(OrchardProperties.newHashMapCopy());
            // Disable file resources for includes
            options.includePath_$eq(orchardIncludePath);
            // Include sites specifically for orchard services
            options.additionalIncludes_$eq(orchardAdditionalIncludes);
            final List<CompileMessage> compileMsgs = new LinkedList<CompileMessage>();
            final CompileLogger cl = new OrchardCompileLogger(compileMsgs);
            // TODO: Update to use Backend system.
            final Expression result = (Expression) getCompiler().apply(new OrcStringInputContext(program), options, cl, NullProgressMonitor$.MODULE$);
            if (result == null || cl.getMaxSeverity().compareTo(Severity.WARNING) > 0) {
                if (compileMsgs.isEmpty()) {
                    final InvalidProgramException e = new InvalidProgramException("Compilation failed");
                    logger.throwing(getClass().getCanonicalName(), "compile", e);
                    throw e;
                } else {
                    final InvalidProgramException e = new InvalidProgramException(compileMsgs);
                    logger.throwing(getClass().getCanonicalName(), "compile", e);
                    throw e;
                }
                //FIXME:Report warnings
            } else {
                final String oilString = OrcXML.astToXml(result).toString();
                //System.err.println(oilString);
                return oilString;
            }
        } catch (final IOException e) {
            final InvalidProgramException ipe = new InvalidProgramException("I/O error: " + e.getMessage());
            logger.throwing(getClass().getCanonicalName(), "compile", ipe);
            throw ipe;
        }
    }

    protected StandardOrcCompiler getCompiler() {
        synchronized (this) {
            if (compiler == null) {
                // TODO: Update to use Backend system.
                compiler = new StandardOrcCompiler();
            }
        }
        return compiler;
    }

}
