//
// porcEBenchmarkExperiments.scala -- Scala objects implementing various benchmarking experiments of PorcE
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.io.File

import orc.test.util.FactorDescription

object PorcEShared {
  val mainOrcBenchmarks = Seq(
            "test_data/performance/threads.orc",
//            "test_data/performance/threadring2.orc",
            //"test_data/performance/Wide.orc",
            "test_data/performance/black-scholes/black-scholes-scala-opt.orc",
            "test_data/performance/black-scholes/black-scholes-scala.orc",
            "test_data/performance/black-scholes/black-scholes-opt.orc",
            "test_data/performance/black-scholes/black-scholes.orc",
            "test_data/performance/k-means/k-means-scala.orc",
            "test_data/performance/k-means/k-means-scala-opt.orc",
            "test_data/performance/k-means/k-means.orc",
            "test_data/performance/k-means/k-means-opt.orc",
            "test_data/performance/bigsort/bigsort-scala.orc",
            "test_data/performance/bigsort/bigsort.orc",
            "test_data/performance/bigsort/bigsort-scala-opt.orc",
            "test_data/performance/bigsort/bigsort-opt.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-swaption.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim-opt.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            "test_data/performance/swaptions/swaptions-naive-opt.orc",
            "test_data/performance/swaptions/swaptions-naive.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-subroutines-opt.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
//            "test_data/performance/sssp/sssp-batched-opt.orc",
//            "test_data/performance/sssp/sssp-batched.orc",
//            "test_data/performance/sssp/sssp-batched-scala-opt.orc",
//            "test_data/performance/sssp/sssp-batched-scala.orc",
            //"test_data/performance/canneal/canneal-naive.orc",
            //"test_data/performance/canneal/canneal-partitioned.orc",
            //"test_data/performance/dedup/dedup-boundedchannel.orc",
            "test_data/performance/dedup/dedup-opt.orc",
            "test_data/performance/dedup/dedup.orc",
            "test_data/performance/dedup/dedup-scala-opt.orc",
            "test_data/performance/dedup/dedup-scala.orc",
            //"test_data/performance/fp-growth/fp-growth.orc",
            "test_data/performance/map-reduce/wordcount-mixed-orc-java.orc",
            "test_data/performance/map-reduce/wordcount-mixed-orc-java-opt.orc",
            "test_data/performance/map-reduce/wordcount-pure-orc.orc",
            "test_data/performance/map-reduce/wordcount-pure-orc-opt.orc",
            "test_data/performance/n-queens/8-queens.orc",
            "test_data/performance/n-queens/8-queens-opt.orc",
            "test_data/performance/sieve/savina_sieve.orc",
            "test_data/performance/sieve/savina_sieve-opt.orc",
            "test_data/performance/sieve/savina_sieve-scala.orc",
            "test_data/performance/sieve/savina_sieve-scala-opt.orc",
            )
  private val nonOpt = Seq(
            "test_data/performance/threads.orc",
            "test_data/performance/threadring2.orc",
            //"test_data/performance/Wide.orc",
            )
  private val externalLanguages = Seq("scala", "java")
  val mainPureOrcBenchmarks = mainOrcBenchmarks.filterNot(fn => externalLanguages.exists(fn contains _))

  val mainOrcScalaBenchmarks = mainOrcBenchmarks.filter(fn => externalLanguages.exists(fn contains _))

  def onlyOpt(s: Seq[String]): Seq[String] = s.filter(fn => (fn contains "opt") || nonOpt.exists(fn contains _))
  def onlyNonOpt(s: Seq[String]): Seq[String] = s.filter(fn => !(fn contains "opt") || nonOpt.exists(fn contains _))

