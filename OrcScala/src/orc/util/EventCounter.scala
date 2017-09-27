//
// EventCounter.scala -- Scala object EventCounter
// Project OrcScala
//
// Created by jthywiss on Jul 18, 2017.
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

/** Rudimentary event counting facility.
  *
  * Each thread accumulates counts in a thread-local counting map.  At JVM
  * shutdown, the count maps are dumped to stdout.
  *
  * Counts are accumulated per "event type"s, which are [[scala.Symbol]]s,
  * using the `count` method.
  *
  * @author jthywiss
  */
object EventCounter {
  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val counterOn = true

  private final val eventCountsTL = if (counterOn) new ThreadLocal[EventCounts]() {
    override protected def initialValue = new EventCounts(Thread.currentThread().getId)
  } else null

  @inline
  def count[A](eventType: Symbol) = {
    if (counterOn) {
      eventCountsTL.get().add(eventType, 1L)
    }
  }

  @inline
  private final class EventCounts(val javaThreadId: Long) {
    final val countMap = new scala.collection.mutable.HashMap[Symbol, Array[Long]]

    @inline
    def add(eventType: Symbol, intervalCount: Long) {
      val accums = countMap.get(eventType)
      if (accums.isDefined) {
        accums.get(0) += intervalCount
      } else {
        val newAccums = new Array[Long](1)
        newAccums(0) = intervalCount
        countMap.put(eventType, newAccums)
      }
    }

    EventCountDumpThread.register(this)
  }

  private object EventCountDumpThread extends Thread("EventCountDumpThread") {
    private val accums = scala.collection.mutable.Set[EventCounts]()
    override def run = synchronized {
      val sumMap = new scala.collection.mutable.HashMap[Symbol, Array[Long]]
      var eventTypeColWidth = 14

      for (ec <- accums) {
        for (e <- ec.countMap) {
          val sums = sumMap.getOrElseUpdate(e._1, Array(0L))
          sums(0) += e._2(0)
          if (e._1.name.length > eventTypeColWidth) eventTypeColWidth = e._1.name.length
        }
      }

      /* Convention: synchronize on System.err during output of block */
      System.err synchronized {
        System.err.append(s"Event Counters: begin, ${sumMap.size} entries\n")
        System.err.append("Event-Type".padTo(eventTypeColWidth, '-'))
        System.err.append("\t-------Count--------\n")

        for (e <- sumMap) {
          System.err.append(e._1.name.padTo(eventTypeColWidth, ' '))
          System.err.append(f"\t${e._2(0)}%20d\n")
        }
        System.err.append(f"Event Counters: end\n")
      }

      val outDir = System.getProperty("orc.executionlog.dir")
      /* Create, if necessary, output directory, but only leaf directory, not full path. */
      if (outDir != null && outDir.nonEmpty) new File(outDir).mkdir()
      if (outDir != null) {
        val eventCountCsvFile = new File(outDir, s"eventCount-${ManagementFactory.getRuntimeMXBean().getName()}.csv")
        assert(eventCountCsvFile.createNewFile(), s"Event count output file: File already exists: $eventCountCsvFile")
        val eventCountCsv = new OutputStreamWriter(new FileOutputStream(eventCountCsvFile), "UTF-8")
        val csvWriter = new CsvWriter(eventCountCsv.append(_))
        val tableColumnTitles = Seq("Event Type", "Count")
        csvWriter.writeHeader(tableColumnTitles)
        val rows = sumMap.map(e => ((e._1.name,e._2(0))))
        csvWriter.writeRows(rows)
        eventCountCsv.close()
      }
    }

    def register(ec: EventCounts) = synchronized {
      accums += ec
    }
  }

  if (counterOn) Runtime.getRuntime().addShutdownHook(EventCountDumpThread)

}
