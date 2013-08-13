//
// CompileLogger.java -- Java interface CompileLogger
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.compiletime;

import orc.ast.AST;
import orc.compile.parse.OrcInputContext;
import scala.util.parsing.input.Position;

/**
 * Interface to environment's message mechanism for compiler diagnostics. This
 * my be writing to stderr, updating a GUI compile status window, or the like.
 * 
 * @author jthywiss
 */
public interface CompileLogger {
    public enum Severity {
        /**
         * Severity of this message is not known.
         */
        UNKNOWN,

        /**
         * Severity of this message is internal, for tool debugging -- users
         * don't care.
         */
        DEBUG,

        /**
         * Severity of this message is completely routine. For example, counts
         * of output size.
         */
        INFO,

        /**
         * Severity of this message is not routine, but not a problem.
         */
        NOTICE,

        /**
         * Severity of this message is a potential problem, but not bad enough
         * to cause output to be disregarded -- it may still be usable.
         */
        WARNING,

        /**
         * Severity of this message is a problem that is severe enough that
         * output was discarded or should be discarded -- it is not usable.
         */
        ERROR,

        /**
         * Severity of this message is a problem that has caused input
         * processing to be stopped.
         */
        FATAL,

        /**
         * Severity of this message is an internal failure of the tool (not the
         * user's fault).
         */
        INTERNAL
    }

    /**
     * Record that compile processing has begun for the given file. This also
     * resets maxSeverity. Do not call for included files, only for the "main"
     * file that the compiler was invoked on.
     * 
     * @param inputContext OrcInputContext which compiler processing is
     *            beginning
     */
    public void beginProcessing(OrcInputContext inputContext);

    /**
     * Record that the compiler is processing a depedency. Do not call for the
     * "main" file that the compiler was invoked on. Only call after
     * {@link #beginProcessing(OrcInputContext)}.
     * 
     * @param inputContext OrcInputContext of dependency which compiler is
     *            processing
     */
    public void beginDependency(OrcInputContext inputContext);

    /**
     * Record a compile problem message. This message is forwarded to the
     * environment in a manner specific to the implementing class.
     * 
     * @param severity {@link Severity} of this message
     * @param code integer code of this message (potentially used for filtering,
     *            is supported by the environment}
     * @param message String localized message specifically describing problem
     *            for the complier user
     * @param location {@link Position} where problem occurred. Indicate unknown
     *            location with a partially filled Position.
     * @param astNode {@link AST} subtree containing problem, or null if not
     *            applicable or not available.
     * @param exception {@link Throwable} indicating problem, or null if not
     *            applicable or not available.
     */
    public void recordMessage(Severity severity, int code, String message, Position location, AST astNode, Throwable exception);

    /**
     * Convenience method, equivalent to
     * <code>recordMessage(severity, code, message, location, null, exception)</code>
     * 
     * @param severity {@link Severity} of this message
     * @param code integer code of this message (potentially used for filtering,
     *            is supported by the environment}
     * @param message String localized message specifically describing problem
     *            for the complier user
     * @param location {@link Position} where problem occurred. Indicate unknown
     *            location with a partially filled Position.
     * @param exception {@link Throwable} indicating problem, or null if not
     *            applicable or not available.
     */
    public void recordMessage(Severity severity, int code, String message, Position location, Throwable exception);

    /**
     * Convenience method, equivalent to
     * <code>recordMessage(severity, code, message, location, astNode, null)</code>
     * 
     * @param severity {@link Severity} of this message
     * @param code integer code of this message (potentially used for filtering,
     *            is supported by the environment}
     * @param message String localized message specifically describing problem
     *            for the complier user
     * @param location {@link Position} where problem occurred. Indicate unknown
     *            location with a partially filled Position.
     * @param astNode {@link AST} subtree containing problem, or null if not
     *            applicable or not available.
     */
    public void recordMessage(Severity severity, int code, String message, Position location, AST astNode);

    /**
     * Convenience method, equivalent to
     * <code>recordMessage(severity, code, message, null, null, null)</code>
     * 
     * @param severity {@link Severity} of this message
     * @param code integer code of this message (potentially used for filtering,
     *            is supported by the environment}
     * @param message String localized message specifically describing problem
     *            for the complier user
     */
    public void recordMessage(Severity severity, int code, String message);

    /**
     * @return maximum severity of messages recoded since
     *         {@link #beginProcessing(OrcInputContext)} was invoked
     */
    public Severity getMaxSeverity();

    /**
     * Record that the compiler has completed processing a depedency. Do not
     * call for the "main" file that the compiler was invoked on. Only call
     * after {@link #beginDependency(OrcInputContext)}.
     * 
     * @param inputContext OrcInputContext of dependency which compiler is
     *            processing
     */
    public void endDependency(OrcInputContext inputContext);

    /**
     * Record that compile processing is complete for the given file. Only call
     * after {@link #beginProcessing(OrcInputContext)}.
     * 
     * @param inputContext OrcInputContext which compiler processing is complete
     */
    public void endProcessing(OrcInputContext inputContext);
}
