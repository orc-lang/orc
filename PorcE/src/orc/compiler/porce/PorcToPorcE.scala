package orc.compiler.porce

import scala.collection.mutable

import com.oracle.truffle.api.{ RootCallTarget, Truffle }
import com.oracle.truffle.api.frame.{ FrameDescriptor, FrameSlot, FrameSlotKind }

import orc.ast.{ ASTWithIndex, porc }
import orc.run.porce
import orc.run.porce.runtime.{ PorcEClosure, PorcEExecution, PorcEExecutionHolder, PorcERuntime }
import orc.util.{ TFalse, TTrue, TUnknown }
import orc.values.Field
import swivel.Zipper
import orc.run.porce.PorcELanguage

object PorcToPorcE {
  def apply(m: porc.MethodCPS, execution: PorcEExecutionHolder, usingInvokationInterceptor: Boolean, language: PorcELanguage = null): (PorcEClosure, collection.Map[Int, RootCallTarget]) = {
    new PorcToPorcE(usingInvokationInterceptor, language)(m, execution)
  }
}

class PorcToPorcE(val usingInvokationInterceptor: Boolean, val language: PorcELanguage) {
  case class Context(
      descriptor: FrameDescriptor, execution: PorcEExecutionHolder,
      argumentVariables: Seq[porc.Variable], closureVariables: Seq[porc.Variable],
      runtime: PorcERuntime, inTailPosition: Boolean) {
    def withTailPosition = copy(inTailPosition = true)
    def withoutTailPosition = copy(inTailPosition = false)
  }
  
  implicit def unwrapPorcEExecutionHolder(execution: PorcEExecutionHolder): PorcEExecution = {
    execution.exec
  }
  
  implicit class AddContextMap[T](val underlying: Seq[T]) {
    def contextMap[B](f: (T, Context) => B)(implicit ctx: Context): Seq[B] = {
      underlying.init.map(f(_, ctx.withoutTailPosition)) :+ f(underlying.last, ctx)
    }
  }

