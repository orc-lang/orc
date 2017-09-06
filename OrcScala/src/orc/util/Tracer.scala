//
// Tracer.scala -- Scala object Tracer
// Project OrcScala
//
// Created by jthywiss on Feb 21, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.io.{ File, FileOutputStream, OutputStreamWriter }
import java.lang.management.ManagementFactory

/** Rudimentary event tracing facility.
  *
  * Each thread writes events to a thread-local event trace buffer.  At JVM
  * shutdown, the trace buffers are dumped to stdout.
  *
  * Events record the time, a location ID, event type, and two event
  * arguments (suggestively named "from" and "to").  Times are recorded as
  * Java's system millisecond clock (UTC-ish time), and as Java's nanoTime
  * (higher resolution, but with undefined epoch).
  *
  * At Orc startup, event-publishing subsystems register event type IDs/names.
  *
  * Differentiating usage of Logger, Tracer, and Profiler: Logging is intended
  * for abnormal or significant events.  Tracing is intended for recording
  * routine events on an object for debugging.  Profiling is intended for
  * performance measurement.
  *
  * @author jthywiss
  */
object Tracer {

  //TODO: Rolling buffer alloc/swap/resize

  //TODO: "Who wants to be a macro?" "Oooh, pick me, pick me!!"

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceOn = false

  final val onlyDumpSelectedLocations = false

  private val selectedLocations: java.util.HashSet[Long] = new java.util.HashSet[Long]()

  private final val eventIdMapInitSize = 32
  private val eventTypeNameMap = new scala.collection.mutable.LongMap[String](eventIdMapInitSize)
  private final val defaultPrettyprint = (_: Long) => (arg: Long) => f"${arg}%016x"
  private val eventPrettyprintFromArg = new scala.collection.mutable.LongMap[Long => String](defaultPrettyprint, eventIdMapInitSize)
  private val eventPrettyprintToArg = new scala.collection.mutable.LongMap[Long => String](defaultPrettyprint, eventIdMapInitSize)

  private final val traceBufferTL = if (traceOn) new ThreadLocal[TraceBuffer]() {
    override protected def initialValue = new TraceBuffer(Thread.currentThread().getId)
  } else null

  def registerEventTypeId(eventTypeId: Long, eventTypeName: String): Unit = synchronized {
    require(eventTypeName.length == 8, s"Tracer event type ID ${eventTypeId}'s name must be 8 characters.  Got '${eventTypeName}'")
    val oldVal = eventTypeNameMap.put(eventTypeId, eventTypeName)
    require(oldVal == None, s"Multiple registration of event type ID $eventTypeId: '${oldVal.get}' and '${eventTypeName}'")
  }

  def registerEventTypeId(eventTypeId: Long, eventTypeName: String, prettyprintFromArg: Long => String): Unit = synchronized {
    registerEventTypeId(eventTypeId, eventTypeName)
    eventPrettyprintFromArg.put(eventTypeId, prettyprintFromArg)
  }

  def registerEventTypeId(eventTypeId: Long, eventTypeName: String, prettyprintFromArg: Long => String, prettyprintToArg: Long => String): Unit = synchronized {
    registerEventTypeId(eventTypeId, eventTypeName, prettyprintFromArg)
    eventPrettyprintToArg.put(eventTypeId, prettyprintToArg)
  }

  @inline
  def trace(eventTypeId: Long, eventLocationId: Long, eventFromArg: Long, eventToArg: Long) {
    if (traceOn) traceBufferTL.get().add(eventTypeId, eventLocationId, System.currentTimeMillis, System.nanoTime, eventFromArg, eventToArg)
  }

  @inline
  private final class TraceBuffer(val javaThreadId: Long) {
    final val size = 1048576
    final val typeIds = new Array[Long](size)
    final val locationIds = new Array[Long](size)
    final val millitimes = new Array[Long](size)
    final val nanotimes = new Array[Long](size)
    final val fromArgs = new Array[Long](size)
    final val toArgs = new Array[Long](size)

    final var nextWriteIndex = 0

