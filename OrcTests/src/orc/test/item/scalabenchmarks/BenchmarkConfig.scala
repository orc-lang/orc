package orc.test.item.scalabenchmarks

import java.util.Timer
import java.util.TimerTask

object BenchmarkConfig {
  /** The benchmark should use this number to change the size of the problem.
    * Ideally the benchmark should increase in runtime roughly linearly with this
    * value. However, that may not always be possible or practical.
    *
    * If a value is not specified the size is 1.
    */
  val problemSize = System.getProperty("orc.test.benchmark.problemSize", "1").toInt

  def problemSizeScaledInt(n: Int) = ((n * 1.0) * problemSize).floor.toInt
  def problemSizeLogScaledInt(n: Int) = ((n * 1.0) * (math.log(problemSize) + 1)).floor.toInt
  def problemSizeSqrtScaledInt(n: Int) = ((n * 1.0) * math.sqrt(problemSize)).floor.toInt

  /** The number of times the benchmark function will run f or zero to
    * disable benchmarking. If benchmarking is disabled then benchmark(f)
    * behaves exactly like f().
    *
    * The default value is 0.
    */
  val nRuns = System.getProperty("orc.test.benchmark.nRuns", "0").toInt

  /** The soft limit of the amount of time this benchmark will run.
    *
    * The harness will not call the benchmark again if more than
    * this number of seconds has passed.
    * 
    * If this value is negative then there is no time limit.
    *
    * The default value is no soft limit.
    */
  val softTimeLimit = System.getProperty("orc.test.benchmark.softTimeLimit", "-1").toDouble

  /** The hard limit of the amount of time this benchmark will run.
    *
    * The harness will forceably exit after this amount of time of running.
    * For the forcable exit to occure the program must call 
    * startHardLimitTimer() when the benchmarks start.
    * 
    * If this value is negative then there is no time limit.
    *
    * The default value is no hard limit.
    */
  val hardTimeLimit = System.getProperty("orc.test.benchmark.hardTimeLimit", "-1").toDouble

  /** The number of partitions or threads that should be used by the benchmarks.
    *
    * The default value is number of available CPUs * 2.
    */
  val nPartitions = Option(System.getProperty("orc.test.benchmark.nPartitions")).map(_.toInt).getOrElse(Runtime.getRuntime.availableProcessors() * 2)
  
 
  def startHardTimeLimit(): Unit = {
    if (hardTimeLimit > 0) {
      val t = new Timer(true)
      t.schedule(new TimerTask {
        def run(): Unit = {
          System.err.append("Hard time limit reached. Exiting JVM.\n")
          System.exit(124)
        }
      }, (hardTimeLimit * 1000).toLong)
    }
  }
}