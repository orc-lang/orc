package orc.test.proc

import orc.test.util.FactorDescription
import java.io.File

object PorcEStrongScalingExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 7
    
  case class MyPorcEExperimentalCondition(
      orcFile: File, 
      nCPUs: Int, 
      truffleBackgroundCompilation: Boolean, 
      allowSpawnInlining: Boolean, 
      optLevel: Int) 
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("TruffleBackgroundCompilation", "Truffle Background Compilation", "", "Should truffle compile in the background"),
      FactorDescription("allowSpawnInlining", "Allow Spawn Inlining", "", ""),
      FactorDescription("optLevel", "Optimization Level", "", "-O level")
    )
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> truffleBackgroundCompilation,
        "orc.porce.allowSpawnInlining" -> allowSpawnInlining
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", optLevel.toString)
  }
  case class MyScalaExperimentalCondition(
      benchmarkClass: Class[_], 
      nCPUs: Int) 
      extends ArthursBenchmarkEnv.ScalaExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("benchmarkClass", "Benchmark Class", "", "The class run for this benchmark"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use")
    )
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = (Seq(1, 4, 8, 16, 24, 48)).reverse 
      val porce = for {
        optLevel <- Seq(3)
        nCPUs <- nCPUsValues
        allowSpawnInlining <- Seq(true)
        fn <- Seq(
            "test_data/performance/Mandelbrot.orc",
            "test_data/performance/8-queens.orc",
            "test_data/performance/threadring.orc",
            "test_data/performance/threadring2.orc",
            "test_data/performance/black-scholes/black-scholes-partitioned-seq.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute.orc",
            "test_data/performance/black-scholes/black-scholes.orc",
            "test_data/performance/k-means/k-means-scala-inner.orc",
            "test_data/performance/k-means/k-means.orc",
            "test_data/performance/bigsort/bigsort.orc",
            "test_data/performance/bigsort/bigsort-partially-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            "test_data/performance/sssp/sssp-batched-partitioned.orc",
            "test_data/performance/canneal/canneal-naive.orc",
            "test_data/performance/canneal/canneal-partitioned.orc",
            "test_data/performance/dedup/dedup-boundedchannel.orc",
            "test_data/performance/dedup/dedup.orc",
            )
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(new File("OrcTests/" + fn), nCPUs, false, allowSpawnInlining, optLevel)
      }
      val scala = for {
        nCPUs <- nCPUsValues
        benchmark <- Seq(
            orc.test.item.scalabenchmarks.Mandelbrot,
            orc.test.item.scalabenchmarks.NQueens,
            orc.test.item.scalabenchmarks.ThreadRing,
            orc.test.item.scalabenchmarks.blackscholes.BlackScholesPar,
            orc.test.item.scalabenchmarks.kmeans.KMeansPar,
            orc.test.item.scalabenchmarks.kmeans.KMeansParManual,
            orc.test.item.scalabenchmarks.BigSortPar,
            orc.test.item.scalabenchmarks.swaptions.SwaptionsParTrial, 
            orc.test.item.scalabenchmarks.swaptions.SwaptionsParSwaption,
            orc.test.item.scalabenchmarks.sssp.SSSPBatchedPar, 
            orc.test.item.scalabenchmarks.canneal.Canneal, 
            orc.test.item.scalabenchmarks.dedup.DedupNestedPar, 
            orc.test.item.scalabenchmarks.dedup.DedupBoundedQueue, 
            )
      } yield {
        val cls = Class.forName(benchmark.getClass.getCanonicalName.stripSuffix("$"))
        MyScalaExperimentalCondition(cls, nCPUs)
      }
      porce ++ scala 
    }
    runExperiment(experimentalConditions)
  }
}



object PorcESteadyStateExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 10
    
  case class MyPorcEExperimentalCondition(
      orcFile: File, 
      truffleBackgroundCompilation: Boolean, 
      truffleCompilationThreshold: Int, 
      truffleCompilerThreads: Int) 
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("TruffleBackgroundCompilation", "Truffle Background Compilation", "", "Should truffle compile in the background"),
      FactorDescription("TruffleCompilationThreshold", "Number of runs before compilation", "", ""),
      FactorDescription("TruffleCompilerThreads", "Number of truffle compilation threads", "", ""),
    )
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> truffleBackgroundCompilation,
        "graal.TruffleCompilationThreshold" -> truffleCompilationThreshold,
        "graal.TruffleCompilerThreads" -> truffleCompilerThreads,
        )
  }
  case class MyScalaExperimentalCondition(
      benchmarkClass: Class[_]) 
      extends ArthursBenchmarkEnv.ScalaExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("benchmarkClass", "Benchmark Class", "", "The class run for this benchmark"),
    )
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val porce = for {
        bg <- Seq(false)
        threshold <- Seq(10, 150, 300, 600, 1000)
        compThreads <- Seq(24)
        fn <- Seq(
            "test_data/performance/Hamming.orc",
            "test_data/performance/threadring.orc",
            "test_data/performance/black-scholes/black-scholes.orc",
            "test_data/performance/canneal/canneal-naive.orc",
            )
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(new File("OrcTests/" + fn), bg, threshold, compThreads)
      }
      val scala = for {
        benchmark <- Seq(
            orc.test.item.scalabenchmarks.Hamming,
            orc.test.item.scalabenchmarks.ThreadRing,
            orc.test.item.scalabenchmarks.blackscholes.BlackScholesPar,
            orc.test.item.scalabenchmarks.canneal.Canneal,
            )
      } yield {
        val cls = Class.forName(benchmark.getClass.getCanonicalName.stripSuffix("$"))
        MyScalaExperimentalCondition(cls)
      }
      porce ++ scala 
    }
    runExperiment(experimentalConditions)
  }
}