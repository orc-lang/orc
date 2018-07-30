//
// PorcToPorcE.scala -- Scala class and object PorcToPorcE
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compiler.porce

import scala.collection.mutable

import com.oracle.truffle.api.{ RootCallTarget, Truffle }
import com.oracle.truffle.api.frame.{ FrameDescriptor, FrameSlot, FrameSlotKind }

import orc.ast.{ ASTWithIndex, porc }
import orc.run.porce
import orc.run.porce.runtime.{ PorcEClosure, PorcEExecution, PorcERuntime }
import orc.util.{ TFalse, TTrue, TUnknown }
import orc.values.Field
import swivel.Zipper
import orc.run.porce.PorcELanguage
import orc.run.porce.runtime.NoInvocationInterception
import scala.annotation.varargs

object PorcToPorcE {
  def method(m: porc.MethodCPS,
      execution: PorcEExecution, language: PorcELanguage = null): (PorcEClosure, collection.Map[Int, RootCallTarget]) = {
    val converter = new PorcToPorcE(!execution.isInstanceOf[NoInvocationInterception], language)
    converter(m, execution)
  }

  /** Convert an expression in a provided context with potential substitutions.
    *
    * This is used for inlining.
    *
    */
  def expression(e: porc.Expression,
      argumentVariables: Seq[porc.Variable], closureVariables: Seq[porc.Variable], frameDescriptor: FrameDescriptor,
      closureMap: collection.Map[Int, RootCallTarget], substs: collection.Map[porc.Variable, porc.Variable],
      execution: PorcEExecution, language: PorcELanguage = null, isTail: Boolean = true): porce.Expression = {
    val converter = new PorcToPorcE(!execution.isInstanceOf[NoInvocationInterception], language)
    converter(e.toZipper(), execution, argumentVariables, closureVariables, frameDescriptor, closureMap, substs, isTail)
  }

  @varargs
  def variableSeq(vars: porc.Variable*): Seq[porc.Variable] = {
    vars
  }
}

class PorcToPorcE(val usingInvokationInterceptor: Boolean, val language: PorcELanguage) {
  case class Context(
      descriptor: FrameDescriptor, execution: PorcEExecution,
      argumentVariables: Seq[porc.Variable], closureVariables: Seq[porc.Variable],
      variableSubstitutions: collection.Map[porc.Variable, porc.Variable],
      runtime: PorcERuntime, inTailPosition: Boolean) {
    def withTailPosition = copy(inTailPosition = true)
    def withoutTailPosition = copy(inTailPosition = false)
  }

  implicit class AddContextMap[T](val underlying: Seq[T]) {
    def contextMap[B](f: (T, Context) => B)(implicit ctx: Context): Seq[B] = {
      underlying.init.map(f(_, ctx.withoutTailPosition)) :+ f(underlying.last, ctx)
    }
  }

  private def getEnclosingMethod(e: porc.PorcAST.Z): porc.Variable = {
    e.parents.collectFirst({ case porc.Method.Z(n, _, _, _) => n }).getOrElse {
      throw new IllegalArgumentException(s"Could not find an enclosing method for $e. ")
    }
  }

  object LocalVariables {
    val MethodGroupClosure = new porc.Variable(Some("MethodGroupClosure"))
    val Join = new porc.Variable(Some("Join"))
    val Resolve = new porc.Variable(Some("Resolve"))
  }

  private def lookupVariable(x: porc.Variable)(implicit ctx: Context) =
    ctx.descriptor.findOrAddFrameSlot(x, FrameSlotKind.Object)

  private def normalizeOrder(s: TraversableOnce[porc.Variable]) = {
    s.toSeq.sortBy(_.optionalName)
  }

  val fieldOrderCache = mutable.HashMap[Set[Field], Seq[Field]]()

