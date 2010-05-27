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

import orc.error.compiletime.CompilationException
import orc.error.compiletime.CompileLogger
import orc.error.compiletime.CompileLogger.Severity

/**
 * 
 *
 * @author jthywiss
 */
class OrcCompiler extends OrcCompilerAPI {

	val translator = new orc.translation.Translator
		
	def compile(options: OrcOptions, source: Reader[Char]): orc.oil.Expression = {
			try {
				compileLogger.beginProcessing(options.filename)
				val extendedAst = OrcParser.parse(options, source).get
				val oilAst = translator.translate(options, extendedAst)
				val refinedAst = refineOil(oilAst)
				refinedAst
			} catch {case e: CompilationException =>
				compileLogger.recordMessage(Severity.FATAL, 0, e.getMessageOnly(), e.getSourceLocation(), null, e)
				null
			} finally {
				compileLogger.endProcessing(options.filename)
			}
	}

	def compile(options: OrcOptions, source: java.io.Reader): orc.oil.Expression = compile(options, StreamReader(source))

	def compileLogger: CompileLogger = null

}
