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

import java.io.OutputStreamWriter
import java.lang.IndexOutOfBoundsException

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

  //TODO: "Who wants to be a macro?" "Oooh, pick me, pick me!!"

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceOn = true

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
    if (traceOn) {
      try {
        traceBufferTL.get().add(eventTypeId, eventLocationId, System.currentTimeMillis, System.nanoTime, eventFromArg, eventToArg)
      } catch {
        case _: IndexOutOfBoundsException =>
          val n = new TraceBuffer(Thread.currentThread().getId)
          n.add(eventTypeId, eventLocationId, System.currentTimeMillis, System.nanoTime, eventFromArg, eventToArg)
          traceBufferTL.set(n)
      }
    }
  }

  @inline
  final class TraceBuffer(val javaThreadId: Long) {
    final val size = 1048576
    final var typeIds = new Array[Long](size)
    final var locationIds = new Array[Long](size)
    final var millitimes = new Array[Long](size)
    final var nanotimes = new Array[Long](size)
    final var fromArgs = new Array[Long](size)
    final var toArgs = new Array[Long](size)

    final private var nextWriteIndex = 0
    final var eventsInBuffer = 0

    def add(eventTypeId: Long, eventLocationId: Long, eventTimeMillis: Long, eventTimeNanos: Long, eventFromArg: Long, eventToArg: Long): Unit = this synchronized {
      if (nextWriteIndex >= size) {
        throw new IndexOutOfBoundsException()
      }
      typeIds(nextWriteIndex) = eventTypeId
      locationIds(nextWriteIndex) = eventLocationId
      millitimes(nextWriteIndex) = eventTimeMillis
      nanotimes(nextWriteIndex) = eventTimeNanos
      fromArgs(nextWriteIndex) = eventFromArg
      toArgs(nextWriteIndex) = eventToArg
      nextWriteIndex = (nextWriteIndex + 1) // % size
    }

    /** Close this buffer so it will not accept any more events.
      *
      * This will cause the buffer to be replaced next time an event
      * would be added to it.
      */
    def close() = this synchronized {
      eventsInBuffer = nextWriteIndex
      nextWriteIndex = size
    }
    
    def dispose() = this synchronized {
      typeIds = null
      locationIds = null
      millitimes = null
      nanotimes = null
      fromArgs = null
      toArgs = null
    }

    register(this)
  }

  def selectLocation(locID: Long) = synchronized {
    selectedLocations.add(locID)
  }

  private var buffers = scala.collection.mutable.Set[TraceBuffer]()

  /** Return the current set of TraceBuffers and reset tracing.
    * 
    * After this is called all new events will go in new buffers
    * so the returned buffers will not change.
    *
    * This is mostly thread safe w.r.t. adding trace events. However,
    * the dump point is not atomic meaning that events can appear in
    * the dump even if they are caused by events which appear after
    * this dump in another thread. This is because, while there is a
    * atomic dump point for each thread, those points are not
    * synchronized between threads.
    *
    * While this method is running tracing may block in any thread.
    * In general this method should be called when no important
    * tracing is happening.
    */
  def takeBuffers(): collection.Set[TraceBuffer] = synchronized {
    val oldBuffers = buffers
    
    // The point at which events are "after this dump" in a thread is somewhere between the 
    // reassignment of buffers and the call to close on the buffer in use by that thread.
    buffers = scala.collection.mutable.Set[TraceBuffer]()    
    oldBuffers.foreach(_.close())
    
    oldBuffers
  }

  /** Dump the trace buffers to the terminal and to files.
    *
    * This is mostly thread safe w.r.t. adding trace events. However,
    * the dump point is not atomic meaning that events can appear in
    * the dump even if they are caused by events which appear after
    * this dump in another thread. This is because, while there is a
    * atomic dump point for each thread, those points are not
    * synchronized between threads.
    *
    * While this method is running tracing will block in all threads.
    * In general this method should be called when no important
    * tracing is happening.
    */
  def dumpBuffers(suffix: String) = synchronized {
    val oldBuffers = takeBuffers()
    
    if (false) {
      val a = System.err
      /* Convention: synchronize on a during output of block */
      a synchronized {
        a.append(s"Trace Buffer: begin $suffix\n")
        a.append(s"-----Time-(ms)-----  -----Time-(ns)-----  -----Thread-ID-----  -Token/Group-ID-  EvntType  ------From------  -------To-------\n")
  
        for (tb <- oldBuffers) {
          Tracer synchronized {
            tb synchronized {
              for (i <- 0 to tb.eventsInBuffer - 1) {
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
        a.append(s"Trace Buffer: end $suffix\n")
      }
    }

    val csvOut = ExecutionLogOutputStream(s"trace-$suffix", "csv", "Trace output file")
    if (csvOut.isDefined && oldBuffers.nonEmpty && oldBuffers.exists(_.eventsInBuffer > 0)) {
      val traceCsv = new OutputStreamWriter(csvOut.get, "UTF-8")
      val csvWriter = new CsvWriter(traceCsv.append(_))
      val tableColumnTitles = Seq("Absolute Time (ms) [absTime]", "Precise Time (ns) [time]", "Thread ID [threadId]", "Token/Group ID [sourceId]", "Event Type [type]", "From [from]", "To [to]")
      csvWriter.writeHeader(tableColumnTitles)
      for (tb <- oldBuffers) {
        tb synchronized {
          val numRows = tb.eventsInBuffer
          val eventTypeNames = tb.typeIds.view(0, numRows).map(eventTypeNameMap(_).trim)
          val rows = new JoinedColumnsTraversable(numRows, tb.millitimes, tb.nanotimes, new ConstantSeq(tb.javaThreadId, numRows), tb.locationIds, eventTypeNames, tb.fromArgs, tb.toArgs)
          csvWriter.writeRows(rows)
        }
      }
      traceCsv.close()
    }
  }

  DumperRegistry.register(dumpBuffers)

  private def register(tb: TraceBuffer) = synchronized {
    buffers += tb
  }

  if (traceOn) Runtime.getRuntime().addShutdownHook(new Thread(() => dumpBuffers("shutdown"), "TraceBufferDumpThread"))
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
