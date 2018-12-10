//
// PorcEExecution.scala -- Scala class PorcEExecution
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.io.{ FileOutputStream, OutputStreamWriter, PrintWriter }
import java.util.{ ArrayList, Collections }
import java.util.logging.Level

import scala.ref.WeakReference

import orc.{ CaughtEvent, ExecutionRoot, HaltedOrKilledEvent, OrcEvent, PublishedEvent }
import orc.error.runtime.{ HaltException, TokenError }
import orc.run.core.EventHandler
import orc.run.distrib.porce.CallTargetManager
import orc.run.porce.{ HasId, InvokeCallRecordRootNode, InvokeWithTrampolineRootNode, Logger, PorcERootNode, PorcEUnit, SimpleWorkStealingSchedulerWrapper }
import orc.run.porce.instruments.DumpSpecializations
import orc.util.{ DumperRegistry, ExecutionLogOutputStream }

import com.oracle.truffle.api.{ RootCallTarget, Truffle }
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.{ Node, RootNode }
import java.lang.reflect.WeakCache
import java.util.WeakHashMap
import scala.collection.mutable.WeakHashMap

abstract class PorcEExecution(val runtime: PorcERuntime, protected var eventHandler: OrcEvent => Unit)
  extends ExecutionRoot with EventHandler with CallTargetManager with NoInvocationInterception {
  val truffleRuntime = Truffle.getRuntime()

  runtime.installHandlers(this)

  val oldHandler = eventHandler
  eventHandler = {
    case e @ CaughtEvent(je: Error) => { Logger.log(Level.SEVERE, "Unexpected Java Error thrown; killing Orc Execution", je); oldHandler(e); kill() }
    case e @ CaughtEvent(_: TokenError) => { oldHandler(e); kill() }
    case e => oldHandler(e)
  }

  protected val allCounters = scala.collection.mutable.WeakHashMap[Counter,Unit]()
  def trackCounter(c: Counter) = allCounters synchronized { allCounters.put(c, ()) }

  /** Kill this execution */
  def kill()

  @TruffleBoundary @noinline
  def notifyOrcWithBoundary(e: OrcEvent) = {
    notifyOrc(e)
  }

  def notifyOfException(e: Throwable, node: Node) = {
    @TruffleBoundary @noinline
    def handle(e: Throwable) = {
      val oe = OrcBacktrace.orcifyException(e, node)
      notifyOrc(new CaughtEvent(oe))
    }

    e match {
      case e: HaltException if e.getCause != null => handle(e.getCause())
      case e: HaltException => ()
      case e: Throwable => handle(e)
    }
  }

  /// CallTargetManager Implementation

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

  // TODO: callSiteMap and trampolineMap are really similar. One is indexed by call target and one by call site. These will be strongly correlated. Should they be combined or otherwise used together?

  private val callSiteMap = new java.util.concurrent.ConcurrentHashMap[Int, RootCallTarget]()

  @TruffleBoundary(allowInlining = true) @noinline
  def invokeCallTarget(callSiteId: Int, p: PorcEClosure, c: Counter, t: Terminator, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    val callTarget = {
      val v = callSiteMap.get(callSiteId)
      if (v == null)
        callSiteMap.computeIfAbsent(callSiteId, (_) =>
          truffleRuntime.createCallTarget(new InvokeCallRecordRootNode(runtime.language, arguments.length + 3, callSiteId, this)))
      else
        v
    }
    val args = Array(Array.emptyObjectArray, target, p, c, t) ++: arguments
    callTarget.call(args: _*)
  }

  private val trampolineMap = new java.util.concurrent.ConcurrentHashMap[RootNode, RootCallTarget]()

  @TruffleBoundary(allowInlining = true) @noinline
  def invokeClosure(target: PorcEClosure, args: Array[AnyRef]): Unit = {
    val callTarget = {
      val key = target.body.getRootNode()
      def createCallTarget(root: RootNode) = truffleRuntime.createCallTarget(new InvokeWithTrampolineRootNode(runtime.language, root, this))
      key match {
        case key: PorcERootNode =>
          key.getTrampolineCallTarget
        case _ =>
          val v = trampolineMap.get(key)
          if (v == null)
            trampolineMap.computeIfAbsent(key, (root) => createCallTarget(root))
          else
            v
      }
    }
    args(0) = target.environment
    callTarget.call(args: _*)
  }

  private val extraRegisteredRootNodes = Collections.synchronizedList(new ArrayList[WeakReference[RootNode]]())

  def registerRootNode(root: RootNode): Unit = {
    extraRegisteredRootNodes.add(WeakReference(root))
  }

  private val specializationsFile = ExecutionLogOutputStream.getFile(s"truffle-node-specializations", "txt")
  private var lastGoodRepNumber = 0

  specializationsFile foreach { specializationsFile =>
    DumperRegistry.register { name =>
      val repNum = try {
        name.drop(3).toInt + 1
      } catch {
        case _: NumberFormatException =>
          lastGoodRepNumber + 1
      }
      lastGoodRepNumber = repNum
      import scala.collection.JavaConverters._
      specializationsFile.delete()
      val out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(specializationsFile)))
      val callTargets = callTargetMap.values.toSet ++ trampolineMap.values.asScala ++ callSiteMap.values.asScala
      val ers = extraRegisteredRootNodes.asScala.collect({ case WeakReference(r) => r })
      for (r <- (callTargets.map(_.getRootNode) ++ ers).toSeq.sortBy(_.toString)) {
        DumpSpecializations(r, repNum, out)
      }
      out.close()
    }
  }

  def onProgramHalted() = {
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

  val c: Counter = new Counter(execution) {
    def onResurrect() = {
      throw new AssertionError("The top-level counter can never be resurrected.")
    }

    def onHaltOptimized(): PorcEClosure = {
      // Runs regardless of discorporation.
      Logger.fine("Top level context complete.")
      runtime.removeRoot(execution)
      notifyOrcWithBoundary(HaltedOrKilledEvent)
      execution.synchronized {
        execution._isDone = true
        execution.notifyAll()
      }
      execution.onProgramHalted()
      null
    }
  }

  val t = new Terminator()

  private val dumperTimer = new java.util.Timer("Execution dumper", true)
  dumperTimer.scheduleAtFixedRate(new java.util.TimerTask() { override def run() = { dumpExecutionState() } } , 10*1000, 10*1000)

  def dumpExecutionState() = {
    println(orc.util.GetScalaTypeName(this) + ":\n  counters=" + allCounters.keys.mkString("\n    ") + "\n  terminator=" + t)
  }

  def scheduleProgram(prog: PorcEClosure, callTargetMap: collection.Map[Int, RootCallTarget]): Unit = {
    setCallTargetMap(callTargetMap)

    Logger.finest(s"Loaded program. CallTagetMap:\n${callTargetMap.mkString("\n")}")

    val nStarts = System.getProperty("porce.nStarts", "1").toInt
    // Token: From initial.
    for (_ <- 0 until nStarts) {
      c.newToken()
      val s = CallClosureSchedulable.varArgs(prog, Array(null, p, c, t), execution)
      SimpleWorkStealingSchedulerWrapper.traceTaskParent(-1, SimpleWorkStealingSchedulerWrapper.getSchedulableID(s))
      runtime.schedule(s)
    }
    c.haltToken()
  }

  override def kill(): Unit = {
    t.kill()
  }

  protected override def setCallTargetMap(callTargetMap: collection.Map[Int, RootCallTarget]) = {
    super.setCallTargetMap(callTargetMap)
    this.callTargetMap += (-1 -> pCallTarget)
  }

}
