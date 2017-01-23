//
// OrcScriptException.java -- Java class OrcScriptException
// Project OrcScala
//
// Created by jthywiss on Aug 3, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script;

import java.io.PrintStream;
import java.io.PrintWriter;

import javax.script.ScriptException;

import orc.compile.parse.OrcSourceRange;
import orc.error.OrcException;

/**
 * A ScriptException proxy for an OrcException
 *
 * @author jthywiss
 */
public class OrcScriptException extends ScriptException {
    private static final long serialVersionUID = 8541712728830055894L;

    private final OrcException orcE;

    /**
     * Constructs an object of class OrcScriptException.
     *
     * @param e
     */
    public OrcScriptException(final OrcException e) {
        super(e);
        orcE = e;
    }

    @Override
    public String getFileName() {
        final OrcSourceRange pos = orcE.getPosition();
        if (pos != null) {
            return pos.start().resource().descr();
        } else {
            return "";
        }
    }

    @Override
    public int getLineNumber() {
        final OrcSourceRange pos = orcE.getPosition();
        if (pos != null) {
            return pos.start().line();
        } else {
            return -1;
        }
    }

    @Override
    public int getColumnNumber() {
        final OrcSourceRange pos = orcE.getPosition();
        if (pos != null) {
            return pos.start().column();
        } else {
            return -1;
        }
    }

    @Override
    public String getMessage() {
        return orcE.getMessage();
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        throw new UnsupportedOperationException("OrcScriptException.fillInStackTrace is not supported");
    }

    @Override
    synchronized public Throwable getCause() {
        return orcE.getCause();
    }

    @Override
    public String getLocalizedMessage() {
        return orcE.getLocalizedMessage();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return orcE.getStackTrace();
    }

    @Override
    public synchronized Throwable initCause(final Throwable cause) {
        throw new UnsupportedOperationException("OrcScriptException.initCause is not supported");
    }

    @Override
    public void printStackTrace() {
        orcE.printStackTrace();
    }

    @Override
    public void printStackTrace(final PrintStream s) {
        orcE.printStackTrace(s);
    }

    @Override
    public void printStackTrace(final PrintWriter s) {
        orcE.printStackTrace(s);
    }

    @Override
    public void setStackTrace(final StackTraceElement[] stackTrace) {
        throw new UnsupportedOperationException("OrcScriptException.setStackTrace is not supported");
    }

    @Override
    public String toString() {
        return orcE.toString();
    }

}