  private def normalizeFieldOrder(s: Iterable[Field]) = {
    // TODO: PERFORMANCE: If we could somehow combine the field lists for classes in the same heirachy (or simply used in the same places) we could avoid the need for as many PICs.
    fieldOrderCache.getOrElseUpdate(s.toSet, s.toSeq.sortBy(_.name))
  }

  //private def normalizeOrderAndLookup(s: TraversableOnce[porc.Variable])(implicit ctx: Context) = {
  //  normalizeOrder(s).map(lookupVariable(_))
  //}

  val closureMap = mutable.HashMap[Int, RootCallTarget]()

  def makeCallTarget(root: porce.PorcERootNode): RootCallTarget = {
    require(root.porcNode().isDefined, s"$root")
    require(root.porcNode().get.isInstanceOf[ASTWithIndex], s"${root.porcNode().get}")
    require(root.porcNode().get.asInstanceOf[ASTWithIndex].optionalIndex.isDefined, s"${root.porcNode().get}")
    val callTarget = Truffle.getRuntime().createCallTarget(root)
    closureMap += (root.getId() -> callTarget)
    callTarget
  }

  def transform(inx: porc.Variable)(implicit ctx: Context): porce.Expression = {
    val x = ctx.variableSubstitutions.getOrElse(inx, inx)
    val res = if (ctx.argumentVariables.contains(x)) {
      porce.Read.Argument.create(ctx.argumentVariables.indexOf(x))
    } else if (ctx.closureVariables.contains(x)) {
      porce.Read.Closure.create(ctx.closureVariables.indexOf(x))
    } else {
      porce.Read.Local.create(lookupVariable(x))
    }
    res.setPorcAST(x)
    res
  }

  def apply(m: porc.MethodCPS, execution: PorcEExecution): (PorcEClosure, collection.Map[Int, RootCallTarget]) = {
    val descriptor = new FrameDescriptor()
    val newBody = apply(m.toZipper().body,
      descriptor = descriptor,
      execution = execution,
      argumentVariables = Seq(m.pArg, m.cArg, m.tArg),
      closureVariables = Seq(),
      closureMap = Map(),
      substs = Map(),
      isTail = true)
    val rootNode = porce.PorcERootNode.create(language, descriptor, newBody, 3, 0, m.name, execution)
    rootNode.setPorcAST(m)
    val callTarget = makeCallTarget(rootNode)
    val closure = new PorcEClosure(Array(), callTarget, true)
    (closure, closureMap)
  }

  def apply(e: porc.Expression.Z, execution: PorcEExecution,
      argumentVariables: Seq[porc.Variable], closureVariables: Seq[porc.Variable], descriptor: FrameDescriptor,
      closureMap: collection.Map[Int, RootCallTarget], substs: collection.Map[porc.Variable, porc.Variable],
      isTail: Boolean): porce.Expression = {
    this.closureMap ++= closureMap
    implicit val ctx = Context(
      descriptor = descriptor,
      execution = execution,
      argumentVariables = argumentVariables,
      closureVariables = closureVariables,
      variableSubstitutions = substs,
      runtime = execution.runtime,
      inTailPosition = isTail)
    val newBody = transform(e)
    newBody
  }