  def getEnclosingMethod(e: porc.PorcAST.Z): Option[porc.Variable] = {
    e.parents.
      takeWhile({ case _: porc.Continuation.Z => false; case _ => true }).
      collectFirst({ case porc.Method.Z(n, _, _, _) => n })
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

  def transform(x: porc.Variable)(implicit ctx: Context): porce.Expression = {
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

  def apply(m: porc.MethodCPS, execution: PorcEExecutionHolder): (PorcEClosure, collection.Map[Int, RootCallTarget]) = {
    val descriptor = new FrameDescriptor()
    implicit val ctx = Context(
      descriptor = descriptor,
      execution = execution,
      argumentVariables = Seq(m.pArg, m.cArg, m.tArg),
      closureVariables = Seq(),
      runtime = execution.newRef().get().runtime,
      inTailPosition = true)
    val newBody = transform(m.body.toZipper())
    val rootNode = porce.PorcERootNode.create(language, descriptor, newBody, 3, 0)
    rootNode.setPorcAST(m)
    val callTarget = makeCallTarget(rootNode)
    val closure = new PorcEClosure(Array(), callTarget, true)
    (closure, closureMap)
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
        case porc.Continuation.Z(args, body) =>
          val descriptor = new FrameDescriptor()
          val capturedVars = normalizeOrder(e.freeVars)
          val capturingExprs = capturedVars.map(transform(_)(innerCtx)).toArray

          {
            val ctx = innerCtx.withTailPosition.copy(descriptor = descriptor, closureVariables = capturedVars, argumentVariables = args)

            val newBody = transform(body)(ctx)
            val rootNode = porce.PorcERootNode.create(language, descriptor, newBody, args.size, capturedVars.size)
            rootNode.setPorcAST(e.value)
            makeCallTarget(rootNode)
            porce.NewContinuation.create(capturingExprs, rootNode)
          }
        case porc.CallContinuation.Z(target, arguments) =>
          porce.call.CallContinuation.CPS.create(transform(target)(innerCtx), arguments.map(transform(_)(innerCtx)).toArray, ctx.execution.newRef(), thisCtx.inTailPosition)
        case porc.MethodCPSCall.Z(isExt, target, p, c, t, arguments) =>
          lazy val newTarget = transform(target)(innerCtx)
          val newArguments = (p +: c +: t +: arguments).map(transform(_)(innerCtx)).toArray
          val exec = ctx.execution.newRef()
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
          val exec = ctx.execution.newRef()
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
        case porc.NewSimpleCounter.Z(p, h) =>
          porce.NewCounter.Simple.create(ctx.execution, transform(p), transform(h))
        case porc.NewServiceCounter.Z(p, p2, t) =>
          porce.NewCounter.Service.create(ctx.execution, transform(p), transform(p2), transform(t))
        case porc.NewTerminatorCounter.Z(p, t) =>
          porce.NewCounter.Terminator.create(ctx.execution, transform(p), transform(t))
        case porc.NewTerminator.Z(p) =>
          porce.NewTerminator.create(transform(p))
        case porc.NewToken.Z(c) =>
          porce.NewToken.create(transform(c))
        case porc.HaltToken.Z(c) =>
          porce.HaltToken.create(transform(c), ctx.execution.newRef())
        case porc.Kill.Z(c, t, k) =>
          porce.Kill.create(transform(c), transform(t), transform(k), ctx.execution.newRef())
        case porc.CheckKilled.Z(t) =>
          porce.CheckKilled.create(transform(t))
        case porc.Bind.Z(fut, v) =>
          porce.Bind.create(transform(fut), transform(v))
        case porc.BindStop.Z(fut) =>
          porce.BindStop.create(transform(fut))
        case porc.Spawn.Z(c, t, must, comp) =>
          porce.Spawn.create(transform(c), transform(t), must, transform(comp), ctx.execution.newRef())
        case porc.Resolve.Z(p, c, t, futures) =>
          // Due to not generally being on as hot of paths we do not have a single value version of Resolve. It could easily be created if needed.
          val join = lookupVariable(LocalVariables.Join)
          val newJoin = porce.Write.Local.create(
            join,
            porce.Resolve.New.create(transform(p), transform(c), transform(t), futures.size, ctx.execution.newRef()))
          val processors = futures.zipWithIndex.map { p =>
            val (f, i) = p
            porce.Resolve.Future.create(porce.Read.Local.create(join), transform(f), i)
          }
          val finishJoin = porce.Resolve.Finish.create(porce.Read.Local.create(join), ctx.execution.newRef())
          finishJoin.setTail(thisCtx.inTailPosition)
          porce.Sequence.create((newJoin +: processors :+ finishJoin).toArray)
        case porc.Force.Z(p, c, t, Seq(future)) =>
          // Special optimized case for only one future.
          porce.Force.SingleFuture.create(transform(p), transform(c), transform(t), transform(future), ctx.execution.newRef())
        case porc.Force.Z(p, c, t, futures) =>
          val join = lookupVariable(LocalVariables.Join)
          val newJoin = porce.Write.Local.create(
            join,
            porce.Force.New.create(transform(p), transform(c), transform(t), futures.size, ctx.execution.newRef()))
          val processors = futures.zipWithIndex.map { p =>
            val (f, i) = p
            porce.Force.Future.create(porce.Read.Local.create(join), transform(f), i)
          }
          val finishJoin = porce.Force.Finish.create(porce.Read.Local.create(join), ctx.execution.newRef())
          finishJoin.setTail(thisCtx.inTailPosition)
          porce.Sequence.create((newJoin +: processors :+ finishJoin).toArray)
        case porc.SetDiscorporate.Z(c) =>
          porce.SetDiscorporate.create(transform(c))
        case porc.TryOnException.Z(b, h) =>
          porce.TryOnException.create(transform(b)(thisCtx), transform(h)(thisCtx))
        case porc.TryFinally.Z(b, h) =>
          porce.TryFinally.create(transform(b), transform(h))
        case porc.IfLenientMethod.Z(arg, l, r) =>
          porce.IfLenientMethod.create(transform(arg), transform(l), transform(r))
        case porc.GetField.Z(o, f) =>
          porce.GetField.create(transform(o), f, ctx.execution.newRef())
        case porc.GetMethod.Z(o) =>
          porce.GetMethod.create(transform(o), ctx.execution.newRef())
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
      val descriptor = new FrameDescriptor()
      val ctx = oldCtx.withTailPosition.copy(descriptor = descriptor, closureVariables = closureVariables, argumentVariables = arguments)

      val newBody = transform(m.body)(ctx)
      val methodSlot = oldCtx.descriptor.findOrAddFrameSlot(m.name)
      val rootNode = porce.PorcERootNode.create(language, descriptor, newBody, arguments.size, closureVariables.size)
      rootNode.setPorcAST(m.value)
      val readClosure = porce.Read.Local.create(closureSlot)

      makeCallTarget(rootNode)

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
