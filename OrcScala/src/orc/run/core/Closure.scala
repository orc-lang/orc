//
// Closure.scala -- Scala class Closure
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.{ OrcRuntime, Schedulable }
import orc.ast.oil.nameless.Def
import orc.util.BlockableMapExtension
import java.io.ObjectStreamException

/** A closure that both resolves itself and represents the closure itself. This should
  * be scheduled when it is created.
  *
  * @author dkitchin, amp
  */
class Closure(
  private[run] val index: Int,
  val closureGroup: ClosureGroup)
  extends ResolvableCollectionMember[Def](index, closureGroup) with Serializable {
  def code: Def = definition

  override def toString = super.toString + (code.body.sourceTextRange, closureGroup, index)

  @throws(classOf[ObjectStreamException])
  protected def writeReplace(): AnyRef = {
    new ClosureMarshalingReplacement(index, closureGroup)
  }
}

protected case class ClosureMarshalingReplacement(index: Int, closureGroup: ClosureGroup) {
  @throws(classOf[ObjectStreamException])
  protected def readResolve(): AnyRef = new Closure(index, closureGroup)
}

class ClosureGroup(
  _defs: List[Def],
  _lexicalContext: List[Binding],
  runtime: OrcRuntime)
  extends ResolvableCollection[Def, Closure](_defs, _lexicalContext, runtime) {
  def buildMember(i: Int) = new Closure(i, this)
}
