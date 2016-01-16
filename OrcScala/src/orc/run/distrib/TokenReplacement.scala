//
// TokenReplacement.scala -- Scala class TokenReplacement and its auxiliary classes
// Project OrcScala
//
// Created by jthywiss on Dec 26, 2015.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import orc.ast.AST
import orc.ast.oil.nameless.Expression
import orc.run.core.{ Binding, BindingFrame, BoundFuture, BoundStop, BoundValue, EmptyFrame, Frame, FunctionFrame, FutureFrame, Group, GroupFrame, Live, Publishing, RightSidePublished, RightSideSilent, RightSideUnknown, SequenceFrame, Token, TokenState, VirtualClock }

/** Replacement for a Token for use in serialization.
  *
  * @author jthywiss
  */
@SerialVersionUID(-655352528693128511L)
class TokenReplacement(token: Token, astRoot: Expression, val tokenProxyId: DOrcExecution#GroupProxyId) extends Serializable {

  // Logger.fine("TokenReplacement fields: " + getClass.getDeclaredFields.mkString("; "))

  protected def marshalBinding(execution: DOrcExecution)(b: Binding): BindingReplacement = (b match {
    case BoundFuture(g) => {
      g.state match {
        case RightSidePublished(None) => BoundStop
        case RightSidePublished(Some(v)) => BoundValue(v)
        case RightSideSilent => BoundStop
        case RightSideUnknown(_) => {
          val id = execution.ensureFutureIsRemotelyAccessibleAndGetId(g)
          BoundFutureReplacement(id)
        }
      }
    }
    case _ => b
  }) match {
    case br: BindingReplacement => br
    case BoundValue(ro: RemoteObjectRef) => {
      BoundRemoteReplacement(ro.remoteRefId)
    }
    //FIXME: Make copy versus reference decision here
    case BoundValue(v) if (!v.isInstanceOf[java.io.Serializable]) => {
      val id = execution.remoteIdForObject(v)
      BoundRemoteReplacement(id)
    }
    case b: Binding with Serializable => {
      val br = SerializableBindingReplacement(b)
      b match {
        case BoundValue(mn: DOrcMarshallingNotifications) => mn.marshalled()
        case _ => {/* Nothing to do */}
      }
      br
    }
  }

  protected def marshalFrame(execution: DOrcExecution, ast: Expression)(f: Frame): FrameReplacement = {
    f match {
      case BindingFrame(n, previous) => BindingFrameReplacement(n)
      case SequenceFrame(node, previous) => SequenceFrameReplacement(AstNodeIndexing.nodeIndexInTree(node, ast).get)
      case FunctionFrame(callpoint, env, previous) => FunctionFrameReplacement(AstNodeIndexing.nodeIndexInTree(callpoint, ast).get, (env map marshalBinding(execution)).toArray)
      case FutureFrame(k, previous) => ??? //FIXME:Need to refactor FutureFrame to eliminate closures
      case GroupFrame(previous) => GroupFrameReplacement
    }
  }

  val astNodeIndex: Seq[Int] = AstNodeIndexing.nodeIndexInTree(token.getNode, astRoot).get

  /* N.B.: The stack's EmptyFrame will not appear in the array, because of Frame's iterator behavior */
  private def extractStack(t: Token, astRoot: Expression) = (t.getStack.toArray.reverse) map marshalFrame((t.getGroup.execution.asInstanceOf[DOrcExecution]), astRoot)

  val stack = extractStack(token, astRoot)
  val env = token.getEnv.toArray map marshalBinding(token.getGroup.execution.asInstanceOf[DOrcExecution])

  //val clock: VirtualClock = t.getClock
  //val state: TokenState =
  //assert(t.state == Live)

  def asToken(origin: Location, astRoot: Expression, newGroup: Group) = {
    val dorcExecution = newGroup.execution.asInstanceOf[DOrcExecution]
    val _node = AstNodeIndexing.lookupNodeInTree(astRoot, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.unmarshalFrame(dorcExecution, origin, stackTop, astRoot) }

    new MigratedToken(_node, _stack, env.toList map { _.unmarshalBinding(dorcExecution, origin) }, newGroup /*, clock, state*/ )
  }

  def asPublishingToken(origin: Location, astRoot: Expression, newGroup: Group, v: Option[AnyRef]) = {
    val dorcExecution = newGroup.execution.asInstanceOf[DOrcExecution]
    val _node = AstNodeIndexing.lookupNodeInTree(astRoot, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.unmarshalFrame(dorcExecution, origin, stackTop, astRoot) }

    //FIXME: Hack: push a GroupFrame to compensate for the one consumed incorrectly by publishing through the GroupProxy
    new MigratedToken(_node, GroupFrame(_stack), env.toList map { _.unmarshalBinding(dorcExecution, origin) }, newGroup, None /*clock*/ , Publishing(v))
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

/** Replacement for a Binding for use in serialization.
  *
  * @author jthywiss
  */
protected abstract class BindingReplacement() {
  def unmarshalBinding(execution: DOrcExecution, origin: Location): Binding
}

protected case class SerializableBindingReplacement(b: Binding with Serializable) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: Location) = {
    b match {
      case BoundValue(mn: DOrcMarshallingNotifications) => mn.unmarshalled()
      case _ => {/* Nothing to do */}
    }
    b
  }
}

protected case class BoundRemoteReplacement(remoteRemoteRefId: RemoteObjectRef#RemoteRefId) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: Location) = {
    execution.localObjectForRemoteId(remoteRemoteRefId) match {
      case Some(v) => BoundValue(v)
      case None =>
        Logger.finest(s"Binding placeholder for remote object $remoteRemoteRefId")
        BoundValue(new RemoteObjectRef(remoteRemoteRefId))
    }
  }
}

protected case class BoundFutureReplacement(bindingId: RemoteFutureRef#RemoteRefId) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: Location) = {
    BoundFuture(execution.futureForId(bindingId))
  }
}

/** Replacement for a Frame for use in serialization.
  *
  * @author jthywiss
  */
protected abstract class FrameReplacement() {
  def unmarshalFrame(execution: DOrcExecution, origin: Location, previous: Frame, ast: Expression): Frame
}
protected case object EmptyFrameReplacement extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: Location, previous: Frame, ast: Expression) = throw new AssertionError("EmptyFrameReplacement.asFrame called")
}
protected case class BindingFrameReplacement(n: Int) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: Location, previous: Frame, ast: Expression) = BindingFrame(n, previous)
}
protected case class SequenceFrameReplacement(nodeAddr: Seq[Int]) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: Location, previous: Frame, ast: Expression) = SequenceFrame(AstNodeIndexing.lookupNodeInTree(ast, nodeAddr).asInstanceOf[Expression], previous)
}
protected case class FunctionFrameReplacement(callpointAddr: Seq[Int], env: Array[BindingReplacement]) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: Location, previous: Frame, ast: Expression) = FunctionFrame(AstNodeIndexing.lookupNodeInTree(ast, callpointAddr).asInstanceOf[Expression], env.toList map { _.unmarshalBinding(execution, origin) }, previous)
}
//  protected case class FutureFrameReplacement(k: (Option[AnyRef] => Unit)) extends FrameReplacement() {
//    def unmarshalFrame(execution: DOrcExecution, previous: Frame, ast: Expression) = FutureFrame(k, previous)
//  }
protected case object GroupFrameReplacement extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: Location, previous: Frame, ast: Expression) = GroupFrame(previous)
}