    def add(eventTypeId: Long, eventLocationId: Long, eventTimeMillis: Long, eventTimeNanos: Long, eventFromArg: Long, eventToArg: Long): Unit = {
      this synchronized {
        typeIds(nextWriteIndex) = eventTypeId
        locationIds(nextWriteIndex) = eventLocationId
        millitimes(nextWriteIndex) = eventTimeMillis
        nanotimes(nextWriteIndex) = eventTimeNanos
        fromArgs(nextWriteIndex) = eventFromArg
        toArgs(nextWriteIndex) = eventToArg
        nextWriteIndex = (nextWriteIndex + 1) //% size
      }
    }

    TraceBufferDumpThread.register(this)
  }

  def selectLocation(locID: Long) = synchronized {
    selectedLocations.add(locID)
  }

  private object TraceBufferDumpThread extends Thread("TraceBufferDumpThread") {
    private val buffers = scala.collection.mutable.Set[TraceBuffer]()
    override def run = synchronized {
      val a = System.err
      /* Convention: synchronize on a during output of block */
      a synchronized {
        a.append(s"Trace Buffer: begin\n")
        a.append(s"-----Time-(ms)-----  -----Time-(ns)-----  -----Thread-ID-----  -Token/Group-ID-  EvntType  ------From------  -------To-------\n")

        for (tb <- buffers) {
          Tracer synchronized {
            tb synchronized {
              for (i <- 0 to tb.nextWriteIndex - 1) {
                if (!onlyDumpSelectedLocations || selectedLocations.contains(tb.locationIds(i))) {
                  val eventTypeName = eventTypeNameMap(tb.typeIds(i))
                  val prettyFromArg = eventPrettyprintFromArg(tb.typeIds(i))(tb.fromArgs(i))
                  val prettyToArg = eventPrettyprintToArg(tb.typeIds(i))(tb.toArgs(i))
                  a.append(f"${tb.millitimes(i)}%19d  ${tb.nanotimes(i)}%19d  ${tb.javaThreadId}%19d  ${tb.locationIds(i)}%016x  ${eventTypeName}  ${prettyFromArg}%16s  ${prettyToArg}%16s\n")
                }
              }
            }
          }
        }
        a.append(s"Trace Buffer: end\n")
      }

      val outDir = System.getProperty("orc.executionlog.dir")
      if (outDir != null) {
        val traceCsvFile = new File(outDir, s"trace-${ManagementFactory.getRuntimeMXBean().getName()}.csv")
        assert(traceCsvFile.createNewFile(), s"Trace output file: File already exists: $traceCsvFile")
        val traceCsv = new OutputStreamWriter(new FileOutputStream(traceCsvFile), "UTF-8")
        val csvWriter = new CsvWriter(traceCsv.append(_))
        val tableColumnTitles = Seq("Time (ms)", "Time (ns)", "Thread ID", "Token/Group ID", "Event Type", "From", "To")
        csvWriter.writeHeader(tableColumnTitles)
        for (tb <- buffers) {
          tb synchronized {
            val numRows = tb.nextWriteIndex
            val eventTypeNames = tb.typeIds.view(0, numRows).map(eventTypeNameMap(_));
            val rows = new JoinedColumnsTraversable(numRows, tb.millitimes, tb.nanotimes, new ConstantSeq(tb.javaThreadId, numRows), tb.locationIds, eventTypeNames, tb.fromArgs, tb.toArgs)
            csvWriter.writeRows(rows)
          }
        }
        traceCsv.close()
      }
    }

    def register(tb: TraceBuffer) = synchronized {
      buffers += tb
    }
  }

  if (traceOn) Runtime.getRuntime().addShutdownHook(TraceBufferDumpThread)

}

private class ConstantSeq[A](val value: A, override val length: Int) extends Seq[A] {
  override def apply(idx: Int): A = value
  override def iterator: Iterator[A] = Iterator.fill(length)(value)
}

private class JoinedColumnsTraversable(override val size: Int, columns: Seq[Any]*) extends Traversable[Product] {
  def foreach[U](f: Product => U): Unit = {
    scala.Product10
    for (i <- 0 to size - 1) {
      f(new ColumnProduct(i, columns))
    }
  }

  override def isEmpty: Boolean = size == 0

  class ColumnProduct(index: Int, columns: Seq[Seq[Any]]) extends Product {
    override def productElement(n: Int): Any = columns(n)(index)
    override def productArity: Int = columns.size
    override def canEqual(that: Any): Boolean = that.isInstanceOf[ColumnProduct]
  }
}
