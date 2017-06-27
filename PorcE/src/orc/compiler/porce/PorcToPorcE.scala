package orc.compiler.porce

import orc.ast.porc
import orc.run.porce
import swivel.Zipper
import com.oracle.truffle.api.frame.FrameSlotKind
import com.oracle.truffle.api.frame.FrameDescriptor
import com.oracle.truffle.api.CallTarget
import orc.run.porce.runtime.PorcEExecution
import orc.run.porce.runtime.PorcEClosure
import orc.compile.Logger

class PorcToPorcE {
  case class Context(descriptor: FrameDescriptor, execution: PorcEExecution)

  private def lookupVariable(x: porc.Variable)(implicit ctx: Context) =
    ctx.descriptor.findOrAddFrameSlot(x, FrameSlotKind.Object)
  
  private def normalizeOrder(s: TraversableOnce[porc.Variable]) = {
    s.toSeq.sortBy(_.optionalName)
  }

  private def normalizeOrderAndLookup(s: TraversableOnce[porc.Variable])(implicit ctx: Context) = {
    normalizeOrder(s).map(lookupVariable(_))
  }

  def apply(e: porc.MethodCPS, execution: PorcEExecution): PorcEClosure = {
    val descriptor = new FrameDescriptor()
    implicit val ctx = Context(descriptor = descriptor, execution = execution)
    val m = transform(e.toZipper(), Seq(), Seq())
    m.getClosure(Array[AnyRef]())
  }

  def transform(e: porc.Expression.Z)(implicit ctx: Context): porce.Expression = {
    e match {
      case porc.Constant.Z(v) =>
        porce.Argument.createConstant(v)
      case porc.PorcUnit.Z() =>
        porce.Argument.createPorcUnit()
      case Zipper(x: porc.Variable, p) =>
        val slot = ctx.descriptor.findFrameSlot(x)
        porce.Argument.createVariable(slot)
      case porc.Sequence.Z(es) =>
        porce.Sequence.create(es.map(transform(_)).toArray)
      case porc.Let.Z(x, v, body) =>
        porce.Let.create(lookupVariable(x), transform(v), transform(body))
      case porc.Continuation.Z(args, body) =>
        val descriptor = new FrameDescriptor()
        val oldCtx = ctx
        val capturedVars = normalizeOrder(e.freeVars)
        val capturedSlots = capturedVars.map(lookupVariable)
        
        {
          implicit val ctx = oldCtx.copy(descriptor = descriptor)
          
          val argSlots = args.map(lookupVariable(_))
          val capturingSlots = capturedVars.map(lookupVariable)
          val newBody = transform(body)
          porce.Continuation.create(argSlots.toArray, capturedSlots.toArray, capturingSlots.toArray, descriptor, newBody)
        }
      case porc.CallContinuation.Z(target, arguments) =>
        porce.Call.InternalOnly.create(transform(target), arguments.map(transform(_)).toArray, ctx.execution)
      case porc.MethodCPSCall.Z(isExt, target, p, c, t, arguments) =>
        porce.Call.CPS.create(transform(target), (p +: c +: t +: arguments).map(transform(_)).toArray, ctx.execution)
      case porc.MethodDirectCall.Z(isExt, target, arguments) =>
        porce.Call.Direct.create(transform(target), arguments.map(transform(_)).toArray, ctx.execution)
      case porc.MethodDeclaration.Z(methods, body) =>
        val recCapturedVars = normalizeOrder(methods.map(_.name)).view.force
        val scopeCapturedVars = normalizeOrder(methods.flatMap(m => m.body.freeVars -- m.allArguments).toSet -- recCapturedVars)
        //val capturedVars = scopeCapturedVars ++ recCapturedVars

        Logger.fine(s"Converting decl group $recCapturedVars with:\nscopeCapturedVars = $scopeCapturedVars")
        
        val newMethods = methods.map(transform(_, scopeCapturedVars, recCapturedVars))
                
        porce.MethodDeclaration.create(newMethods.toArray, transform(body))
      case porc.NewFuture.Z() =>
        porce.NewFuture.create()
      case porc.NewCounter.Z(p, h) =>
        porce.NewCounter.create(ctx.execution, transform(p), transform(h))
      case porc.NewTerminator.Z(p) =>
        porce.NewTerminator.create(transform(p))
      case porc.Halt.Z(c) =>
        porce.Halt.create(transform(c))
      case porc.Kill.Z(t) =>
        porce.Kill.create(transform(t))
      case porc.SpawnBindFuture.Z(fut, c, t, comp) =>
        porce.SpawnBindFuture.create(transform(fut), transform(c), transform(t), transform(comp), ctx.execution)
      case porc.Spawn.Z(c, t, comp) =>
        porce.Spawn.create(transform(c), transform(t), transform(comp), ctx.execution)
      case porc.Force.Z(p, c, t, b, futures) =>
        porce.Force.create(transform(p), transform(c), transform(t), futures.map(transform).toArray, ctx.execution)
      case porc.SetDiscorporate.Z(c) =>
        porce.SetDiscorporate.create(transform(c))        
      case porc.TryOnKilled.Z(b, h) =>
        porce.TryOnKilled.create(transform(b), transform(h))
      case porc.TryOnHalted.Z(b, h) =>
        porce.TryOnHalted.create(transform(b), transform(h))
      case porc.TryFinally.Z(b, h) =>
        porce.TryFinally.create(transform(b), transform(h))    
        
      case porc.IfDef.Z(arg, l, r) =>
        porce.IfDef.create(transform(arg), transform(l), transform(r))      
      case porc.GetField.Z(o, f) =>
        porce.GetField.create(transform(o), f, ctx.execution)      
      case porc.New.Z(bindings) =>
        ???
    }
  }
  
  def transform(m: porc.Method.Z, scopeCapturedVars: Seq[porc.Variable], recCapturedVars: Seq[porc.Variable])(implicit ctx: Context): porce.Method = { 
    val oldCtx = ctx
    val scopeCapturingSlots = scopeCapturedVars.map(lookupVariable(_))
    
    def process(arguments: Seq[porc.Variable]) = {
      val descriptor = new FrameDescriptor()
      implicit val ctx = oldCtx.copy(descriptor = descriptor)
      val scopeCapturedSlots = (scopeCapturedVars ++ recCapturedVars).map(lookupVariable(_))
      assert(scopeCapturedSlots.size == scopeCapturingSlots.size + recCapturedVars.size)
      val argSlots = arguments.map(lookupVariable(_))
      val newBody = transform(m.body)
      Logger.fine(s"Converting decl ${m.name} with:\ncapturingSlots = $scopeCapturingSlots\ncapturedSlots = $scopeCapturedSlots\nargSlots = $argSlots\n$descriptor")
      porce.Method.create(oldCtx.descriptor.findOrAddFrameSlot(m.name), 
          argSlots.toArray, scopeCapturedSlots.toArray, scopeCapturingSlots.toArray, descriptor, m.value.isDef, newBody)
    }  
    
    m match {
      case porc.MethodDirect.Z(name, _, arguments, body) =>
        process(arguments)
      case porc.MethodCPS.Z(name, pArg, cArg, tArg, _, arguments, body) =>
        process(pArg +: cArg +: tArg +: arguments)
    }
  }
}