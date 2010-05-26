//
// OrcCompiler.scala -- Scala class OrcCompiler
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import scala.util.parsing.input.Reader
import scala.util.parsing.input.StreamReader

/**
 * 
 *
 * @author jthywiss
 */
class OrcCompiler {
	def compile(source: Reader[Char]): orc.oil.Expression = {
		// 1. Parse
		val extendedAst = OrcParser.parse(source).get
		// 2. Translate extended AST to OIL
		translateToOil(extendedAst)
		// 3. Call refineOilAfterCompileBeforeSave hook for extenders
	}

	def compile(source: java.io.Reader): orc.oil.Expression = compile(StreamReader(source))

}
