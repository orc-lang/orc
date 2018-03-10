package orc.util

import orc.values.sites.InvokerMethod
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import orc.Invoker
import orc.OrcRuntime
import orc.run.porce.runtime.CPSCallContext
import orc.values.Signal

object TraceOrcTask {
  final val Execute = 50L
  orc.util.Tracer.registerEventTypeId(Execute, "Execute ")

  @TruffleBoundary
  def traceExecute(ctx: CPSCallContext, x: Long, y: Long): Unit = {
    orc.util.Tracer.trace(Execute, System.identityHashCode(ctx.p.environment), x, y)
  }
}

object TraceTask extends InvokerMethod {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    new Invoker {
      def canInvoke(target: AnyRef,arguments: Array[AnyRef]): Boolean = {
        target == TraceTask && arguments(0).isInstanceOf[Number]
      }
      
      def invoke(callContext: orc.CallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
        val ctx = callContext.asInstanceOf[CPSCallContext]
        TraceOrcTask.traceExecute(ctx, arguments(0).asInstanceOf[Number].longValue(), 0)
        callContext.publish(Signal)
      }
    }
  }
}