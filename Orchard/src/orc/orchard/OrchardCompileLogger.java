//
// OrchardCompileLogger.java -- Java class OrchardCompileLogger
// Project Orchard
//
// Created by jthywiss on Aug 26, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.List;

import orc.ast.AST;
import orc.compile.parse.OrcInputContext;
import orc.compile.parse.OrcSourceRange;
import orc.error.compiletime.CompileLogger;

/**
 * A CompileMessageRecorder that collects logged compile messages into a list
 *
 * @author jthywiss
 */
public class OrchardCompileLogger implements CompileLogger {
    /**
     * A record of a logged compile message
     *
     * @author jthywiss
     */
    public static class CompileMessage {

        public final Severity severity;
        public final int code;
        public final String message;
        public final OrcSourceRange position;
        public final AST astNode;
        public final Throwable exception;

        /**
         * Constructs an object of class CompileMessage.
         *
         * @param severity
         * @param code
         * @param message
         * @param position
         * @param astNode
         * @param exception
         */
        public CompileMessage(final Severity severity, final int code, final String message, final scala.Option<OrcSourceRange> position, final AST astNode, final Throwable exception) {
            this.severity = severity;
            this.code = code;
            this.message = message;
            this.position = position != null && position.isDefined() ? position.get() : null;
            this.astNode = astNode;
            this.exception = exception;
        }

        public String longMessage() {
            if (position != null) {
                return position.toString() + ": " + message + "\n" + position.lineContentWithCaret();
            } else {
                return "<undefined position>: " + message;
            }
        }

    }

    private Severity maxSeverity = Severity.UNKNOWN;
    private final List<CompileMessage> msgList;

    /**
     * Constructs an object of class OrchardCompileLogger.
     */
    public OrchardCompileLogger(final List<CompileMessage> msgList) {
        this.msgList = msgList;
    }

    @Override
    public void beginProcessing(final OrcInputContext inputContext) {
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

        msgList.add(new CompileMessage(severity, code, message, location, astNode, exception));
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