  val mainScalaBenchmarks = Seq(
            //orc.test.item.scalabenchmarks.Mandelbrot,
            orc.test.item.scalabenchmarks.NQueens,
            orc.test.item.scalabenchmarks.SavinaSieve,
//            orc.test.item.scalabenchmarks.ThreadRing,
            orc.test.item.scalabenchmarks.blackscholes.BlackScholesPar,
            orc.test.item.scalabenchmarks.kmeans.KMeansPar,
            //orc.test.item.scalabenchmarks.kmeans.KMeansParManual,
            orc.test.item.scalabenchmarks.BigSortPar,
            orc.test.item.scalabenchmarks.swaptions.SwaptionsParTrial,
            //orc.test.item.scalabenchmarks.swaptions.SwaptionsParSwaption,
//            orc.test.item.scalabenchmarks.sssp.SSSPBatchedPar,
            //orc.test.item.scalabenchmarks.canneal.Canneal,
            orc.test.item.scalabenchmarks.dedup.DedupNestedPar,
            //orc.test.item.scalabenchmarks.dedup.DedupBoundedQueue,
            //orc.test.item.scalabenchmarks.Threads, // Don't bother running. It will time out.
            orc.test.item.scalabenchmarks.WordCount,
            )

  val mainJvmOpts = Seq(
      "-javaagent:../ScalaGraalAgent/build/ScalaGraalAgent-0.1.jar",
      "-XX:-RestrictContended",
      "-XX:+UseParallelGC",
      "-XX:+UseNUMA",
      //"-XX:+UseG1GC",
      "-Xms16g", "-Xmx16g", "-Xss8m")

  val mainOrcArgs = Seq(
      "-O", "3", "--echo-ir", "0x1000",
      "--opt-opt", "orct:sequentialize-force",
      "--max-site-threads=16")

  val mainSystemProperties = Map[String, Any](
        "graal.TruffleBackgroundCompilation" -> true,
        "graal.TraceTruffleCompilation" -> true,
        "graal.TruffleCompilationThreshold" -> 300,
        //"graal.TruffleMaximumRecursiveInlining" -> 10,
        "graal.TruffleInliningMaxCallerSize" -> 200,
        "graal.InliningDepthError" -> 5000,
        //"graal.MaximumInliningSize" -> 5000,
        //"graal.TrivialInliningSize" -> 1000,
        //"graal.MaximumDesiredSize" -> 30000,
        //"graal.SmallCompiledLowLevelGraphSize" -> 1500,

        "orc.numerics.preferLP" -> true,
        "orc.porce.maxStackDepth" -> 128,
        //"orc.porce.optimizations.knownSiteSpecialization" -> true,
        //"orc.porce.optimizations.environmentCaching" -> false,
        "orc.ast.generateUniqueVariableNames" -> true,
        "orc.porce.truffleASTInlining" -> true,
        "orc.porce.truffleASTInliningLimit" -> 10000,
        //"orc.porce.universalTCO" -> true,
        //"orc.porce.useExternalCallKindDecision" -> true,
        "orc.porce.useControlledParallelism" -> true,
        "orc.porce.useRepTime" -> true,
        )



  trait HasRunNumber {
    def run: Int
  }

}

object PorcEStrongScalingExperiment extends PorcEBenchmark {
  import PorcEShared._

  def softTimeLimit: Double = 60 * 16

  case class MyPorcEExperimentalCondition(
      run: Int,
      orcFile: File,
      nCPUs: Int,
      optLevel: Int)
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition with HasRunNumber {
    override def nRuns = super.nRuns

    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("optLevel", "Optimization Level", "", "-O level"),
    )

