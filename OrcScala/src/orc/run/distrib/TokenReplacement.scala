//
// TokenReplacement.scala -- Scala class TokenReplacement and its auxiliary classes
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

package orc.run.distrib

import orc.ast.AST
import orc.ast.oil.nameless.{ Def, Expression }
import orc.run.core.{ Binding, BindingFrame, BoundReadable, BoundStop, BoundValue, Closure, ClosureGroup, EmptyFrame, Frame, FunctionFrame, Future, FutureFrame, Group, GroupFrame, Live, LocalFuture, Publishing, SequenceFrame, Token, TokenState, VirtualClock }

/** Replacement for a Token for use in serialization.
  *
  * @author jthywiss
  */
abstract class TokenReplacementBase(token: Token, astRoot: Expression, val tokenProxyId: DOrcExecution#GroupProxyId, destination: PeerLocation) extends Serializable {

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

  protected def marshalPublishingValue(execution: DOrcExecution, destination: PeerLocation)(pv: Option[AnyRef]): Option[AnyRef with java.io.Serializable] = {
    pv.map(execution.marshalValue(destination)(_))
  }

  protected def marshalBinding(execution: DOrcExecution, ast: Expression, destination: PeerLocation)(b: Binding): BindingReplacement = {
    (b match {
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
        val cgr = marshalClosureGroup(c.closureGroup, execution, ast, destination)
        BoundClosureReplacement(c.index, cgr)
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
  }

  protected def marshalFrame(execution: DOrcExecution, ast: Expression, destination: PeerLocation)(f: Frame): FrameReplacement = {
    f match {
      case BindingFrame(n, previous) => BindingFrameReplacement(n)
      case SequenceFrame(node, previous) => SequenceFrameReplacement(AstNodeIndexing.nodeIndexInTree(node, ast).get)
      case FunctionFrame(callpoint, env, previous) => FunctionFrameReplacement(AstNodeIndexing.nodeIndexInTree(callpoint, ast).get, (env map marshalBinding(execution, ast, destination)).toArray)
      case FutureFrame(k, previous) => ??? //FIXME:Need to refactor FutureFrame to eliminate closures, but only if we are going to marshal non-site calls (i.e. non-strict calls)
      case GroupFrame(previous) => GroupFrameReplacement
      case EmptyFrame => throw new AssertionError("EmptyFrame should not appear during stack iteration")
    }
  }

  protected def marshalClosureGroup(cg: ClosureGroup, execution: DOrcExecution, ast: Expression, destination: PeerLocation) = {

    //FIXME: Broken: Requires closures' enclose operation to be complete.
    //assert(cg.isResolved, "Closure group must be resolved")
    while (!cg.isResolved) {
      Logger.info("Awaiting resolution of "+cg)
      Thread.sleep(10);
    }

    ClosureGroupReplacement.closureGroupReplacements synchronized {
      ClosureGroupReplacement.closureGroupReplacements.get(cg) match {
        case null => {
          Logger.fine("Creating new CGR for " + cg + ": " + (cg.definitions map { _.optionalVariableName.getOrElse("") }).mkString(","))
          val defNodesIndicies = cg.definitions map { AstNodeIndexing.nodeIndexInTree(_, ast).get }
          val lexicalContext = (cg.lexicalContext map marshalBinding(execution, ast, destination)).toArray
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
  private def extractStack(t: Token, astRoot: Expression, destination: PeerLocation) = (t.getStack.toArray.reverse) map marshalFrame((t.getGroup.execution.asInstanceOf[DOrcExecution]), astRoot, destination)

  val stack = extractStack(token, astRoot, destination)

  private def extractEnv(t: Token, astRoot: Expression, destination: PeerLocation) = t.getEnv.toArray map marshalBinding(t.getGroup.execution.asInstanceOf[DOrcExecution], astRoot, destination)

  val env = extractEnv(token, astRoot, destination)

  //val clock: VirtualClock = t.getClock
  //val state: TokenState =
  //assert(t.state == Live)

  val debugId: Long = token.debugId
}

@SerialVersionUID(-655352528693128511L)
class TokenReplacement(token: Token, astRoot: Expression, tokenProxyId: DOrcExecution#GroupProxyId, destination: PeerLocation) extends TokenReplacementBase(token, astRoot, tokenProxyId, destination) {

  def asToken(origin: PeerLocation, astRoot: Expression, newGroup: Group) = {
    val dorcExecution = newGroup.execution.asInstanceOf[DOrcExecution]
    val _node = AstNodeIndexing.lookupNodeInTree(astRoot, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.unmarshalFrame(dorcExecution, origin, stackTop, astRoot) }

    new MigratedToken(_node, _stack, env.toList map { _.unmarshalBinding(dorcExecution, origin, astRoot) }, newGroup, None /* clock */, Live, debugId)
  }

}

@SerialVersionUID(7131096891671375273L)
class PublishingTokenReplacement(token: Token, astRoot: Expression, tokenProxyId: DOrcExecution#GroupProxyId, destination: PeerLocation, publishingValue: Option[AnyRef]) extends TokenReplacementBase(token, astRoot, tokenProxyId, destination) {

  override def toString = super.toString.stripSuffix(")") + s", pubValue=$pubValue)"

  val pubValue = marshalPublishingValue(token.getGroup.execution.asInstanceOf[DOrcExecution], destination)(publishingValue)

  def asPublishingToken(origin: PeerLocation, astRoot: Expression, newGroup: Group) = {
    val dorcExecution = newGroup.execution.asInstanceOf[DOrcExecution]
    val _node = AstNodeIndexing.lookupNodeInTree(astRoot, astNodeIndex).asInstanceOf[Expression]
    val _stack = stack.foldLeft[Frame](EmptyFrame) { (stackTop, addFrame) => addFrame.unmarshalFrame(dorcExecution, origin, stackTop, astRoot) }
    val _v = pubValue.map(dorcExecution.unmarshalValue(_))

    //FIXME: Hack: push a GroupFrame to compensate for the one consumed incorrectly by publishing through the GroupProxy
    new MigratedToken(_node, GroupFrame(_stack), env.toList map { _.unmarshalBinding(dorcExecution, origin, astRoot) }, newGroup, None /*clock*/ , Publishing(_v), debugId)
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
  def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression): Binding
}

protected final case class BoundValueReplacement(boundValue: AnyRef with java.io.Serializable) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression) = {
    BoundValue(execution.unmarshalValue(boundValue))
  }
}

protected final case class BoundFutureReplacement(bindingId: RemoteFutureRef#RemoteRefId) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression) = {
    BoundReadable(execution.futureForId(bindingId))
  }
}

protected final case class BoundClosureReplacement(index: Int, closureGroupReplacement: ClosureGroupReplacement) extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression) = {
    val cg = closureGroupReplacement.unmarshalClosureGroup(execution, origin, astRoot)
    BoundReadable(new Closure(index, cg))
  }
}

protected final case object BoundStopReplacement extends BindingReplacement() {
  override def unmarshalBinding(execution: DOrcExecution, origin: PeerLocation, astRoot: Expression) = {
    BoundStop
  }
}

protected final case class ClosureGroupReplacement(defNodesIndicies: List[Seq[Int]], env: Array[BindingReplacement]) {
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

protected final object ClosureGroupReplacement {
  val closureGroupReplacements = new java.util.concurrent.ConcurrentHashMap[ClosureGroup, ClosureGroupReplacement]()
}

/** Replacement for a Frame for use in serialization.
  *
  * @author jthywiss
  */
protected abstract sealed class FrameReplacement() {
  def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression): Frame
}
protected final case object EmptyFrameReplacement extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = throw new AssertionError("EmptyFrameReplacement.asFrame called")
}
protected final case class BindingFrameReplacement(n: Int) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = BindingFrame(n, previous)
}
protected final case class SequenceFrameReplacement(nodeAddr: Seq[Int]) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = SequenceFrame(AstNodeIndexing.lookupNodeInTree(astRoot, nodeAddr).asInstanceOf[Expression], previous)
}
protected final case class FunctionFrameReplacement(callpointAddr: Seq[Int], env: Array[BindingReplacement]) extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = FunctionFrame(AstNodeIndexing.lookupNodeInTree(astRoot, callpointAddr).asInstanceOf[Expression], env.toList map { _.unmarshalBinding(execution, origin, astRoot) }, previous)
}
//  protected final case class FutureFrameReplacement(k: (Option[AnyRef] => Unit)) extends FrameReplacement() {
//    def unmarshalFrame(execution: DOrcExecution, previous: Frame, astRoot: Expression) = FutureFrame(k, previous)
//  }
protected final case object GroupFrameReplacement extends FrameReplacement() {
  override def unmarshalFrame(execution: DOrcExecution, origin: PeerLocation, previous: Frame, astRoot: Expression) = GroupFrame(previous)
}
