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
import orc.ast.oil.nameless.{ Def, Expression }
import orc.run.core.{ Binding, BindingFrame, BoundClosure, BoundFuture, BoundStop, BoundValue, Closure, ClosureGroup, EmptyFrame, Frame, FunctionFrame, FutureFrame, Group, GroupFrame, Live, Publishing, RightSidePublished, RightSideSilent, RightSideUnknown, SequenceFrame, Token, TokenState, VirtualClock }

/** Replacement for a Token for use in serialization.
  *
  * @author jthywiss
  */
@SerialVersionUID(-655352528693128511L)
class TokenReplacement(token: Token, astRoot: Expression, val tokenProxyId: DOrcExecution#GroupProxyId) extends Serializable {

  // Logger.fine("TokenReplacement fields: " + getClass.getDeclaredFields.mkString("; "))

  protected def marshalBinding(execution: DOrcExecution, ast: Expression)(b: Binding): BindingReplacement = (b match {
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
    case BoundClosure(c: Closure) => {
      val cgr = marshalClosureGroup(c.closureGroup, execution, ast)
      BoundClosureReplacement(c.index, cgr)
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
      case FunctionFrame(callpoint, env, previous) => FunctionFrameReplacement(AstNodeIndexing.nodeIndexInTree(callpoint, ast).get, (env map marshalBinding(execution, ast)).toArray)
      case FutureFrame(k, previous) => ??? //FIXME:Need to refactor FutureFrame to eliminate closures, but only if we are going to marshall non-site calls (i.e. non-strict calls)
      case GroupFrame(previous) => GroupFrameReplacement
    }
  }

  protected def marshalClosureGroup(cg: ClosureGroup, execution: DOrcExecution, ast: Expression) = {

    //FIXME: Broken: Requires closures' enclose operation to be complete.
    assert (cg.isResolved, "Closure group must be resolved")

    ClosureGroupReplacement.closureGroupReplacements synchronized {
      ClosureGroupReplacement.closureGroupReplacements.get(cg) match {
        case null => {
          Logger.fine("Creating new CGR for "+cg+": "+(cg.defs map {_.optionalVariableName.getOrElse("")}).mkString(","))
          val defNodesIndicies = cg.defs map { AstNodeIndexing.nodeIndexInTree(_, ast).get }
          val lexicalContext = (cg.lexicalContext map marshalBinding(execution, ast)).toArray
              val cgr = new ClosureGroupReplacement(defNodesIndicies, lexicalContext)
          ClosureGroupReplacement.closureGroupReplacements.put(cg, cgr)
          cgr
        }
        case cgr => cgr
      }
    }
  }

  val astNodeIndex: Seq[Int] = AstNodeIndexing.nodeIndexInTree(token.getNode, astRoot).get

  /* N.B.: The stack's EmptyFrame will not appear in the array, because of Frame's iterator behavior */
  private def extractStack(t: Token, astRoot: Expression) = (t.getStack.toArray.reverse) map marshalFrame((t.getGroup.execution.asInstanceOf[DOrcExecution]), astRoot)

  val stack = extractStack(token, astRoot)

  private def extractEnv(t: Token, astRoot: Expression) = t.getEnv.toArray map marshalBinding(t.getGroup.execution.asInstanceOf[DOrcExecution], astRoot)

  val env = extractEnv(token, astRoot)

  Logger.fine("TokenReplacement.env=" + env.mkString("|"))

  //val clock: VirtualClock = t.getClock
  //val state: TokenState =
  //assert(t.state == Live)

  def asToken(origin: PeerLocation, astRoot: Expression, newGroup: Group) = {
    val dorcExecution = newGroup.execution.asInstanceOf[DOrcExecution]
    val _node = AstNodeIndexing.lookupNodeInTree(astRoot, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.unmarshalFrame(dorcExecution, origin, stackTop, astRoot) }

    new MigratedToken(_node, _stack, env.toList map { _.unmarshalBinding(dorcExecution, origin, astRoot) }, newGroup /*, clock, state*/ )
  }

  def asPublishingToken(origin: PeerLocation, astRoot: Expression, newGroup: Group, v: Option[AnyRef]) = {
    val dorcExecution = newGroup.execution.asInstanceOf[DOrcExecution]
    val _node = AstNodeIndexing.lookupNodeInTree(astRoot, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.unmarshalFrame(dorcExecution, origin, stackTop, astRoot) }

    //FIXME: Hack: push a GroupFrame to compensate for the one consumed incorrectly by publishing through the GroupProxy
    new MigratedToken(_node, GroupFrame(_stack), env.toList map { _.unmarshalBinding(dorcExecution, origin, astRoot) }, newGroup, None /*clock*/ , Publishing(v))
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
  def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression): Binding
}

protected case class SerializableBindingReplacement(b: Binding with Serializable) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression) = {
    b match {
      case BoundValue(mn: DOrcMarshallingNotifications) => mn.unmarshalled()
      case _ => {/* Nothing to do */}
    }
    b
  }
}

