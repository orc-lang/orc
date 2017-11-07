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
        "orc.porce.allowSpawnInlining" -> allowSpawnInlining,
        "orc.numerics.preferLP" -> "true"
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", optLevel.toString)
    
    override def toJvmArgs = Seq("-XX:+UseG1GC", "-Xms64g", "-Xmx115g") ++ super.toJvmArgs
  }
  case class MyScalaExperimentalCondition(
      benchmarkClass: Class[_], 
      nCPUs: Int) 
      extends ArthursBenchmarkEnv.ScalaExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("benchmarkClass", "Benchmark Class", "", "The class run for this benchmark"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use")
    )
    
    override def toJvmArgs = Seq("-XX:+UseG1GC", "-Xms64g", "-Xmx115g") ++ super.toJvmArgs
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
            //"test_data/performance/8-queens.orc",
            //"test_data/performance/threadring.orc",
            //"test_data/performance/threadring2.orc",
            "test_data/performance/black-scholes/black-scholes-partitioned-seq.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-partitioned-seq.orc",
            "test_data/performance/black-scholes/black-scholes.orc",
            "test_data/performance/k-means/k-means-scala-inner.orc",
            "test_data/performance/k-means/k-means.orc",
            //"test_data/performance/bigsort/bigsort.orc",
            //"test_data/performance/bigsort/bigsort-partially-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            //"test_data/performance/sssp/sssp-batched-partitioned.orc",
            "test_data/performance/canneal/canneal-naive.orc",
            "test_data/performance/canneal/canneal-partitioned.orc",
            //"test_data/performance/dedup/dedup-boundedchannel.orc",
            //"test_data/performance/dedup/dedup.orc",
            )
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(new File("OrcTests/" + fn), nCPUs, false, allowSpawnInlining, optLevel)
      }
      val scala = for {
        nCPUs <- nCPUsValues
        benchmark <- Seq(
            orc.test.item.scalabenchmarks.Mandelbrot,
            //orc.test.item.scalabenchmarks.NQueens,
            //orc.test.item.scalabenchmarks.ThreadRing,
            orc.test.item.scalabenchmarks.blackscholes.BlackScholesPar,
            orc.test.item.scalabenchmarks.kmeans.KMeansPar,
            orc.test.item.scalabenchmarks.kmeans.KMeansParManual,
            //orc.test.item.scalabenchmarks.BigSortPar,
            orc.test.item.scalabenchmarks.swaptions.SwaptionsParTrial, 
            orc.test.item.scalabenchmarks.swaptions.SwaptionsParSwaption,
            //orc.test.item.scalabenchmarks.sssp.SSSPBatchedPar, 
            orc.test.item.scalabenchmarks.canneal.Canneal, 
            //orc.test.item.scalabenchmarks.dedup.DedupNestedPar, 
            //orc.test.item.scalabenchmarks.dedup.DedupBoundedQueue, 
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




object PorcEInliningTCOExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 4
  override def hardTimeLimit: Double = 60 * 5.5
    
  case class MyPorcEExperimentalCondition(
      orcFile: File, 
      nCPUs: Int, 
      allowAllSpawnInlining: Boolean, 
      truffleASTInlining: Boolean, 
      universalTCO: Boolean, 
      optLevel: Int) 
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("allowAllSpawnInlining", "Allow All Spawn Inlining", "", ""),
      FactorDescription("universalTCO", "Use universal TCO", "", ""),
      FactorDescription("truffleASTInlining", "truffleASTInlining", "", ""),
      FactorDescription("optLevel", "Optimization Level", "", "-O level"),
    )
    
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> "false",
        "orc.porce.allowSpawnInlining" -> true,
        "orc.porce.allowAllSpawnInlining" -> allowAllSpawnInlining,
        "orc.numerics.preferLP" -> "true",
        "orc.porce.truffleASTInlining" -> truffleASTInlining,
        "orc.porce.universalTCO" -> universalTCO,
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", optLevel.toString)
    
    override def toJvmArgs = Seq("-XX:+UseG1GC", "-Xms64g", "-Xmx115g") ++ super.toJvmArgs
  }
  case class MyScalaExperimentalCondition(
      benchmarkClass: Class[_], 
      nCPUs: Int) 
      extends ArthursBenchmarkEnv.ScalaExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("benchmarkClass", "Benchmark Class", "", "The class run for this benchmark"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use")
    )
    
    override def toJvmArgs = Seq("-XX:+UseG1GC", "-Xms64g", "-Xmx115g") ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = (Seq(1, 4, 8, 16, 24)).reverse 
      val porce = for {
        optLevel <- Seq(3)
        fn <- Seq(
            //"test_data/performance/Mandelbrot.orc",
            //"test_data/performance/8-queens.orc",
            //"test_data/performance/threadring.orc",
            //"test_data/performance/threadring2.orc",
            //"test_data/performance/black-scholes/black-scholes-partitioned-seq.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-partitioned-seq.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-partially-seq.orc",
            //"test_data/performance/black-scholes/black-scholes.orc",
            //"test_data/performance/k-means/k-means-scala-inner.orc",
            //"test_data/performance/k-means/k-means.orc",
            //"test_data/performance/bigsort/bigsort.orc",
            //"test_data/performance/bigsort/bigsort-partially-seq.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-subroutines-seq.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            //"test_data/performance/sssp/sssp-batched-partitioned.orc",
            //"test_data/performance/canneal/canneal-naive.orc",
            //"test_data/performance/canneal/canneal-partitioned.orc",
            //"test_data/performance/dedup/dedup-boundedchannel.orc",
            //"test_data/performance/dedup/dedup.orc",
            )
        nCPUs <- nCPUsValues
        allowAllSpawnInlining <- Seq(true, false)
        truffleASTInlining <- Seq(true, false)
        universalTCO <- Seq(true, false)
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(new File("OrcTests/" + fn), nCPUs, allowAllSpawnInlining, truffleASTInlining, universalTCO, optLevel)
      }
      porce 
    }
    runExperiment(experimentalConditions)
  }
}



object PorcEDevelopmentImprovementExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 5
  override def hardTimeLimit: Double = 60 * 6.25
  
  trait Option extends Product with Serializable {
    def optopt(enabled: Boolean): Map[String, String] = Map()
    def sysprop(enabled: Boolean): Map[String, String] = Map()
  }
  
  case class BooleanOptOpt(name: String, enabledState: Boolean) extends Option {
    override def optopt(enabled: Boolean): Map[String, String] = {
      Map(name -> (if(enabled) enabledState else !enabledState).toString)
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
        BooleanSysProp("orc.porce.allowSpawnInlining", true),
        )

    // Only inlining fast tasks (based on runtime profiling):
    val InlineAverageTimeLimit = Seq(
        StringSysProp("orc.porce.inlineAverageTimeLimit", "10.0", "10000000.0"),
        )

    // Polymorphic inline caches for calls:
    val PolyInlineCaches = Seq(
      StringSysProp("orc.porce.cache.getFieldMaxCacheSize", "4", "0"),
      StringSysProp("orc.porce.cache.internalCallMaxCacheSize", "12", "0"),
      StringSysProp("orc.porce.cache.externalDirectCallMaxCacheSize", "8", "0"),
      StringSysProp("orc.porce.cache.externalCPSCallMaxCacheSize", "12", "0"),
      BooleanSysProp("orc.porce.optimizations.externalCPSDirectSpecialization", true),
      )

    // Specialize compiled code for runtime states such as futured already being resolved:
    val SpecializeOnRuntimeStates = Seq(
      BooleanSysProp("orc.porce.optimizations.inlineForceResolved", true),
      BooleanSysProp("orc.porce.optimizations.inlineForceHalted", true),
      BooleanSysProp("orc.porce.optimizations.specializeOnCounterStates", true),
      BooleanSysProp("orc.porce.optimizations.environmentCaching", true),
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
  
  val Steps = Seq(
      Orctimizer.PeepholeOptimizations,
      Orctimizer.Inlining,
      Porc.PeepholeOptimizations,
      Porc.Inlining,
      Orctimizer.ForceElimination,     // 5
      Orctimizer.FutureElimination,
      PorcE.OccationallySchedule,
      PorcE.AllowSpawnInlining,
      PorcE.InlineAverageTimeLimit,
      PorcE.PolyInlineCaches,          // 10
      PorcE.SpecializeOnRuntimeStates, // 11
      LimitedPrecision,      
      PorcE.OptimizedTCO,
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
    
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> "false",
        ) ++ 
        enabledOptions.flatMap(_.sysprop(true)) ++ 
        disabledOptions.flatMap(_.sysprop(false))
    
    def optoptString = {
      val opts = (enabledOptions.flatMap(_.optopt(true)) ++ disabledOptions.flatMap(_.optopt(false)))
      opts.map({ case (n, v) => s"$n=$v"}).mkString(",")
    }
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", "3", "--opt-opt", optoptString)
    
    override def toJvmArgs = Seq("-XX:+UseG1GC", "-Xms64g", "-Xmx115g") ++ super.toJvmArgs
  }
  
  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = Seq(24, 8)
      val porce = for {
        nCPUs <- nCPUsValues
        fn <- Seq(
            "test_data/performance/black-scholes/black-scholes.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute.orc",
            "test_data/performance/k-means/k-means.orc",
            "test_data/performance/k-means/k-means-scala-inner.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            "test_data/performance/dedup/dedup.orc",
            "test_data/performance/dedup/dedup-boundedchannel.orc",
            "test_data/performance/Mandelbrot.orc",
            )
        step <- 0 to Steps.size
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