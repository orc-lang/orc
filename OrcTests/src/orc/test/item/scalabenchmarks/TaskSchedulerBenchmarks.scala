//
// TaskSchedulerBenchmarks.scala -- Scala experimental benchmarks TaskSchedulerBenchmarks
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import java.util.Scanner
import java.util.concurrent.{ ConcurrentHashMap, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit, TimeoutException }
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConverters.mapAsScalaConcurrentMapConverter
import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration.Duration

import orc.Schedulable
import orc.run.extensions.{ OrcThreadPoolExecutor, SimpleWorkStealingScheduler }

//  -XX:StartFlightRecording=delay=2s,dumponexit=true,name=ThreadPoolExecutor_${current_date},filename=ThreadPoolExecutor_${current_date}.jfr,settings=profile

/*
object JFREvents {
  val PRODUCER_URI = "http://www.smx.com/smx/";
  val PRODUCER = {
    try {
      new Producer("Smurfberry Exchange producer",
        "A demo event producer for the fictional Smurfberry Exchange.", PRODUCER_URI);
    } catch {
      case e: Exception =>
        // Add proper exception handling.
        e.printStackTrace()
        null
    }
  }

  @EventDefinition(path = "orc/microbenchmarks/testtask", name = "Count Task Run")
  class CountTaskRun(eventToken: EventToken,
    @ValueDefinition(name = "Call #") var callNumber: Int,
    @ValueDefinition(name = "Task ID") var taskId: Long) extends TimedEvent(eventToken) {

  }

  object CountTaskRun {
    val CountTaskRunToken = PRODUCER.addEvent(classOf[CountTaskRun]);

    def apply() = {

      new CountTaskRun(CountTaskRunToken, 0, 0)
    }
  }
}
*/

object TaskSchedulerBenchmarks {
  trait Task extends Schedulable {
    override val nonblocking = true
  }

  trait TaskSchedulerBenchmark {

    def startScheduler(): Unit
    def stopScheduler(): Unit
    def schedule(t: Task): Unit

    def allowBlock[T](f: => T): T

    val allTasks = new ConcurrentHashMap[CountTask, Unit]()
    val nBlockingEvents = new AtomicInteger(0)

    case class CountTask(var calls: Int, val i: Seq[Int], val splitAt: Set[Int], val blockAt: Set[Int]) extends Task {
      private[this] val p = Promise[Unit]()
      val future = p.future
      var subtasks = Seq[CountTask]()

      allTasks.put(this, ())

      def run(): Unit = {
        //val event = JFREvents.CountTaskRun();
        //event.begin();
        try {
          //println(s"Running $i ($calls)")
          calls -= 1
          if (blockAt.contains(calls)) {
            allowBlock {
              nBlockingEvents.getAndIncrement()
              //Thread.sleep(10)
            }
          }
          if (splitAt.contains(calls)) {
            //println(s"Splitting $i ($calls)")
            val t = copy(i = i :+ calls)
            //subtasks :+= t
            schedule(t)
          }
          if (calls > 0) {
            schedule(this)
          } else {
            p.success(())
          }
          //event.callNumber = calls
          //event.taskId = i.foldLeft(0L)(_ * 10000 + _)
        } finally {
          //event.end();
          //event.commit();
        }
      }
    }

