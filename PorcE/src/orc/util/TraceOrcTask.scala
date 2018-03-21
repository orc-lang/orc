//
// TraceOrcTask.scala -- Scala objects TraceOrcTask and Orc site TraceTask
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import orc.values.sites.InvokerMethod
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import orc.Invoker
import orc.OrcRuntime
import orc.run.porce.runtime.CPSCallContext
import orc.values.Signal
import orc.DirectInvoker

object TraceOrcTask {
  final val Execute = 50L
  orc.util.Tracer.registerEventTypeId(Execute, "Execute ")

  @TruffleBoundary
  def traceExecute(ctx: CPSCallContext, x: Long, y: Long): Unit = {
    orc.util.Tracer.trace(Execute, System.identityHashCode(ctx.p.environment), x, y)
  }

  @TruffleBoundary
  def traceExecute(x: Long, y: Long): Unit = {
    orc.util.Tracer.trace(Execute, 0, x, y)
  }
}

object TraceTask extends InvokerMethod {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    // FIXME: Support 2 args and produce useful errors if called wrong.
    new DirectInvoker {
      def canInvoke(target: AnyRef,arguments: Array[AnyRef]): Boolean = {
        target == TraceTask && arguments(0).isInstanceOf[Number]
      }
      
      def invoke(callContext: orc.CallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
        val ctx = callContext.asInstanceOf[CPSCallContext]
        TraceOrcTask.traceExecute(ctx, arguments(0).asInstanceOf[Number].longValue(), 0)
        callContext.publish(Signal)
      }
      
      def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
        TraceOrcTask.traceExecute(arguments(0).asInstanceOf[Number].longValue(), 0)
        Signal
      }
    }
  }
}