package orc.run

import orc.util.ExecutionLogOutputStream
import java.io.OutputStreamWriter
import orc.util.CsvWriter

/** A tracer object for profiling the runtime.
  *
  * This is mostly designed for use with PorcE, but could be used with any runtime.
  * It traces transitions into and out of different parts of the runtime. A post
  * processor (run in process to avoid serialization overhead) extracts timing
  * information in different regions based on the transitions.
  *
  */
object RuntimeProfiler {  
  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  @inline
  final val profileRuntime = false
  
  /* Regions of interest:
   * 
   * Call dispatch
   * Site implementations
   * 
   * Other interesting regions to implement later:
   * 
   * Scheduling overhead (closure creation and scheduling)
   * 
   * Regions are expected to form a stack during execution, with matching
   * nested enter and exit events.
   */
  
  @inline
  final val EnterRegion = 41L
  orc.util.Tracer.registerEventTypeId(EnterRegion, "EntrRegn")
  @inline
  final val ExitRegion = 42L
  orc.util.Tracer.registerEventTypeId(ExitRegion, "ExitRegn")
  
  @inline
  final val CallDispatch = 1L
  @inline
  final val SiteImplementation = 2L
  @inline
  final val JavaDispatch = 3L
  
  def regionToString(region: Long) = region match {
    case CallDispatch => "CallDispatch"
    case SiteImplementation => "SiteImplementation"
    case JavaDispatch => "JavaDispatch"
  }

  @inline
  def traceEnter(region: Long): Unit = {
    if (profileRuntime) {
      orc.util.Tracer.trace(EnterRegion, -1, 0L, region)
    }
  }

  @inline
  def traceEnter(region: Long, id: Int): Unit = {
    if (profileRuntime) {
      orc.util.Tracer.trace(EnterRegion, id, 0L, region)
    }
  }

  @inline
  def traceExit(region: Long): Unit = {
    if (profileRuntime) {
      orc.util.Tracer.trace(ExitRegion, -1, region, 0L)
    }
  }

  
  @inline
  def traceExit(region: Long, id: Int): Unit = {
    if (profileRuntime) {
      orc.util.Tracer.trace(ExitRegion, id, region, 0L)
    }
  }
  
  def calibrationIterations = 100000
  
  @volatile
  var bh1 = 0
  @volatile
  var bh2 = 1
  
  // Consume i in such a way that the compiler cannot optimize it away.
  // This technique is lifted from JMH: jmh-core/src/main/java/org/openjdk/jmh/logic/BlackHole.java
  def consume(i: Int) = {
    if(i == bh1 & i == bh2) {
      // This code is unreachable globally, but reachable from the local view of the compiler
      bh1 = i
      // The write prevents speculation on the predicate of the if.
    }
  }
  
  def calibration(): (Long, Long) = {
    var i = calibrationIterations
    val start = System.nanoTime()
    while(i > 0) {
      traceEnter(-2, -1)
      consume(i)
      traceExit(-2, -1)
      i -= 1
    }
    val end = System.nanoTime()
    val totalTime = end - start
    
    val buffers = orc.util.Tracer.takeBuffers()
    
    var totalMeasuredTime = 0L
    var nMeasures = 0
    
    for (buf <- buffers) {
      var lastEnter = 0L
      for (i <- 0 until buf.eventsInBuffer if buf.locationIds(i) == -1) {
        buf.typeIds(i) match {
              case EnterRegion if buf.toArgs(i) == -2 =>
                lastEnter = buf.nanotimes(i)
              case ExitRegion if buf.fromArgs(i) == -2 && lastEnter != 0 =>
                totalMeasuredTime += buf.nanotimes(i) - lastEnter
                nMeasures += 1
              case _ => ()
            }
      }
    }
    
    assert(nMeasures == calibrationIterations)
    val innerTime = totalMeasuredTime / nMeasures
    val outerTime = totalTime / calibrationIterations - innerTime 
    
    (innerTime, outerTime)
  }

  def dump(suffix: String) = synchronized {
    if (profileRuntime) {
      //orc.util.Tracer.dumpBuffers(suffix)
      val buffers = orc.util.Tracer.takeBuffers()
      
      val (calibrationInnerTime, calibrationOuterTime) = calibration()
      
      Logger.info(s"Measured calibration values ($calibrationInnerTime, $calibrationOuterTime)")
      
      val csvOut = ExecutionLogOutputStream(s"runtime_profile_$suffix", "csv", "Runtime profile file")
      if (csvOut.isDefined) {
        val traceCsv = new OutputStreamWriter(csvOut.get, "UTF-8")
        val csvWriter = new CsvWriter(traceCsv.append(_))
        val tableColumnTitles = Seq(
            "Call Site ID [callId]",
            //"Tag [tag] (a tag summerizing context information for this region)",
            "Call Dispatch Time (ns) [cdTime]",
            "Java Dispatch Time (ns) [jdTime]",
            "Site Implementation Time (ns) [siteTime]",
            )
        csvWriter.writeHeader(tableColumnTitles)
        
        for (buf <- buffers) {
          case class ProfileFrame(tpe: Long, start: Long, end: Option[Long]) {
            def len = end map { _ - start }
          }
          var stack = List[ProfileFrame]()
          var nested = List[ProfileFrame]()
          for (i <- 0 until buf.eventsInBuffer) {
            buf.typeIds(i) match {
              case EnterRegion =>
                buf.toArgs(i) match {
                  case CallDispatch =>
                    // Clear stack assuming calls cannot be nested.
                    stack = ProfileFrame(CallDispatch, buf.nanotimes(i), None) :: Nil
                    nested = Nil
                  case JavaDispatch =>
                    stack = ProfileFrame(JavaDispatch, buf.nanotimes(i), None) :: stack
                  case SiteImplementation =>
                    stack = ProfileFrame(SiteImplementation, buf.nanotimes(i), None) :: stack
                }
              case ExitRegion =>
                val region = buf.fromArgs(i)
                val pops = stack.indexWhere(_.tpe == region)
                for (_ <- 0 to pops) {
                  nested = stack.head.copy(end = Some(buf.nanotimes(i))) :: nested
                  stack = stack.tail
                }
                if (region == CallDispatch && nested.nonEmpty) {
                  def selfType(region: Long): Long = {
                    val i = nested.indexWhere(_.tpe == region)
                    val n = nested.drop(i)
                    if (n.nonEmpty && i >= 0)
                      n.head.len.get - calibrationInnerTime -
                        n.tail.headOption.flatMap(_.len).getOrElse(0L) - calibrationOuterTime
                    else
                      0
                  }
                  csvWriter.writeRow((
                      buf.locationIds(i),
                      //if (nested.tail.exists(_.tpe == JavaDispatch)) "J" else "O",
                      selfType(CallDispatch),
                      selfType(JavaDispatch),
                      selfType(SiteImplementation)
                      ))
                  
                  // Clear stack assuming calls cannot be nested.
                  stack = Nil
                  nested = Nil
                }
              case _ => ()
            }
          }
        }
        traceCsv.close()
      }
    }
  }
}