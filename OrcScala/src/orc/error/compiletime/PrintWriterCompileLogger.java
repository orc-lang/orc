//
// PrintWriterCompileLogger.java -- Java class PrintWriterCompileLogger
// Project OrcScala
//
// Created by jthywiss on Aug 19, 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.compiletime;

import java.io.PrintWriter;

import orc.ast.AST;
import orc.compile.parse.OrcInputContext;
import orc.compile.parse.OrcSourceRange;

/**
 * A CompileMessageRecorder that writes messages to a PrintWriter (such as one
 * for stderr).
 *
 * @author jthywiss
 */
public class PrintWriterCompileLogger implements CompileLogger {
    private final PrintWriter outWriter;

    private Severity maxSeverity = Severity.UNKNOWN;

    /**
     * Constructs an object of class PrintWriterCompileLogger.
     *
     * @param logToWriter the Writer configuration in use.
     */
    public PrintWriterCompileLogger(final PrintWriter logToWriter) {
        if (logToWriter == null) {
            throw new NullPointerException("Cannot construct PrintWriterCompileLogger with a null PrintWriter");
        }
        outWriter = logToWriter;
    }

    @Override
    public void beginProcessing(final OrcInputContext inputContext) {
        if (outWriter == null) {
            throw new NullPointerException("Cannot use a options with a null stderr");
        }
        maxSeverity = Severity.UNKNOWN;
    }

    @Override
    public void endProcessing(final OrcInputContext inputContext) {
        // Nothing needed
    }

    @Override
    public void beginDependency(final OrcInputContext inputContext) {
        // Nothing needed
    }

    @Override
    public void endDependency(final OrcInputContext inputContext) {
        // Nothing needed
    }

    @Override
    public void recordMessage(final Severity severity, final int code, final String message, final scala.Option<OrcSourceRange> location, final AST astNode, final Throwable exception) {

        maxSeverity = severity.ordinal() > maxSeverity.ordinal() ? severity : maxSeverity;

        if (location != null && location.isDefined()) {
            outWriter.println(location.get().toString() + ": " + message + (exception instanceof CompilationException ? " [[OrcWiki:" + exception.getClass().getSimpleName() + "]]" : ""));
            outWriter.println(location.get().lineContentWithCaret());
        } else {
            outWriter.println("<undefined position>: " + message);
        }
    }

    @Override
    public void recordMessage(final Severity severity, final int code, final String message, final scala.Option<OrcSourceRange> location, final Throwable exception) {
        recordMessage(severity, code, message, location, null, exception);
    }

    @Override
    public void recordMessage(final Severity severity, final int code, final String message, final scala.Option<OrcSourceRange> location, final AST astNode) {
        recordMessage(severity, code, message, location, astNode, null);
    }

    @Override
    public void recordMessage(final Severity severity, final int code, final String message) {
        recordMessage(severity, code, message, null, null, null);
    }

    @Override
    public Severity getMaxSeverity() {
        return maxSeverity;
    }
}
