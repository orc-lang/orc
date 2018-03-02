package orc.run.porce.runtime

import java.io.{ OutputStreamWriter, PrintWriter }
import java.util.{ Timer, TimerTask }

import orc.{ CaughtEvent, ExecutionRoot, HaltedOrKilledEvent, OrcEvent, PublishedEvent }
import orc.error.runtime.{ ExceptionHaltException, HaltException }
import orc.run.core.EventHandler
import orc.run.distrib.porce.CallTargetManager
import orc.run.porce.{ HasId, InvokeCallRecordRootNode, InvokeWithTrampolineRootNode, Logger, PorcEUnit, SimpleWorkStealingSchedulerWrapper }
import orc.run.porce.instruments.DumpSpecializations
import orc.util.{ CsvWriter, ExecutionLogOutputStream }

import com.oracle.truffle.api.{ RootCallTarget, Truffle }
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.nodes.Node

class PorcEExecution(val runtime: PorcERuntime, protected var eventHandler: OrcEvent => Unit)
  extends ExecutionRoot with EventHandler with CallTargetManager with NoInvocationInterception {
  val truffleRuntime = Truffle.getRuntime()

  runtime.installHandlers(this)

  @TruffleBoundary @noinline
  def notifyOrcWithBoundary(e: OrcEvent) = {
    notifyOrc(e)
  }

  @TruffleBoundary(allowInlining = true) @noinline
  def notifyOfException(e: Throwable, node: Node) = {
    def handle(e: Throwable) = {
      val oe = OrcBacktrace.orcifyException(e, node)
      notifyOrc(new CaughtEvent(oe))
    }
    
    e match {
      case e: ExceptionHaltException => handle(e.getCause())
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

  val callSiteMap = new java.util.concurrent.ConcurrentHashMap[Int, RootCallTarget]()

  def invokeCallTarget(callSiteId: Int, p: PorcEClosure, c: Counter, t: Terminator, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    val callTarget = {
      val v = callSiteMap.get(callSiteId)
      if (v == null)
        callSiteMap.computeIfAbsent(callSiteId, (_) => 
          truffleRuntime.createCallTarget(new InvokeCallRecordRootNode(runtime.language, arguments.length + 3, this)))
      else
        v
    }
    val args = Array(Array.emptyObjectArray, target, p, c, t) ++: arguments
    //Logger.info(s"$callSiteId: $p $c $t $target $arguments => $callTarget(${args.mkString(", ")}")
    callTarget.call(args: _*)
  }

  val trampolineMap = new java.util.concurrent.ConcurrentHashMap[RootNode, RootCallTarget]()

  def invokeClosure(target: PorcEClosure, args: Array[AnyRef]): Unit = {
    val callTarget = {
      val key = target.body.getRootNode()
      val v = trampolineMap.get(key)
      if (v == null)
        trampolineMap.computeIfAbsent(key, (root) => 
          truffleRuntime.createCallTarget(new InvokeWithTrampolineRootNode(runtime.language, root, this)))
      else
        v
    }
    args(0) = target.environment
    callTarget.call(args: _*)
  }
  
  {
    // This is disabled debug code for tracing problems related to Counters
    if (false) {
      val timer = new Timer(true)
      timer.schedule(new TimerTask {
        def run(): Unit = {
          Counter.report()
        }
      }, 20000)
    }
  }

  def onProgramHalted() = {
    import scala.collection.JavaConverters._
     
    /* ROOTNODE-STATISTICS
    {
      val csvOut = ExecutionLogOutputStream("rootnode-statistics", "csv", "RootNode run times data")
      if (csvOut.isDefined) {
        val traceCsv = new OutputStreamWriter(csvOut.get, "UTF-8")
        val csvWriter = new CsvWriter(traceCsv.append(_))
        val tableColumnTitles = Seq(
            "RootNode Name [name]", "Total Spawns [spawns]", 
            "Total Bind Single Future [bindSingle]", "Total Bind Multiple Futures [bindJoin]", 
            "Total Halt Continuation [halt]", "Total Publication Callbacks [publish]",
            "Total Spawned Time (ns) [totalSpawnedTime]", "Total Spawned Calls [spawnedCalls]")
        csvWriter.writeHeader(tableColumnTitles)
        for (t <- callTargetMap.values) {
          t.getRootNode match {
            case n: PorcERootNode =>
              val (nSpawns, nBindSingle, nBindJoin, nHalt, nPublish, nSpawnedCalls, spawnedTime) = n.getCollectedCallInformation()
              csvWriter.writeRow(Seq(n.getName, nSpawns, nBindSingle, nBindJoin, nHalt, nPublish, nSpawnedCalls, spawnedTime, 
                  n.porcNode.map(_.sourceTextRange.toString).getOrElse("")))
            case _ =>
              ()
          }
        }
        traceCsv.close()
      }
    }
    */
  
    {
      val csvOut = ExecutionLogOutputStream("trampoline-calls", "csv", "Trampoline call counts")
      if (csvOut.isDefined) {
        val traceCsv = new OutputStreamWriter(csvOut.get, "UTF-8")
        val csvWriter = new CsvWriter(traceCsv.append(_))
        val tableColumnTitles = Seq(
            "RootNode Name [name]", 
            "Number of Calls [calls]")
        csvWriter.writeHeader(tableColumnTitles)
        for (t <- trampolineMap.values().asScala) {
          t.getRootNode match {
            case n: InvokeWithTrampolineRootNode if n.getRoot() != null =>
              csvWriter.writeRow(Seq(n.getRoot().getName, n.getCallCount, n.getRoot().porcNode.map(_.sourceTextRange.toString).getOrElse("")))
            case n: InvokeWithTrampolineRootNode =>
              Logger.warning(s"Failed to dump trampoline call counts since root == null: $n")
            case _ =>
              ()
          }
        }
        traceCsv.close()
      }
    }
  
    val specializationsOut = ExecutionLogOutputStream("truffle-node-specializations", "txt", "Truffle node specializations")
    if (specializationsOut.isDefined) {
        val out = new PrintWriter(new OutputStreamWriter(specializationsOut.get))
        val callTargets = callTargetMap.values.toSet ++ trampolineMap.values.asScala ++ callSiteMap.values.asScala
        for (t <- callTargets.toSeq.sortBy(_.toString)) {
          DumpSpecializations(t.getRootNode, out)
        }
        out.close()
    }
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
      notifyOrc(HaltedOrKilledEvent)
      execution.synchronized {
        execution._isDone = true
        execution.notifyAll()
      }
      execution.onProgramHalted()
    }
  }

  val t = new Terminator

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

  protected override def setCallTargetMap(callTargetMap: collection.Map[Int, RootCallTarget]) = {
    super.setCallTargetMap(callTargetMap)
    this.callTargetMap += (-1 -> pCallTarget)
  }  
}