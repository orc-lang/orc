//
// OrcScriptException.java -- Java class OrcScriptException
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Aug 3, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script;

import java.io.PrintStream;
import java.io.PrintWriter;

import javax.script.ScriptException;

import orc.compile.parse.PositionWithFilename;
import orc.error.OrcException;
import scala.util.parsing.input.Position;

/**
 * A ScriptException proxy for an OrcException
 *
 * @author jthywiss
 */
@SuppressWarnings("serial")
// We don't care about serialization compatibility of Exceptions
public class OrcScriptException extends ScriptException {

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

  /*
   * (non-Javadoc)
   * @see javax.script.ScriptException#getColumnNumber()
   */
  @Override
  public int getColumnNumber() {
    return orcE.getPosition().column();
  }

  /*
   * (non-Javadoc)
   * @see javax.script.ScriptException#getFileName()
   */
  @Override
  public String getFileName() {
    final Position pos = orcE.getPosition();
    if (pos instanceof PositionWithFilename) {
      return ((PositionWithFilename) pos).filename();
    } else {
      return "";
    }
  }

  /*
   * (non-Javadoc)
   * @see javax.script.ScriptException#getLineNumber()
   */
  @Override
  public int getLineNumber() {
    return orcE.getPosition().line();
  }

  /*
   * (non-Javadoc)
   * @see javax.script.ScriptException#getMessage()
   */
  @Override
  public String getMessage() {
    return orcE.getMessage();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#fillInStackTrace()
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    throw new UnsupportedOperationException("OrcScriptException.fillInStackTrace is not supported");
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#getCause()
   */
  @Override
  synchronized public Throwable getCause() {
    return orcE.getCause();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#getLocalizedMessage()
   */
  @Override
  public String getLocalizedMessage() {
    return orcE.getLocalizedMessage();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#getStackTrace()
   */
  @Override
  public StackTraceElement[] getStackTrace() {
    return orcE.getStackTrace();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#initCause(java.lang.Throwable)
   */
  @Override
  public synchronized Throwable initCause(final Throwable cause) {
    throw new UnsupportedOperationException("OrcScriptException.initCause is not supported");
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#printStackTrace()
   */
  @Override
  public void printStackTrace() {
    orcE.printStackTrace();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
   */
  @Override
  public void printStackTrace(final PrintStream s) {
    orcE.printStackTrace(s);
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#printStackTrace(java.io.PrintWriter)
   */
  @Override
  public void printStackTrace(final PrintWriter s) {
    orcE.printStackTrace(s);
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#setStackTrace(java.lang.StackTraceElement[])
   */
  @Override
  public void setStackTrace(final StackTraceElement[] stackTrace) {
    throw new UnsupportedOperationException("OrcScriptException.setStackTrace is not supported");
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#toString()
   */
  @Override
  public String toString() {
    return orcE.toString();
  }

}