    override def systemProperties = super.systemProperties ++ mainSystemProperties
    override def toOrcArgs = super.toOrcArgs ++ mainOrcArgs ++ Seq("-O", optLevel.toString)
    override def toJvmArgs = mainJvmOpts ++ super.toJvmArgs
  }

  case class MyScalaExperimentalCondition(
      run: Int,
      benchmarkClass: Class[_],
      nCPUs: Int)
      extends ArthursBenchmarkEnv.ScalaExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition with HasRunNumber {
    override def nRuns = super.nRuns / 2 max 1

    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("benchmarkClass", "Benchmark Class", "", "The class run for this benchmark"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use")
    )

    override def toJvmArgs = mainJvmOpts ++ super.toJvmArgs ++ Seq(
        s"-Dorc.test.benchmark.softTimeLimit=${softTimeLimit / 2}",
        s"-Dorc.test.benchmark.hardTimeLimit=${hardTimeLimit / 2}",
        )

  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = Seq(16, 1, 8)
      //val nCPUsValues = Seq(16, 1, 8, 12, 4)
      //val nCPUsValues = Seq(24, 12, 6, 18, 1)
      //val nCPUsValues = Seq(24, 16, 1, 8)
      val nRuns = 1
      val porce = for {
        run <- 0 until nRuns
        optLevel <- Seq(3, 0)
        nCPUs <- if (optLevel < 2) nCPUsValues.take(1) else nCPUsValues
        fn <- if (optLevel < 2) onlyOpt(mainPureOrcBenchmarks) else onlyOpt(mainOrcBenchmarks)
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(run, new File("OrcTests/" + fn), nCPUs, optLevel)
      }
      val scala = for {
        run <- 0 until 1
        nCPUs <- nCPUsValues
        benchmark <- mainScalaBenchmarks
      } yield {
        val cls = Class.forName(benchmark.getClass.getCanonicalName.stripSuffix("$"))
        MyScalaExperimentalCondition(run, cls, nCPUs)
      }
      (porce ++ scala).sortBy(v => (v.run,
          nCPUsValues.indexOf(v.nCPUs),
          (v match {
            case v: MyPorcEExperimentalCondition => -v.optLevel
            case _ => -0
          }),
          ))
    }
    runExperiment(experimentalConditions)
  }
}


object PorcEKnownSiteSpecializationExperiment extends PorcEBenchmark {
  import PorcEShared._

  def softTimeLimit: Double = 60 * 7

  case class MyPorcEExperimentalCondition(
      run: Int,
      orcFile: File,
      nCPUs: Int,
      knownSiteSpecialization: Boolean)
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition with HasRunNumber {
    override def nRuns = super.nRuns

    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("knownSiteSpecialization", "Specialize known sites", "", ""),
    )

    override def systemProperties = super.systemProperties ++ mainSystemProperties ++
      Map("orc.porce.optimizations.knownSiteSpecialization" -> knownSiteSpecialization
          )
    override def toOrcArgs = super.toOrcArgs ++ mainOrcArgs
    override def toJvmArgs = mainJvmOpts ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val porce = for {
        cpus <- Seq(8) //Seq(8, 24)
        fn <- onlyOpt(mainPureOrcBenchmarks)
        knownSiteSpecialization <- Seq(true) // Seq(true, false)
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(0, new File("OrcTests/" + fn), cpus, knownSiteSpecialization)
      }
      porce
    }
    runExperiment(experimentalConditions)
  }
}


object PorcESpawnFutureExperiment extends PorcEBenchmark {
  import PorcEShared._

  def softTimeLimit: Double = 60 * 8

  case class MyPorcEExperimentalCondition(
      run: Int,
      orcFile: File,
      nCPUs: Int,
      optLevel: Int,
      spawnLimit: Double,
      optFutures: Boolean)
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition with HasRunNumber {
    override def nRuns = super.nRuns

    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("optLevel", "Optimization Level", "", "-O level"),
      FactorDescription("spawnLimit", "Spawn Time Limit", "", ""),
      FactorDescription("optFutures", "Optimize Futures", "", ""),
    )

    val spawnsOptOpt = s"porc:eta-spawn-reduce=true" // ${spawnLimit > 0}"
    val spawnsProperties = Map(
          "orc.porce.inlineAverageTimeLimit" -> spawnLimit,
          "orc.porce.allowSpawnInlining" -> true) // (spawnLimit > 0))
    val futuresOptOpt = s"orct:future-elim=$optFutures,orct:unused-future-elim=$optFutures,orct:future-force-elim=$optFutures,porc:usegraft=$optFutures"

    override def systemProperties = super.systemProperties ++ mainSystemProperties ++ spawnsProperties
    override def toOrcArgs = super.toOrcArgs ++ mainOrcArgs ++ Seq("-O", optLevel.toString, "--opt-opt", spawnsOptOpt + "," + futuresOptOpt)

