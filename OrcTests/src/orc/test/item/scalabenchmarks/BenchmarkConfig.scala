package orc.test.item.scalabenchmarks

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
}