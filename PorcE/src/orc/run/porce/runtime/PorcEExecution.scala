package orc.run.porce.runtime

import java.util.logging.Level

import com.oracle.truffle.api.{ RootCallTarget, Truffle }
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.RootNode

import orc.{ ExecutionRoot, OrcEvent, PublishedEvent }
import orc.run.core.EventHandler
import orc.run.porce.{ HasId, InvokeCallRecordRootNode, Logger, PorcEUnit }
import orc.run.porce.distrib.CallTargetManager

class PorcEExecution(val runtime: PorcERuntime, protected var eventHandler: OrcEvent => Unit)
  extends ExecutionRoot with EventHandler with CallTargetManager with NoInvocationInterception {
  val truffleRuntime = Truffle.getRuntime()

  runtime.installHandlers(this)
  
  @TruffleBoundary @noinline
  def notifyOrcWithBoundary(e: OrcEvent) = {
    notifyOrc(e)
  }

  /// CallTargetManager Implementation

  // FIXME: PERFORMANCE: This should really be an Array. However that would require more work to get right. So I'll do it if it's needed.
  val callTargetMap = collection.mutable.HashMap[Int, RootCallTarget]()

  protected def setCallTargetMap(callTargetMap: collection.Map[Int, RootCallTarget]) = {
    require(!callTargetMap.contains(-1))
    this.callTargetMap ++= callTargetMap
  }

  def callTargetToId(callTarget: RootCallTarget): Int = {
    callTarget.getRootNode() match {
      case r: HasId => r.getId()
      case _ =>
        throw new IllegalArgumentException(s"callTargetToId only accepts CallTargets which reference a RootNode implementing HasId. Received $callTarget")
    }
  }

  def idToCallTarget(id: Int): RootCallTarget = {
    callTargetMap(id)
  }

  val callSiteMap = new java.util.concurrent.ConcurrentHashMap[Int, RootCallTarget]()

  def invokeCallTarget(callSiteId: Int, p: PorcEClosure, c: Counter, t: Terminator, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    val callTarget = callSiteMap.computeIfAbsent(callSiteId, (_) => new InvokeCallRecordRootNode(arguments.length + 3, this).getCallTarget())
    callTarget.call(Array(Array.emptyObjectArray, target, p, c, t) ++: arguments)
  }
}

trait PorcEExecutionWithLaunch extends PorcEExecution {
  execution =>
  
  private var _isDone = false

  /** Block until this context halts.
    */
  def waitForHalt(): Unit = {
    synchronized {
      while (!_isDone) wait()
    }
  }

  def isDone = execution.synchronized { _isDone }

  val pRootNode = new RootNode(null) with HasId {
    def execute(frame: VirtualFrame): Object = {
      // Skip the first argument since it is our captured value array.
      val v = frame.getArguments()(1)
      notifyOrcWithBoundary(PublishedEvent(v))
      // Token: from initial caller of p.
      c.haltToken()
      PorcEUnit.SINGLETON
    }

    def getId() = -1
  }

  val pCallTarget = truffleRuntime.createCallTarget(pRootNode)

  val p: PorcEClosure = new orc.run.porce.runtime.PorcEClosure(Array.emptyObjectArray, pCallTarget, false)

  val c: Counter = new Counter {
    def onResurrect() = {
      throw new AssertionError("The top-level counter can never be resurrected.")
    }

    def onHalt(): Unit = {
      // Runs regardless of discorporation.
      Logger.fine("Top level context complete.")
      runtime.removeRoot(execution)
      execution.synchronized {
        execution._isDone = true
        execution.notifyAll()
      }
    }
  }

  val t = new Terminator

  def scheduleProgram(prog: PorcEClosure, callTargetMap: collection.Map[Int, RootCallTarget]): Unit = {
    setCallTargetMap(callTargetMap)

    val nStarts = System.getProperty("porce.nStarts", "1").toInt
    // Token: From initial.
    for (_ <- 0 until nStarts) {
      c.newToken()
      runtime.schedule(CallClosureSchedulable.varArgs(prog, Array(null, p, c, t)))
    }
    c.haltToken()

    if (CounterConstants.tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      Thread.sleep(10000)
      Counter.report()
    }
  } 
  
  protected override def setCallTargetMap(callTargetMap: collection.Map[Int, RootCallTarget]) = {
    super.setCallTargetMap(callTargetMap)
    this.callTargetMap += (-1 -> pCallTarget)
  }
}