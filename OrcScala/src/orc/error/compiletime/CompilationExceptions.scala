//
// CompilationExceptions.scala -- Scala child classes of CompilationException
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Aug 11, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.compiletime

import scala.util.parsing.input.Position

/**
 * Problem parsing the text of an Orc program. Mostly this
 * is a wrapper around the exceptions thrown by whatever
 * parsing library we use.
 */
class ParsingException(message: String, errorPos: Position) extends
  CompilationException(message) {
  setPosition(errorPos)
}

/**
 * 
 */
class PatternException(message: String) extends
  CompilationException(message)

/**
 * Indicate a problem with site resolution. Ideally
 * this would be a loadtime error, but currently site
 * resolution is done at runtime.
 */
class SiteResolutionException(val siteName: String, cause: Throwable) extends
  CompilationException("Cannot resolve site "+siteName, cause)

/**
 * 
 */
class UnboundVariableException(val varName: String) extends
  CompilationException("Variable " + varName + " is unbound")
