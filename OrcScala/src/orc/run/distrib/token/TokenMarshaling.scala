//
// TokenMarshaling.scala -- Scala class TokenReplacement and its auxiliary classes
// Project OrcScala
//
// Created by jthywiss on Dec 26, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.token

import orc.ast.AST
import orc.ast.oil.nameless.{ Def, Expression }
import orc.run.core.{ Binding, BindingFrame, BoundReadable, BoundStop, BoundValue, Closure, ClosureGroup, EmptyFrame, Frame, FunctionFrame, Future, FutureFrame, Group, GroupFrame, LocalFuture, SequenceFrame, Token, TokenState, VirtualClock }

/** Replacement for a Token for use in serialization.
  *
  * @author jthywiss
  */
abstract class TokenReplacementBase(token: Token, val tokenProxyId: DOrcExecution#GroupProxyId, destination: PeerLocation) extends Serializable {

  /* Token fields:
   *   node: orc.ast.oil.nameless.Expression -- replaced by Seq[Int] indication position of AST node in tree
   *   stack: orc.run.core.Frame -- replaced by Array[FrameReplacement]
   *   env: scala.collection.immutable.List[orc.run.core.Binding] -- replaced by Array[BindingReplacement]
   *   group: orc.run.core.Group -- Supplied during deserialization by remote group proxy mechanism
   *   clock: scala.Option[orc.run.core.VirtualClock] -- TODO
   *   state: orc.run.core.TokenState -- Either Live or Publishing, encoded in TokenReplacement subtype
   *   debugId: Long -- Serializable
   *   functionFramesPushed: Int -- TODO
   *   nonblocking: Boolean -- Always true
   *   toStringRecusionGuard: java.lang.ThreadLocal[java.lang.Object] -- Ignored -- Only used during execution of toString
   */

  override def toString = super.toString + f"(debugId=$debugId%#x,tokenProxyId=$tokenProxyId%#x, astNodeIndex=${astNodeIndex.mkString(".")}, stackTop=${stack.headOption.getOrElse("")}, env=[${env.mkString("|")}])"

  val astNodeIndex: Seq[Int] = AstNodeIndexing.nodeIndexInTree(token.getNode, token.execution.node).get

  /* N.B.: The stack's EmptyFrame will not appear in the array, because of Frame's iterator behavior */
  private def extractStack(t: Token, destination: PeerLocation) = (t.getStack.toArray.reverse) map TokenFieldMarshaling.marshalFrame((t.getGroup.execution.asInstanceOf[DOrcExecution]), destination)

  val stack = extractStack(token, destination)

  private def extractEnv(t: Token, destination: PeerLocation) = t.getEnv.toArray map TokenFieldMarshaling.marshalBinding(t.getGroup.execution.asInstanceOf[DOrcExecution], destination)

  val env = extractEnv(token, destination)

  //val clock: VirtualClock = t.getClock
  //val state: TokenState =
  //assert(t.state == Live)

  val debugId: Long = token.debugId
}


@SerialVersionUID(-655352528693128511L)
class TokenReplacement(token: Token, tokenProxyId: DOrcExecution#GroupProxyId, destination: PeerLocation) extends TokenReplacementBase(token, tokenProxyId, destination) {

  def asToken(origin: PeerLocation, newGroup: Group) = {
    val dorcExecution = newGroup.execution.asInstanceOf[DOrcExecution]
    val _node = AstNodeIndexing.lookupNodeInTree(newGroup.execution.node, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.unmarshalFrame(dorcExecution, origin, stackTop) }

    new MigratedToken(_node, _stack, env.toList map { _.unmarshalBinding(dorcExecution, origin) }, newGroup, None /* clock */, TokenState.Live, debugId)
  }

}