protected case class BoundRemoteReplacement(remoteRemoteRefId: RemoteObjectRef#RemoteRefId) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression) = {
    execution.localObjectForRemoteId(remoteRemoteRefId) match {
      case Some(v) => BoundValue(v)
      case None =>
        Logger.finest(s"Binding placeholder for remote object $remoteRemoteRefId")
        BoundValue(new RemoteObjectRef(remoteRemoteRefId))
    }
  }
}

protected case class BoundFutureReplacement(bindingId: RemoteFutureRef#RemoteRefId) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression) = {
    BoundFuture(execution.futureForId(bindingId))
  }
}

protected case class BoundClosureReplacement(index: Int, closureGroupReplacement: ClosureGroupReplacement) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression) = {
    val cg = closureGroupReplacement.unmarshalClosureGroup(execution, origin, astRoot)
    BoundClosure(new Closure(index, cg))
  }
}

protected case class ClosureGroupReplacement(defNodesIndicies: List[Seq[Int]], env: Array[BindingReplacement]) {
  @transient var cg: ClosureGroup = null
  def unmarshalClosureGroup(execution: DOrcExecution, origin: PeerLocation, ast: Expression) = synchronized {
    if (cg == null) {
      val defs = (defNodesIndicies map { AstNodeIndexing.lookupNodeInTree(ast, _) }).asInstanceOf[List[Def]]
      val lexicalContext = env.toList map { _.unmarshalBinding(execution, origin, ast) }
      cg = new ClosureGroup(defs, lexicalContext, execution.runtime)
      execution.runtime.schedule(cg)
    }
    cg
  }
}

protected object ClosureGroupReplacement {
  val closureGroupReplacements = new java.util.concurrent.ConcurrentHashMap[ClosureGroup, ClosureGroupReplacement]()
}

/** Replacement for a Frame for use in serialization.
  *
  * @author jthywiss
  */
protected abstract class FrameReplacement() {
  def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression): Frame
}
protected case object EmptyFrameReplacement extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = throw new AssertionError("EmptyFrameReplacement.asFrame called")
}
protected case class BindingFrameReplacement(n: Int) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = BindingFrame(n, previous)
}
protected case class SequenceFrameReplacement(nodeAddr: Seq[Int]) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = SequenceFrame(AstNodeIndexing.lookupNodeInTree(astRoot, nodeAddr).asInstanceOf[Expression], previous)
}
protected case class FunctionFrameReplacement(callpointAddr: Seq[Int], env: Array[BindingReplacement]) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = FunctionFrame(AstNodeIndexing.lookupNodeInTree(astRoot, callpointAddr).asInstanceOf[Expression], env.toList map { _.unmarshalBinding(execution, origin, astRoot) }, previous)
}
//  protected case class FutureFrameReplacement(k: (Option[AnyRef] => Unit)) extends FrameReplacement() {
//    def unmarshalFrame(execution: DOrcExecution, previous: Frame, astRoot: Expression) = FutureFrame(k, previous)
//  }
protected case object GroupFrameReplacement extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = GroupFrame(previous)
}
