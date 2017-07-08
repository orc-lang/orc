package orc.compiler.porce

import orc.ast.porc
import orc.run.porce
import swivel.Zipper
import com.oracle.truffle.api.frame.FrameSlotKind
import com.oracle.truffle.api.frame.FrameDescriptor
import com.oracle.truffle.api.CallTarget

class PorcToPorcE {
  case class Context(descriptor: FrameDescriptor)

  private def lookupVariable(x: porc.Variable)(implicit ctx: Context) =
    ctx.descriptor.findOrAddFrameSlot(x, FrameSlotKind.Object)

  def apply(e: porc.Expression): CallTarget = {
    val descriptor = new FrameDescriptor()
    implicit val ctx = Context(descriptor = descriptor)
    val body = porce.Continuation.create(Array(), Array(), transform(e.toZipper()), descriptor)
    body.getCallTarget()
  }

  def transform(e: porc.Expression.Z)(implicit ctx: Context): porce.Expression = {
    e match {
      case porc.Constant.Z(v) =>
        porce.Argument.createConstant(v)
      case porc.PorcUnit.Z() =>
        porce.Argument.createPorcUnit()
      case Zipper(x: porc.Variable, p) =>
        val slot = lookupVariable(x)
        porce.Argument.createVariable(slot)
      case porc.Sequence.Z(es) =>
        porce.Sequence.create(es.map(transform(_)).toArray)
      case porc.Let.Z(x, v, body) =>
        porce.Let.create(lookupVariable(x), transform(v), transform(body))
      case porc.Continuation.Z(args, body) =>
        val descriptor = new FrameDescriptor()
        val oldCtx = ctx
        
        {
          implicit val ctx = oldCtx.copy(descriptor = descriptor)
          
          val argSlots = args.map(lookupVariable(_))
          val capturedSlots = e.freeVars.toSeq.sortBy(_.optionalName).map(lookupVariable(_))
          val newBody = transform(body)
          porce.Continuation.create(argSlots.toArray, capturedSlots.toArray, newBody, descriptor)
        }
      case porc.CallContinuation.Z(target, arguments) =>
        porce.CallContinuation.create(transform(target), arguments.map(transform(_)).toArray)
    }
  }
}