@SerialVersionUID(7131096891671375273L)
class PublishingTokenReplacement(token: Token, tokenProxyId: DOrcExecution#GroupProxyId, destination: PeerLocation, publishingValue: Option[AnyRef]) extends TokenReplacementBase(token, tokenProxyId, destination) {

  override def toString = super.toString.stripSuffix(")") + s", pubValue=$pubValue)"

  val pubValue = TokenFieldMarshaling.marshalPublishingValue(token.getGroup.execution.asInstanceOf[DOrcExecution], destination)(publishingValue)

  def asPublishingToken(origin: PeerLocation, newGroup: Group) = {
    val dorcExecution = newGroup.execution.asInstanceOf[DOrcExecution]
    val _node = AstNodeIndexing.lookupNodeInTree(newGroup.execution.node, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.unmarshalFrame(dorcExecution, origin, stackTop) }
    val _v = pubValue.map(dorcExecution.unmarshalValue(_))

    //FIXME: Hack: push a GroupFrame to compensate for the one consumed incorrectly by publishing through the GroupProxy
    new MigratedToken(_node, GroupFrame(_stack), env.toList map { _.unmarshalBinding(dorcExecution, origin) }, newGroup, None /*clock*/ , TokenState.Publishing(_v), debugId)
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
  _clock: Option[VirtualClock],
  _state: TokenState,
  _debugId: Long)
  extends Token(_node, _stack, _env, _group, _clock, _state, _debugId) {
}


object TokenFieldMarshaling {

  def marshalPublishingValue(execution: DOrcExecution, destination: PeerLocation)(pv: Option[AnyRef]): Option[AnyRef] = {
    pv.map(execution.marshalValue(destination)(_))
  }

  def marshalBinding(execution: DOrcExecution, destination: PeerLocation)(b: Binding): BindingReplacement = {
    //Logger.entering(getClass.getName, "marshalBinding", Seq(b))
    val result = (b match {
      /* Optimization: Treat resolved local futures as just values */
      case BoundReadable(lfut: LocalFuture) => {
        lfut.readIfResolved() match {
          case Some(None) => BoundStop
          case Some(Some(v)) => BoundValue(v)
          case None => b
        }
      }
      case _ => b
    }) match {
      case BoundReadable(fut: Future) => {
        val id = execution.ensureFutureIsRemotelyAccessibleAndGetId(fut)
        BoundFutureReplacement(id)
      }
      case BoundReadable(c: Closure) => {
        /* Closures and ClosureGroups are marshaled (replaced) during serialization */
        BoundClosureReplacement(c)
      }
      case BoundReadable(rb) => {
        throw new AssertionError(s"Cannot marshal: BoundReadable bound to a unrecognized type: ${rb.getClass.getName}: ${rb}")
      }
      case BoundStop => {
        BoundStopReplacement
      }
      //FIXME: Make MigrationDecision = Copy/Move/Remote choice here
      case BoundValue(v) => {
        BoundValueReplacement(execution.marshalValue(destination)(v))
      }
    }
    //Logger.exiting(getClass.getName, "marshalBinding", result)
    result
  }

  def marshalFrame(execution: DOrcExecution, destination: PeerLocation)(f: Frame): FrameReplacement = {
    f match {
      case BindingFrame(n, previous) => BindingFrameReplacement(n)
      case SequenceFrame(node, previous) => SequenceFrameReplacement(AstNodeIndexing.nodeIndexInTree(node, execution.node).get)
      case FunctionFrame(callpoint, env, previous) => FunctionFrameReplacement(AstNodeIndexing.nodeIndexInTree(callpoint, execution.node).get, (env map marshalBinding(execution, destination)).toArray)
      case FutureFrame(k, previous) => ??? //FIXME:Need to refactor FutureFrame to eliminate closures, but only if we are going to marshal non-site calls (i.e. non-strict calls)
      case GroupFrame(previous) => GroupFrameReplacement
      case EmptyFrame => throw new AssertionError("EmptyFrame should not appear during stack iteration")
    }
  }

  def marshalClosureGroup(cg: ClosureGroup, execution: DOrcExecution, destination: PeerLocation) = {
    assert(cg.isResolved, "Closure group must be resolved")

    //Logger.fine("Creating new CGR for " + cg + ": " + (cg.definitions map { _.optionalVariableName.getOrElse("") }).mkString(","))
    val defNodesIndicies = cg.definitions map { AstNodeIndexing.nodeIndexInTree(_, execution.node).get }
    val lexicalContext = (cg.lexicalContext map marshalBinding(execution, destination)).toArray
    val cgr = new ClosureGroupReplacement(defNodesIndicies, lexicalContext)
    cgr
  }

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
protected abstract sealed class BindingReplacement() {
  def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation): Binding
}

protected final case class BoundValueReplacement(boundValue: AnyRef) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation) = {
    BoundValue(execution.unmarshalValue(boundValue))
  }
}

protected final case class BoundFutureReplacement(bindingId: RemoteFutureRef#RemoteRefId) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation) = {
    BoundReadable(execution.futureForId(bindingId))
  }
}

protected final case class BoundClosureReplacement(c: Closure) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation) = {
    /* Closures and ClosureGroups are unmarshaled (resolved) during deserialization */
    BoundValue(c)
  }
}

protected final case object BoundStopReplacement extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation) = {
    BoundStop
  }
}


/** Replacement for a ClosureGroup for use in serialization.
  *
  * @author jthywiss
  */
protected final case class ClosureGroupReplacement(defNodesIndicies: List[Seq[Int]], env: Array[BindingReplacement]) {
  def unmarshalClosureGroup(execution: DOrcExecution, origin: PeerLocation) = synchronized {
    val defs = (defNodesIndicies map { AstNodeIndexing.lookupNodeInTree(execution.node, _) }).asInstanceOf[List[Def]]
    val lexicalContext = env.toList map { _.unmarshalBinding(execution, origin) }
    // FIXME: The None clock is almost certainly wrong!!! However, I think it will no longer be needed since closures will never need to migrate before the are fully resolved.
    val cg = new ClosureGroup(defs, lexicalContext, execution.runtime, None)
    execution.runtime.schedule(cg)
    cg
  }
}


/** Replacement for a Frame for use in serialization.
  *
  * @author jthywiss
  */
protected abstract sealed class FrameReplacement() {
  def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame): Frame
}
protected final case object EmptyFrameReplacement extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame) = throw new AssertionError("EmptyFrameReplacement.asFrame called")
}
protected final case class BindingFrameReplacement(n: Int) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame) = BindingFrame(n, previous)
}
protected final case class SequenceFrameReplacement(nodeAddr: Seq[Int]) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame) = SequenceFrame(AstNodeIndexing.lookupNodeInTree(execution.node, nodeAddr).asInstanceOf[Expression], previous)
}
protected final case class FunctionFrameReplacement(callpointAddr: Seq[Int], env: Array[BindingReplacement]) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame) = FunctionFrame(AstNodeIndexing.lookupNodeInTree(execution.node, callpointAddr).asInstanceOf[Expression], env.toList map { _.unmarshalBinding(execution, origin) }, previous)
}
//  protected final case class FutureFrameReplacement(k: (Option[AnyRef] => Unit)) extends FrameReplacement() {
//    def unmarshalFrame(execution: DOrcExecution, previous: Frame) = FutureFrame(k, previous)
//  }
protected final case object GroupFrameReplacement extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame) = GroupFrame(previous)
}
