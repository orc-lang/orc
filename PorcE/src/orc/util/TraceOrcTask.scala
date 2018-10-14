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

import orc.{ DirectInvoker, Invoker, OrcRuntime }
import orc.run.porce.runtime.MaterializedCPSCallContext
import orc.values.Signal
import orc.values.sites.Site

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

object TraceOrcTask {
  final val Execute = 50L
  orc.util.Tracer.registerEventTypeId(Execute, "Execute ")

  @TruffleBoundary
  def traceExecute(ctx: MaterializedCPSCallContext, x: Long, y: Long): Unit = {
    orc.util.Tracer.trace(Execute, System.identityHashCode(ctx.p.environment), x, y)
  }

  @TruffleBoundary
  def traceExecute(x: Long, y: Long): Unit = {
    orc.util.Tracer.trace(Execute, 0, x, y)
  }
}

object TraceTask extends Site {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    // FIXME: Support 2 args and produce useful errors if called wrong.
    new DirectInvoker {
      def canInvoke(target: AnyRef,arguments: Array[AnyRef]): Boolean = {
        target == TraceTask && arguments(0).isInstanceOf[Number]
      }

      def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
        TraceOrcTask.traceExecute(arguments(0).asInstanceOf[Number].longValue(), 0)
        Signal
      }
    }
  }
}
