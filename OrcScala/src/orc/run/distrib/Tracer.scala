//
// Tracer.scala -- Scala object Tracer
// Project OrcScala
//
// Created by jthywiss on Feb 21, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

/** Tracer utility for orc.run.distrib package. 
  *
  * @see orc.util.Tracer
  * @author jthywiss
  */
object Tracer {

  final val TokenSend = 11L
  orc.util.Tracer.registerEventTypeId(TokenSend, "TokSend ")

  final val TokenReceive = 12L
  orc.util.Tracer.registerEventTypeId(TokenReceive, "TokRecv ")

  @inline
  def traceTokenSend(token: orc.run.core.Token/*, destination: Location*/) {
    orc.util.Tracer.trace(TokenSend, token.debugId, 0L, 0L)
  }

  @inline
  def traceTokenReceive(token: orc.run.core.Token/*, destination: Location*/) {
    orc.util.Tracer.trace(TokenReceive, token.debugId, 0L, 0L)
  }

}