  def transform(e: porc.Expression.Z)(implicit ctx: Context): porce.Expression = {
    val thisCtx = ctx
    val innerCtx = ctx.withoutTailPosition
    //Logger.info(s"At ${e.value}: ${thisCtx.inTailPosition} && ${getEnclosingMethod(e)}")
    //*

    {
      implicit val ctx = thisCtx.withoutTailPosition

      val res = e match {
        case porc.Constant.Z(v) =>
          porce.Read.Constant.create(v)
        case porc.PorcUnit.Z() =>
          porce.Read.Constant.create(porce.PorcEUnit.SINGLETON)
        case Zipper(x: porc.Variable, p) =>
          transform(x)(thisCtx)
        case porc.Sequence.Z(es) =>
          porce.Sequence.create(es.contextMap(transform(_)(_))(thisCtx).toArray)
        case porc.Let.Z(x, v, body) =>
          porce.Sequence.create(Array(
            porce.Write.Local.create(lookupVariable(x), transform(v)(innerCtx)),
            transform(body)(thisCtx)))
        case e@porc.Continuation.Z(args, body) if e.value.optionalIndex.exists(closureMap.contains(_)) =>
          // TODO: Eliminate duplicated code between this case and the next.
          val reuse = {
            val capturedVars = e.freeVars
            val isSubset = capturedVars.forall(thisCtx.closureVariables.contains)
            val nDropped = thisCtx.closureVariables.count(!capturedVars.contains(_))
            //println(s"${if (isSubset) 1 else 0},$nDropped,'${thisCtx.closureVariables}','${normalizeOrder(capturedVars)}'")
            isSubset && nDropped <= 0
          }

          val capturedVars = if (reuse) thisCtx.closureVariables else normalizeOrder(e.freeVars)

          val capturingExprs = capturedVars.map(transform(_)(innerCtx)).toArray

          porce.NewContinuation.create(capturingExprs, closureMap(e.value.optionalIndex.get).getRootNode, reuse)
        case porc.Continuation.Z(args, body) =>
          val descriptor = new FrameDescriptor()

          val reuse = {
            val capturedVars = e.freeVars
            val isSubset = capturedVars.forall(thisCtx.closureVariables.contains)
            val nDropped = thisCtx.closureVariables.count(!capturedVars.contains(_))
            //println(s"${if (isSubset) 1 else 0},$nDropped,'${thisCtx.closureVariables}','${normalizeOrder(capturedVars)}'")
            isSubset && nDropped <= 0
          }

          val capturedVars = if (reuse) thisCtx.closureVariables else normalizeOrder(e.freeVars)

          val capturingExprs = capturedVars.map(transform(_)(innerCtx)).toArray

          {
            val ctx = innerCtx.withTailPosition.copy(descriptor = descriptor, closureVariables = capturedVars, argumentVariables = args)

            val newBody = transform(body)(ctx)
            //val enclosingMethod = e.parents.collectFirst({ case m: porc.Method.Z => m }).map(_.value.name).getOrElse("<MAIN>")
            val rootNode = porce.PorcERootNode.create(language, descriptor, newBody, args.size, capturedVars.size,
                getEnclosingMethod(e), ctx.execution)
            rootNode.setVariables(args, capturedVars)
            rootNode.setPorcAST(e.value)
            makeCallTarget(rootNode)
            porce.NewContinuation.create(capturingExprs, rootNode, reuse)
          }
        case porc.CallContinuation.Z(target, arguments) =>
          porce.call.CallContinuation.CPS.create(transform(target)(innerCtx), arguments.map(transform(_)(innerCtx)).toArray, ctx.execution, thisCtx.inTailPosition)
        case porc.MethodCPSCall.Z(isExt, target, p, c, t, arguments) =>
          lazy val newTarget = transform(target)(innerCtx)
          val newArguments = (p +: c +: t +: arguments).map(transform(_)(innerCtx)).toArray
          val exec = ctx.execution
          isExt match {
            case TTrue if !usingInvokationInterceptor =>
              porce.call.Call.CPS.create(newTarget, newArguments, exec, thisCtx.inTailPosition)
            case TFalse if !usingInvokationInterceptor =>
              porce.call.Call.CPS.create(newTarget, newArguments, exec, thisCtx.inTailPosition)
            case _ =>
              porce.call.Call.CPS.create(newTarget, newArguments, exec, thisCtx.inTailPosition)
          }
        case porc.MethodDirectCall.Z(isExt, target, arguments) =>
          val newTarget = transform(target)
          val newArguments = arguments.map(transform(_)(innerCtx)).toArray
          val exec = ctx.execution
          isExt match {
            case TTrue =>
              porce.call.Call.Direct.create(newTarget, newArguments, exec)
            case TFalse =>
              porce.call.Call.Direct.create(newTarget, newArguments, exec)
            case TUnknown =>
              porce.call.Call.Direct.create(newTarget, newArguments, exec)
          }
        case porc.MethodDeclaration.Z(t, methods, body) =>
          val closure = lookupVariable(LocalVariables.MethodGroupClosure)(innerCtx)

          val recCapturedVars = normalizeOrder(methods.map(_.name)).view.force
          val scopeCapturedVars = normalizeOrder(methods.flatMap(m => m.body.freeVars -- m.allArguments).toSet -- recCapturedVars)
          val allCapturedVars = scopeCapturedVars ++ recCapturedVars

          val scopeCapturedReads = scopeCapturedVars.map(transform(_)(innerCtx))

          val constructClosure = porce.Write.Local.create(
            closure,
            porce.MethodDeclaration.NewMethodClosure.create(transform(t), scopeCapturedReads.toArray, recCapturedVars.size))

          val methodsOrdered = methods.sortBy(_.name.optionalName)
          assert(methodsOrdered.map(_.name) == recCapturedVars)

          val newMethods = methodsOrdered.zipWithIndex.map({ case (m, i) => transform(m, i, closure, allCapturedVars, scopeCapturedVars.size) })

          porce.Sequence.create((constructClosure +: newMethods :+ transform(body)(thisCtx)).toArray)
        case porc.NewFuture.Z(raceFreeResolution) =>
          porce.NewFuture.create(raceFreeResolution)
        case porc.Graft.Z(p, c, t, v) =>
          porce.Graft.create(ctx.execution, transform(p), transform(c), transform(t), transform(v))
        case porc.NewSimpleCounter.Z(p, h) =>
          porce.NewCounter.Simple.create(ctx.execution, transform(p), transform(h))
        case porc.NewServiceCounter.Z(p, p2, t) =>
          porce.NewCounter.Service.create(ctx.execution, transform(p), transform(p2), transform(t))
        case porc.NewTerminatorCounter.Z(p, t) =>
          porce.NewCounter.Terminator.create(ctx.execution, transform(p), transform(t))
        case porc.NewTerminator.Z(p) =>
          porce.NewTerminator.create(transform(p))
        case porc.NewToken.Z(c) =>
          porce.NewToken.create(transform(c), ctx.execution)
        case porc.HaltToken.Z(c) =>
          porce.HaltToken.create(transform(c), ctx.execution)
        case porc.Kill.Z(c, t, k) =>
          porce.Kill.create(transform(c), transform(t), transform(k), ctx.execution)
        case porc.CheckKilled.Z(t) =>
          porce.CheckKilled.create(transform(t))
        case porc.Bind.Z(fut, v) =>
          porce.Bind.create(transform(fut), transform(v), ctx.execution)
        case porc.BindStop.Z(fut) =>
          porce.BindStop.create(transform(fut))
        case porc.Spawn.Z(c, t, must, comp) =>
          porce.Spawn.create(transform(c), transform(t), must, transform(comp), ctx.execution)
        case porc.Resolve.Z(p, c, t, futures) =>
          // Due to not generally being on as hot of paths we do not have a single value version of Resolve. It could easily be created if needed.
          val join = lookupVariable(LocalVariables.Join)
          val newJoin = porce.Write.Local.create(
            join,
            porce.Resolve.New.create(transform(p), transform(c), transform(t), futures.size, ctx.execution))
          val processors = futures.zipWithIndex.map { p =>
            val (f, i) = p
            porce.Resolve.Future.create(porce.Read.Local.create(join), transform(f), i)
          }
          val finishJoin = porce.Resolve.Finish.create(porce.Read.Local.create(join), ctx.execution)
          finishJoin.setTail(thisCtx.inTailPosition)
          porce.Sequence.create((newJoin +: processors :+ finishJoin).toArray)
        case porc.Force.Z(p, c, t, Seq(future)) =>
          // Special optimized case for only one future.
          porce.Force.SingleFuture.create(transform(p), transform(c), transform(t), transform(future), ctx.execution)
        case porc.Force.Z(p, c, t, futures) =>
          val join = lookupVariable(LocalVariables.Join)
          val newJoin = porce.Write.Local.create(
            join,
            porce.Force.New.create(transform(p), transform(c), transform(t), futures.size, ctx.execution))
          val processors = futures.zipWithIndex.map { p =>
            val (f, i) = p
            porce.Force.Future.create(porce.Read.Local.create(join), transform(f), i, ctx.execution)
          }
          val finishJoin = porce.Force.Finish.create(porce.Read.Local.create(join), ctx.execution)
          finishJoin.setTail(thisCtx.inTailPosition)
          porce.Sequence.create((newJoin +: processors :+ finishJoin).toArray)
        case porc.SetDiscorporate.Z(c) =>
          porce.SetDiscorporate.create(transform(c))
        case porc.TryOnException.Z(b, h) =>
          porce.TryOnException.create(transform(b)(thisCtx), transform(h)(thisCtx))
        case porc.TryFinally.Z(b, h) =>
          porce.TryFinally.create(transform(b), transform(h)(thisCtx))
        case porc.IfLenientMethod.Z(arg, l, r) =>
          porce.IfLenientMethod.create(transform(arg), transform(l), transform(r))
        case porc.GetField.Z(o, f) =>
          porce.GetField.create(transform(o), f, ctx.execution)
        case porc.GetMethod.Z(o) =>
          porce.GetMethod.create(transform(o), ctx.execution)
        case porc.New.Z(bindings) =>
          val newBindings = bindings.mapValues(transform(_)).view.force
          val fieldOrdering = normalizeFieldOrder(bindings.keys)
          //Logger.fine(s"Generating object: $fieldOrdering ${System.identityHashCode(fieldOrdering)}\n${e.value}")
          val fieldValues = fieldOrdering.map(newBindings(_))
          porce.NewObject.create(fieldOrdering.toArray, fieldValues.toArray)
      }
      res.setPorcAST(e.value)
      res.setTail(thisCtx.inTailPosition)
      res
    }
    // */  ???
  }