    override def toJvmArgs = mainJvmOpts ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = Seq(24)
      val porce = for {
        run <- 0 until 2
        nCPUs <- if (run < 1) nCPUsValues else nCPUsValues.filterNot(_ < 12)
        optLevel <- Seq(3)
        fn <- onlyOpt(mainPureOrcBenchmarks).toSeq.sorted
        (optSpawns, optFutures) <- Seq((true, true))// Seq((false, false), (true, false), (true, true))
        spawnLimit <- Seq(0.001, 0.01, 1, 10, 100, 1000, 10000, 0, 0.1)
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(run, new File("OrcTests/" + fn), nCPUs, optLevel, spawnLimit, optFutures)
      }
      val lasts = Set(0, 0.1)
      porce.sortBy(c => (lasts contains c.spawnLimit))
    }
    runExperiment(experimentalConditions)
  }
}

object PorcEOptimizationExperiment extends PorcEBenchmark {
  import PorcEShared._

  // Run for a short time (4 min). But if one rep take 30 minutes, let it go, since we HAVE to finish at least one rep to get data.
  def softTimeLimit: Double = 60 * 4
  override def hardTimeLimit: Double = 60 * 30

  case class MyPorcEExperimentalCondition(
      run: Int,
      orcFile: File,
      nCPUs: Int,
      optLevel: Int)
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def nRuns = super.nRuns max 30

    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("optLevel", "Optimization Level", "", "-O level"),
    )

    override def systemProperties = super.systemProperties ++ mainSystemProperties
    override def toOrcArgs = super.toOrcArgs ++ mainOrcArgs ++ Seq("-O", optLevel.toString)
    override def toJvmArgs = mainJvmOpts ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUs = 24
      val porce = for {
        fn <- onlyOpt(mainPureOrcBenchmarks)
        optLevel <- Seq(3, 0)
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(0, new File("OrcTests/" + fn), nCPUs, optLevel)
      }
      porce
    }
    runExperiment(experimentalConditions)
  }
}

object PorcEInliningTCOExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 3.5
  //override def hardTimeLimit: Double = 60 * 5.5

  case class MyPorcEExperimentalCondition(
      orcFile: File,
      nCPUs: Int,
      allowAllSpawnInlining: Boolean,
      universalTCO: Boolean,
      actuallySchedule: Boolean,
      optLevel: Int)
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("allowAllSpawnInlining", "Allow All Spawn Inlining", "", ""),
      FactorDescription("universalTCO", "Use universal TCO", "", ""),
      FactorDescription("actuallySchedule", "Allow scheduling in potentiallySchedule", "", "When false spawning only happens at spawn."),
      FactorDescription("optLevel", "Optimization Level", "", "-O level"),
    )

    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> "false",
        "orc.porce.allowSpawnInlining" -> true,
        "orc.porce.allowAllSpawnInlining" -> allowAllSpawnInlining,
        "orc.numerics.preferLP" -> "true",
        "orc.porce.universalTCO" -> universalTCO,
        "orc.porce.actuallySchedule" -> actuallySchedule,
        "graal.TruffleCompilationThreshold" -> 150,
        )

    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", optLevel.toString)

    override def toJvmArgs = Seq("-XX:+UseParallelGC", "-Xms6g", "-Xmx64g") ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = (Seq(24)).reverse
      val porce = for {
        optLevel <- Seq(3)
        fn <- Seq(
//            "test_data/performance/Mandelbrot.orc",
            //"test_data/performance/8-queens.orc",
            //"test_data/performance/threadring.orc",
            //"test_data/performance/threadring2.orc",
            //"test_data/performance/black-scholes/black-scholes-partitioned-seq.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute.orc",
//            "test_data/performance/black-scholes/black-scholes-scala-compute-partitioned-seq.orc",
//            "test_data/performance/black-scholes/black-scholes.orc",
            //"test_data/performance/k-means/k-means-scala-inner.orc",
            //"test_data/performance/k-means/k-means.orc",
            //"test_data/performance/bigsort/bigsort.orc",
            //"test_data/performance/bigsort/bigsort-partially-seq.orc",
//            "test_data/performance/swaptions/swaptions-naive-scala-swaption.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
//            "test_data/performance/swaptions/swaptions-naive-scala-subroutines-seq.orc",
//            "test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            "test_data/performance/sssp/sssp-batched-partitioned.orc",
//            "test_data/performance/sssp/sssp-batched.orc",
            //"test_data/performance/canneal/canneal-naive.orc",
            //"test_data/performance/canneal/canneal-partitioned.orc",
            "test_data/performance/dedup/dedup-boundedchannel.orc",
//            "test_data/performance/dedup/dedup.orc",
            )
        nCPUs <- nCPUsValues
        allowAllSpawnInlining <- Seq(true, false)
        universalTCO <- Seq(true, false)
        actuallySchedule <- Seq(true, false)
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(new File("OrcTests/" + fn), nCPUs, allowAllSpawnInlining, universalTCO, actuallySchedule, optLevel)
      }
      porce
    }
    runExperiment(experimentalConditions)
  }
}


