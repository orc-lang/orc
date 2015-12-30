//
// TokenReplacement.scala -- Scala class TokenReplacement
// Project OrcScala
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

import orc.ast.AST
import orc.ast.oil.nameless.Expression
import orc.run.core.{ Binding, BindingFrame, BoundFuture, BoundStop, BoundValue, EmptyFrame, Frame, FunctionFrame, FutureFrame, Group, GroupFrame, Live, RightSidePublished, RightSideSilent, RightSideUnknown, SequenceFrame, Token, TokenState, VirtualClock }

/** Replacement for a Token for use in serialization.
  *
  * @author jthywiss
  */
@SerialVersionUID(-655352528693128511L)
class TokenReplacement(t: Token, astRoot: Expression, tokenProxy: RemoteGroupMembersProxy) extends Serializable {

  Logger.fine("TokenReplacement fields: " + getClass.getDeclaredFields.mkString("; "))

  private def envHack(b: Binding): Binding = (b match {
    case BoundFuture(g) => {
      g.state match {
        case RightSidePublished(None) => BoundStop
        case RightSidePublished(Some(v)) => BoundValue(v)
        case RightSideSilent => BoundStop
        case RightSideUnknown(_) => {
          Logger.warning(s"$b not bound yet, replacing with stop")
          BoundStop
        }
      }
    }
    case x => x
  }) match {
    case BoundValue(v) if (!v.isInstanceOf[java.io.Serializable]) => {
      Logger.warning(s"$v not Serializable, replacing with stop")
      BoundStop
    }
    case x => x
  }

  private def stackHack(ast: Expression)(f: Frame): FrameReplacement = {
    f match {
      case BindingFrame(n, previous) => BindingFrameReplacement(n)
      case SequenceFrame(node, previous) => SequenceFrameReplacement(AstNodeIndexing.nodeIndexInTree(node, ast).get)
      case FunctionFrame(callpoint, env, previous) => FunctionFrameReplacement(AstNodeIndexing.nodeIndexInTree(callpoint, ast).get, (env map envHack).toArray)
      case FutureFrame(k, previous) => ??? //FIXME:Need to refactor FutureFrame to eliminate closures
      case GroupFrame(previous) => GroupFrameReplacement
    }
  }

  val astNodeIndex: Seq[Int] = AstNodeIndexing.nodeIndexInTree(t.getNode, astRoot).get

  /* N.B.: The stack's EmptyFrame will not appear in the array, because of Frame's iterator behavior */
  private def extractStack(t: Token, astRoot: Expression) = (t.getStack.toArray.reverse) map stackHack(astRoot)

  val stack = extractStack(t, astRoot)
  val env = (t.getEnv map envHack).toArray
  val tokenProxyId = tokenProxy.thisProxyId

  //val group = t.getGroup
  //val clock: VirtualClock = t.getClock
  //val state: TokenState =
  //assert(t.state == Live)

  def asToken(astRoot: Expression, newGroup: Group) = {
    val _node = AstNodeIndexing.lookupNodeInTree(astRoot, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.asFrame(stackTop, astRoot) }

    new MigratedToken(_node, _stack, env.toList, newGroup /*, clock, state*/ )
  }

}

/** A Token that was moved to this runtime engine from another.
  * Class exists just to provide a constructor for this situation.
  * Otherwise, local and migrated tokens are indistinguishable.
  *
  * @author jthywiss
  */
class MigratedToken(
  _node: Expression,
  _stack: Frame,
  _env: List[Binding],
  _group: Group,
  _clock: Option[VirtualClock] = None,
  _state: TokenState = Live)
  extends Token(_node, _stack, _env, _group, _clock, _state) {
}

/** Utility functions for node addresses in trees.
  *
  * @author jthywiss
  */
object AstNodeIndexing {

  def nodeIndexInTree(node: AST, tree: AST): Option[Seq[Int]] = {
    if (node eq tree) return Some(Seq[Int]())
    val children = tree.subtrees
    var childNum = 0
    for (child <- children) {
      val indexInChild = nodeIndexInTree(node, child)
      if (indexInChild.isDefined) return Some(childNum +: indexInChild.get)
      childNum += 1
    }
    None
  }

  def lookupNodeInTree(tree: AST, index: Seq[Int]): AST = {
    if (index.isEmpty)
      tree
    else
      lookupNodeInTree(tree.subtrees.toIndexedSeq.apply(index.head), index.tail)
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
  def asFrame(previous: Frame, ast: Expression) = SequenceFrame(AstNodeIndexing.lookupNodeInTree(ast, nodeAddr).asInstanceOf[Expression], previous)
}
protected case class FunctionFrameReplacement(callpointAddr: Seq[Int], env: Array[Binding]) extends FrameReplacement() {
  def asFrame(previous: Frame, ast: Expression) = FunctionFrame(AstNodeIndexing.lookupNodeInTree(ast, callpointAddr).asInstanceOf[Expression], env.toList, previous)
}
//  protected case class FutureFrameReplacement(k: (Option[AnyRef] => Unit)) extends FrameReplacement() {
//    def asFrame(previous: Frame, ast: Expression) = FutureFrame(k, previous)
//  }
protected case object GroupFrameReplacement extends FrameReplacement() {
  def asFrame(previous: Frame, ast: Expression) = GroupFrame(previous)
}
