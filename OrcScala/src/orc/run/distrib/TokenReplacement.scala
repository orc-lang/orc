//
// TokenReplacement.scala -- Scala class TokenReplacement
// Project project_name
//
// Created by jthywiss on Dec 26, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import orc.ast.oil.nameless.Expression
import orc.run.core.{ Binding, BindingFrame, EmptyFrame, Frame, FunctionFrame, FutureFrame, Group, GroupFrame, SequenceFrame, Token }

/** Replacement for a Token for use in serialization.
  *
  * @author jthywiss
  */
@SerialVersionUID(-655352528693128511L)
class TokenReplacement(t: Token, astRoot: Expression, tokenProxy: RemoteGroupMembersProxy) extends Serializable {

  Logger.fine("TokenReplacement fields: "+getClass.getDeclaredFields.mkString("; "))

  private def envHack(b: orc.run.core.Binding): orc.run.core.Binding = (b match {
    case orc.run.core.BoundFuture(g) => {
      g.state match {
        case orc.run.core.RightSidePublished(None) => orc.run.core.BoundStop
        case orc.run.core.RightSidePublished(Some(v)) => orc.run.core.BoundValue(v)
        case orc.run.core.RightSideSilent => orc.run.core.BoundStop
        case orc.run.core.RightSideUnknown(_) => {
          Logger.warning(s"$b not bound yet, replacing with stop")
          orc.run.core.BoundStop
        }
      }
    }
    case x => x
  }) match {
    case orc.run.core.BoundValue(v) if (!v.isInstanceOf[java.io.Serializable]) => {
      Logger.warning(s"$v not Serializable, replacing with stop")
      orc.run.core.BoundStop
    }
    case x => x
  }


  private def stackHack(ast: Expression)(f: Frame): FrameReplacement = {
    f match {
      case BindingFrame(n, previous) => BindingFrameReplacement(n)
      case SequenceFrame(node, previous) => SequenceFrameReplacement(AstNodeAddressing.nodeAddressInTree(node, ast).get)
      case FunctionFrame(callpoint, env, previous) => FunctionFrameReplacement(AstNodeAddressing.nodeAddressInTree(callpoint, ast).get, (env map envHack).toArray)
      case FutureFrame(k, previous) => ??? //FIXME:Need to refactor FutureFrame to eliminate closures
      case GroupFrame(previous) => GroupFrameReplacement
    }
  }

  val nodeNum: Seq[Int] = AstNodeAddressing.nodeAddressInTree(t.getNode, astRoot).get

  /* N.B.: The stack's EmptyFrame will not appear in the array, because of Frame's iterator behavior */
  private def extractStack(t: Token, astRoot: Expression) = (t.getStack.toArray.reverse) map stackHack(astRoot)

  val stack = extractStack(t,astRoot)
  val env = (t.getEnv map envHack).toArray
  val tokenProxyId = tokenProxy.proxyId

  //val group = t.getGroup
  //val clock: VirtualClock = t.getClock
  //val state: TokenState =
  //assert(t.state == Live)

  def asToken(astRoot: Expression, newGroup: Group) = {
    val _node = AstNodeAddressing.lookupNodeInTree(astRoot, nodeNum).asInstanceOf[orc.ast.oil.nameless.Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame){ (stackTop, addFrame) => addFrame.asFrame(stackTop, astRoot) }

    new MigratedToken(_node, _stack, env.toList, newGroup/*, clock, state*/)
  }

}


/** A Token that was moved to this runtime engine from another.
  * Class exists just to provide a constructor for this situation.
  * Otherwise, local and migrated tokens are indistinguishable. 
  *
  * @author jthywiss
  */
class MigratedToken(
    _node: orc.ast.oil.nameless.Expression,
    _stack: orc.run.core.Frame,
    _env: List[orc.run.core.Binding],
    _group: orc.run.core.Group,
    _clock: Option[orc.run.core.VirtualClock] = None,
    _state: orc.run.core.TokenState = orc.run.core.Live)
extends Token(_node, _stack, _env, _group, _clock, _state) {
}


/** Utility functions for node addresses in trees.
  *
  * @author jthywiss
  */
object AstNodeAddressing {

  def nodeAddressInTree(node: Expression, tree: Expression): Option[Seq[Int]] = {
    if (node eq tree) return Some(Seq[Int]())
    val children = tree.subtrees.asInstanceOf[Iterable[Expression]]
    var childNum = 0
    for (child <- children) {
      val addrInChild = nodeAddressInTree(node, child)
      if (addrInChild.isDefined) return Some(childNum +: addrInChild.get)
      childNum += 1
    }
    None
  }

  def lookupNodeInTree(tree: Expression, address: Seq[Int]): Expression = {
    if (address.isEmpty)
      tree
    else
      lookupNodeInTree(tree.subtrees.toIndexedSeq.apply(address.head).asInstanceOf[Expression], address.tail)
  }
}


/** Replacement for a Frame for use in serialization.
  *
  * @author jthywiss
  */
protected abstract class FrameReplacement() {
  def asFrame(previous: Frame, ast: Expression): Frame
}
protected case object EmptyFrameReplacement extends FrameReplacement() {
  def asFrame(previous: Frame, ast: Expression) = throw new AssertionError("EmptyFrameReplacement.asFrame called")
}
protected case class BindingFrameReplacement(n: Int) extends FrameReplacement() {
  def asFrame(previous: Frame, ast: Expression) = BindingFrame(n, previous)
}
protected case class SequenceFrameReplacement(nodeAddr: Seq[Int]) extends FrameReplacement() {
  def asFrame(previous: Frame, ast: Expression) = SequenceFrame(AstNodeAddressing.lookupNodeInTree(ast, nodeAddr), previous)
}
protected case class FunctionFrameReplacement(callpointAddr: Seq[Int], env: Array[Binding]) extends FrameReplacement() {
  def asFrame(previous: Frame, ast: Expression) = FunctionFrame(AstNodeAddressing.lookupNodeInTree(ast, callpointAddr), env.toList, previous)
}
//  protected case class FutureFrameReplacement(k: (Option[AnyRef] => Unit)) extends FrameReplacement() {
//    def asFrame(previous: Frame, ast: Expression) = FutureFrame(k, previous)
//  }
protected case object GroupFrameReplacement extends FrameReplacement() {
  def asFrame(previous: Frame, ast: Expression) = GroupFrame(previous)
}