    def main(args: Array[String]) = {
      val ntasks = args(0).toInt
      val ncalls = args(1).toInt

      startScheduler()

      for (i <- 0 until 5) {
        println(s"=================================== $i")
        allTasks.clear()
        nBlockingEvents.set(0)

        Util.timeIt {
          //dumpStats()
          val tasks = for (i <- 0 until ntasks) yield {
            val t = new CountTask(ncalls, Seq(i),
              Set() ++ (0 until ncalls by 3),
              Set(ncalls / 4, ncalls / 6, ncalls / 4 * 3, ncalls / 8 * 5, ncalls / 8 * 7))
            schedule(t)
            t
          }
          //dumpStats()

          for (f <- tasks) {
            def retry(): Unit = {
              try {
                Await.ready(f.future, Duration(5, "s"))
                //println(s"${f.i} completed")
              } catch {
                case _: TimeoutException =>
                  //dumpStats()
                  retry()
              }
            }
            retry()
          }

          for ((f, _) <- allTasks.asScala) {
            def retry(): Unit = {
              try {
                Await.ready(f.future, Duration(2, "s"))
                //println(s"${f.i} completed")
              } catch {
                case _: TimeoutException =>
                  //dumpStats()
                  retry()
              }
            }
            retry()
          }
        }

        println(s"allTasks.size = ${allTasks.size}")
        println(s"nBlockingEvents = ${nBlockingEvents.get()}")
        //dumpStats()
      }

      stopScheduler()
    }
  }

  class ThreadPoolExecutorImpl extends ThreadPoolExecutor(
    math.max(16, Runtime.getRuntime().availableProcessors * 2),
    256,
    2000L, TimeUnit.MILLISECONDS,
    //new PriorityBlockingQueue[Runnable](11, new Comparator[Runnable] { def compare(o1: Runnable, o2: Runnable) = Random.nextInt(2)-1 }),
    new LinkedBlockingQueue[Runnable](),
    new ThreadPoolExecutor.CallerRunsPolicy) {

    def dumpStats() {
      println(s"getLargestPoolSize() = ${getLargestPoolSize()}")
      println(s"getPoolSize() = ${getPoolSize()}")
      println(s"getMaximumPoolSize() = ${getMaximumPoolSize()}")
    }

    def startScheduler(): Unit = {
    }
    def stopScheduler(): Unit = {
      shutdown()
    }
    def allowBlock[T](f: => T): T = {
      f
    }
    def schedule(t: Task): Unit = {
      execute(t)
    }
  }

  class OrcThreadPoolExecutorImpl extends OrcThreadPoolExecutor("Name", 512) {
    def dumpStats() {
      println(s"getLargestPoolSize() = ${getLargestPoolSize()}")
      println(s"getPoolSize() = ${getPoolSize()}")
      println(s"getMaximumPoolSize() = ${getMaximumPoolSize()}")
    }

    val monitor = new Thread(this)

    def startScheduler(): Unit = {
      monitor.start()
    }
    def stopScheduler(): Unit = {
      shutdown()
    }
    def allowBlock[T](f: => T): T = {
      f
    }
    def schedule(t: Task): Unit = {
      execute(t)
    }
  }

  class ThreadPoolExecutorImplBenchmark extends ThreadPoolExecutorImpl with TaskSchedulerBenchmark
  class OrcThreadPoolExecutorImplBenchmark extends OrcThreadPoolExecutorImpl with TaskSchedulerBenchmark
  class SimpleWorkStealingImplBenchmark extends SimpleWorkStealingScheduler(512) with TaskSchedulerBenchmark {
    def allowBlock[T](f: => T): T = {
      potentiallyBlocking(f)
    }

    def schedule(t: Task): Unit = {
      schedule(t: Schedulable)
    }
  }

  def promptEnterKey(): Unit = {
    System.out.println("Press \"ENTER\" to continue...");
    val scanner = new Scanner(System.in);
    scanner.nextLine();
  }

  def runTest(args: Array[String]) = {
    //promptEnterKey()
    args.head match {
      case "ThreadPoolExecutor" =>
        new ThreadPoolExecutorImplBenchmark().main(args.tail)
      case "OrcThreadPoolExecutor" =>
        new OrcThreadPoolExecutorImplBenchmark().main(args.tail)
      case "SimpleWorkStealing" =>
        new SimpleWorkStealingImplBenchmark().main(args.tail)
    }
  }

  def main(args: Array[String]) = {
    //for (i <- 0 until 5) {
    //  println(s"------------ $i")
    runTest(args)
    //}
  }
}
