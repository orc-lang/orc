//
// OrcBacktrace.scala -- Code for building an Orc Backtrace from a Truffle stack.
// Project PorcE
//
// Created by amp on Mar 1, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import scala.collection.JavaConverters._

import orc.compile.parse.OrcSourceRange
import orc.error.runtime.{ JavaException, TokenException }
import orc.run.porce.HasPorcNode

import com.oracle.truffle.api.TruffleStackTraceElement
import com.oracle.truffle.api.nodes.Node

object OrcBacktrace {
  def fromTruffleException(e: Throwable, node: Node): Array[OrcSourceRange] = {
    TruffleStackTraceElement.fillIn(e)
    val truffleFrames = TruffleStackTraceElement.getStackTrace(e).asScala

    def findRange(n: Node): Option[OrcSourceRange] = n match {
        case n: HasPorcNode if n.porcNode.isDefined =>
          n.porcNode flatMap { _.sourceTextRange }
        case n: Node =>
          findRange(n.getParent)
        case null =>
          None
    }
    
    val locations = findRange(node) +: truffleFrames.map(frame => findRange(frame.getLocation))
    locations.flatten.toArray
  }
  
  def orcifyException(e: Throwable, node: Node): TokenException = {
    val backtrace = fromTruffleException(e, node)
    val r = e match {
      case e: TokenException => e
      case e => new JavaException(e)
    }
    r.setBacktrace(backtrace)
    r
  }
}