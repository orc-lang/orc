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

    private final var nextWriteIndex = 0

    @inline
    def add(eventTypeId: Long, eventLocationId: Long, eventTimeMillis: Long, eventTimeNanos: Long, eventFromArg: Long, eventToArg: Long) {
      typeIds(nextWriteIndex) = eventTypeId
      locationIds(nextWriteIndex) = eventLocationId
      millitimes(nextWriteIndex) = eventTimeMillis
      nanotimes(nextWriteIndex) = eventTimeNanos
      fromArgs(nextWriteIndex) = eventFromArg
      toArgs(nextWriteIndex) = eventToArg
      nextWriteIndex = (nextWriteIndex + 1) //% size
    }

    def dump(a: Appendable) = Tracer synchronized {
      /* Convention: synchronize on System.out during output of block */
      System.out synchronized {
        a.append(f"Trace Buffer: begin: Java thread ID $javaThreadId%#x, ${nextWriteIndex.toString} entries\n")
        a.append(f"-----Time-(ms)-----  -----Time-(ns)-----  ThreadID  -Token/Group-ID-  EvntType  ------From------  -------To-------\n")

        for (i <- 0 to nextWriteIndex - 1) {
          if (!onlyDumpSelectedLocations || selectedLocations.contains(locationIds(i))) {
            val eventTypeName = eventTypeNameMap(typeIds(i))
                val prettyFromArg = eventPrettyprintFromArg(typeIds(i))(fromArgs(i))
                val prettyToArg = eventPrettyprintToArg(typeIds(i))(toArgs(i))
                a.append(f"${millitimes(i)}%19d  ${nanotimes(i)}%19d  $javaThreadId%8x  ${locationIds(i)}%016x  ${eventTypeName}  ${prettyFromArg}%16s  ${prettyToArg}%16s\n")
          }
        }
        a.append(f"Trace Buffer: end: Java thread ID $javaThreadId%#x\n")
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
      buffers.map { _.dump(System.out) }
    }
    def register(tb: TraceBuffer) = synchronized {
      buffers += tb
    }
  }

  if (traceOn) Runtime.getRuntime().addShutdownHook(TraceBufferDumpThread)

}