object PorcEDevelopmentImprovementExperiment extends PorcEBenchmark {
  import PorcEShared._

  def softTimeLimit: Double = 60 * 10
  override def hardTimeLimit: Double = 60 * 14

  trait Option extends Product with Serializable {
    def optopt(enabled: Boolean): Map[String, String] = Map()
    def sysprop(enabled: Boolean): Map[String, String] = Map()
  }

  case class BooleanOptOpt(name: String, enabledState: Boolean) extends Option {
    override def optopt(enabled: Boolean): Map[String, String] = {
      Map(name -> (if(enabled) enabledState else !enabledState).toString)
    }
  }

  case class StringOptOpt(name: String, enabledState: String, disabledState: String) extends Option {
    override def optopt(enabled: Boolean): Map[String, String] = {
      Map(name -> (if(enabled) enabledState else disabledState))
    }
  }

  case class BooleanSysProp(name: String, enabledState: Boolean) extends Option {
    override def sysprop(enabled: Boolean): Map[String, String] = {
      Map(name -> (if(enabled) enabledState else !enabledState).toString)
    }
  }

  case class StringSysProp(name: String, enabledState: String, disabledState: String) extends Option {
    override def sysprop(enabled: Boolean): Map[String, String] = {
      Map(name -> (if(enabled) enabledState else disabledState))
    }
  }

  object Orctimizer {
    val PeepholeOptimizations = Seq(
      BooleanOptOpt("orct:branch-elim", true),
      BooleanOptOpt("orct:branch-elim-arg", true),
      BooleanOptOpt("orct:branch-elim-const", true),
      BooleanOptOpt("orct:otherwise-elim", true),
      BooleanOptOpt("orct:stop-equiv", true),
      BooleanOptOpt("orct:stop-elim", true),
      BooleanOptOpt("orct:constant-propogation", true),
      BooleanOptOpt("orct:getmethod-elim", true),
      BooleanOptOpt("orct:trim-elim", true),
      BooleanOptOpt("orct:tuple-elim", true),
      BooleanOptOpt("orct:accessor-elim", true),
      BooleanOptOpt("orct:flag-elim", true),
      BooleanOptOpt("orct:ifdef-elim", true),
      BooleanOptOpt("orct:method-elim", true),
      BooleanOptOpt("orct:tuple-elim", true),
      )

    val Inlining = Seq(
      BooleanOptOpt("orct:inline", true),
      )

    // Future Optimizations:

    val ForceElimination = Seq(
      BooleanOptOpt("orct:force-elim", true),
      BooleanOptOpt("orct:resolve-elim", true),
      BooleanOptOpt("orct:lift-force", true),
      )

    val FutureElimination = Seq(
      BooleanOptOpt("orct:future-elim", true),
      BooleanOptOpt("orct:unused-future-elim", true),
      BooleanOptOpt("orct:future-force-elim", true),
      )
  }


  object Porc {
    val PeepholeOptimizations = Seq(
      BooleanOptOpt("porc:eta-spawn-reduce", true),
      BooleanOptOpt("porc:try-catch-elim", true),
      BooleanOptOpt("porc:try-finally-elim", true),
      )