  def transform(m: porc.Method.Z, index: Int, closureSlot: FrameSlot, closureVariables: Seq[porc.Variable], methodOffset: Int)(implicit ctx: Context): porce.Expression = {
    val oldCtx = ctx

    def process(arguments: Seq[porc.Variable]) = {
      val rootNode = m.value.optionalIndex.flatMap(closureMap.get(_)) match {
        case None =>
          val descriptor = new FrameDescriptor()
          val ctx = oldCtx.withTailPosition.copy(descriptor = descriptor, closureVariables = closureVariables, argumentVariables = arguments)

          val newBody = transform(m.body)(ctx)
          val rootNode = porce.PorcERootNode.create(language, descriptor, newBody, arguments.size, closureVariables.size,
              m.name, ctx.execution)
          rootNode.setVariables(arguments, closureVariables)
          rootNode.setPorcAST(m.value)

          makeCallTarget(rootNode)

          rootNode
        case Some(callTarget) =>
          callTarget.getRootNode.asInstanceOf[porce.PorcERootNode]
      }

      val methodSlot = oldCtx.descriptor.findOrAddFrameSlot(m.name)
      val readClosure = porce.Read.Local.create(closureSlot)
      val newMethod = porce.MethodDeclaration.NewMethod.create(readClosure, methodOffset + index, rootNode, m.value.isRoutine)

      porce.Write.Local.create(methodSlot, newMethod)
    }

    val res = m match {
      case porc.MethodDirect.Z(name, _, arguments, body) =>
        process(arguments)
      case porc.MethodCPS.Z(name, pArg, cArg, tArg, _, arguments, body) =>
        process(pArg +: cArg +: tArg +: arguments)
    }
    res.setPorcAST(m.value)
    res
  }
}