    val Inlining = Seq(
      BooleanOptOpt("porc:inline-let", true),
      BooleanOptOpt("porc:eta-reduce", true),
      )
  }


  val LimitedPrecision = Seq(
    BooleanSysProp("orc.numerics.preferLP", true),
    )

  object PorcE {
    // Reduce scheduling overhead by direct calling on the stack:
    val OccationallySchedule = Seq(
        BooleanSysProp("orc.porce.occationallySchedule", true),
        )

    // Allow inline some spawns into there spawn site instead of calling them:
    val AllowSpawnInlining = Seq(
        BooleanOptOpt("porce:inline-spawn", true),
        )

    // Only inlining fast tasks (based on runtime profiling):
    val InlineAverageTimeLimit = Seq(
        StringOptOpt("porce:inline-average-time-limit", "0.1", "0.0000001"),
        )

    // Polymorphic inline caches for calls:
    val PolyInlineCaches = Seq(
        BooleanOptOpt("porce:polymorphic-inline-caching", true)
      )

    // Specialize compiled code for runtime states such as futured already being resolved:
    val SpecializeOnRuntimeStates = Seq(
        BooleanOptOpt("porce:specialization", true),
      )

    // Optimized TCO (instead of using trampolining through the scheduler):
    val OptimizedTCO = Seq(
      BooleanSysProp("orc.porce.universalTCO", true),
      BooleanSysProp("orc.porce.selfTCO", true),
      )

    // Experimental aggressive optimizations:
    val TruffleASTInlining = Seq(
        BooleanSysProp("orc.porce.truffleASTInlining", true),
        )
    val AllowAllSpawnInlining = Seq(
        BooleanSysProp("orc.porce.allowAllSpawnInlining", true),
        )
  }

  val LastUsedStep = Seq()

  val Steps = Seq(
      //Orctimizer.PeepholeOptimizations ++ Porc.PeepholeOptimizations,
      //Orctimizer.Inlining ++ Porc.Inlining,
      //Orctimizer.ForceElimination,
      //Orctimizer.FutureElimination,
      // PorcE.OccationallySchedule,
      PorcE.PolyInlineCaches,
      PorcE.AllowSpawnInlining ++ PorcE.InlineAverageTimeLimit,
      PorcE.SpecializeOnRuntimeStates,
      //LimitedPrecision,
      LastUsedStep,
      //PorcE.OptimizedTCO,
      )

  case class MyPorcEExperimentalCondition(
      orcFile: File,
      nCPUs: Int,
      step: Int)
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("step", "Development step", "", ""),
    )

    val enabledOptions = Steps.take(step).flatten
    val disabledOptions = Steps.drop(step).flatten

    override def systemProperties = super.systemProperties ++ mainSystemProperties ++
        enabledOptions.flatMap(_.sysprop(true)) ++
        disabledOptions.flatMap(_.sysprop(false))

    def optoptString = {
      val opts = (enabledOptions.flatMap(_.optopt(true)) ++ disabledOptions.flatMap(_.optopt(false)))
      opts.map({ case (n, v) => s"$n=$v"}).mkString(",")
    }

    override def toOrcArgs = super.toOrcArgs ++ mainOrcArgs ++ Seq("-O", "3", "--opt-opt", optoptString)
    override def toJvmArgs = mainJvmOpts ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = Seq(24)
      val porce = for {
        nCPUs <- nCPUsValues
        fn <- onlyOpt(mainOrcBenchmarks)
        /*onlyOpt(mainOrcScalaBenchmarks) ++ Seq(
            "test_data/performance/dedup/dedup-opt.orc",
            "test_data/performance/threads.orc",
            "test_data/performance/threadring2.orc",
            )*/
        step <- 0 to Steps.indexOf(LastUsedStep)
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(new File("OrcTests/" + fn), nCPUs, step)
      }

      /*for(c <- porce) {
        println(c)
        println(c.systemProperties.toSeq.sortBy(_._1).map({ case (n, v) => s"$n=$v"}).mkString(", "))
        println(c.optoptString)
      }*/
      porce
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
