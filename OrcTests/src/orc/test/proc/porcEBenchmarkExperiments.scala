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

object PorcEStrongScalingExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 9
  
  trait HasRunNumber {
    def run: Int
  }   
  
  val jvmOpts = Seq("-XX:+UseParallelGC", "-Xms8g", "-Xmx64g")
    
  case class MyPorcEExperimentalCondition(
      run: Int,
      orcFile: File, 
      nCPUs: Int) 
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition with HasRunNumber {
    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
    )
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> "true",
        "orc.numerics.preferLP" -> "true",
        //"orc.porce.universalTCO" -> "false",
        "graal.TruffleCompilationThreshold" -> 800,
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", "3")
    
    override def toJvmArgs = jvmOpts ++ super.toJvmArgs
  }
  case class MyScalaExperimentalCondition(
      run: Int,
      benchmarkClass: Class[_], 
      nCPUs: Int) 
      extends ArthursBenchmarkEnv.ScalaExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition with HasRunNumber {
    //override def nRuns = super.nRuns / 2
    
    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("benchmarkClass", "Benchmark Class", "", "The class run for this benchmark"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use")
    )
    
    override def toJvmArgs = jvmOpts ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = Set(1, 12, 24)
      val porce = for {
        run <- 0 until 2
        nCPUs <- nCPUsValues
        fn <- Seq(
            //"test_data/performance/Mandelbrot.orc",
            //"test_data/performance/8-queens.orc",
            //"test_data/performance/threadring.orc",
            //"test_data/performance/threadring2.orc",
            "test_data/performance/black-scholes/black-scholes-partitioned-seq.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-partially-seq.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-for-tree.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-partitioned-seq.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-partitioned-seq-optimized.orc",
            "test_data/performance/black-scholes/black-scholes-partially-seq.orc",
            //"test_data/performance/black-scholes/black-scholes.orc",
            "test_data/performance/k-means/k-means-scala-inner.orc",
            "test_data/performance/k-means/k-means.orc",
//            "test_data/performance/bigsort/bigsort.orc",
//            "test_data/performance/bigsort/bigsort-partially-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-swaption.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            "test_data/performance/sssp/sssp-batched-partitioned.orc",
            "test_data/performance/sssp/sssp-batched.orc",
            //"test_data/performance/canneal/canneal-naive.orc",
            //"test_data/performance/canneal/canneal-partitioned.orc",
            "test_data/performance/dedup/dedup-boundedchannel.orc",
            "test_data/performance/dedup/dedup.orc",
            )
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(run, new File("OrcTests/" + fn), nCPUs)
      }
      val scala = for {
        run <- 0 until 1
        nCPUs <- nCPUsValues + 1
        benchmark <- Seq(
            //orc.test.item.scalabenchmarks.Mandelbrot,
            //orc.test.item.scalabenchmarks.NQueens,
            //orc.test.item.scalabenchmarks.ThreadRing,
            orc.test.item.scalabenchmarks.blackscholes.BlackScholesPar,
            orc.test.item.scalabenchmarks.kmeans.KMeansPar,
            orc.test.item.scalabenchmarks.kmeans.KMeansParManual,
//            orc.test.item.scalabenchmarks.BigSortPar,
            orc.test.item.scalabenchmarks.swaptions.SwaptionsParTrial, 
            orc.test.item.scalabenchmarks.swaptions.SwaptionsParSwaption,
            orc.test.item.scalabenchmarks.sssp.SSSPBatchedPar, 
            //orc.test.item.scalabenchmarks.canneal.Canneal, 
            orc.test.item.scalabenchmarks.dedup.DedupNestedPar, 
            orc.test.item.scalabenchmarks.dedup.DedupBoundedQueue, 
            )
      } yield {
        val cls = Class.forName(benchmark.getClass.getCanonicalName.stripSuffix("$"))
        MyScalaExperimentalCondition(run, cls, nCPUs)
      }
      (porce /*++ scala*/).sortBy(o => (o.run, -o.nCPUs, o.toString))
    }
    runExperiment(experimentalConditions)
  }
}

object PorcEFutureForceOptimizationExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 9
  
  trait HasRunNumber {
    def run: Int
  }   
  
  val jvmOpts = Seq("-XX:+UseParallelGC", "-Xms8g", "-Xmx64g")
    
  case class MyPorcEExperimentalCondition(
      run: Int,
      orcFile: File, 
      useGraft: Boolean,
      sequentializeForce: Boolean
      ) 
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with HasRunNumber {
    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("useGraft", "Use Porc-level Graft", "", ""),
      FactorDescription("sequentializeForce", "Sequentialize All Forces", "", ""),
    )
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> "true",
        "orc.numerics.preferLP" -> "true",
        "graal.TruffleCompilationThreshold" -> 800,
        )
        
    override def toOrcArgs = {
      val optopt = Seq( 
        if (useGraft) Some("porc:usegraft") else None,
        if (sequentializeForce) Some("orct:sequentialize-force") else None
        ).flatten
      super.toOrcArgs ++ Seq("-O", "3", "--opt-opt", optopt.mkString(","))
    }
    
    override def toJvmArgs = jvmOpts ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val porce = for {
        run <- 0 until 1
        useGraft <- Seq(true, false)
        sequentializeForce <- Seq(true, false)
        fn <- Seq(
            "test_data/performance/black-scholes/black-scholes-scala-compute.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-for-tree.orc",
            "test_data/performance/black-scholes/black-scholes-scala-compute-for-tree-opt.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-swaption.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            )
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(run, new File("OrcTests/" + fn), useGraft, sequentializeForce)
      }
      porce.sortBy(o => (o.run, o.toString))
    }
    runExperiment(experimentalConditions)
  }
}

object PorcEOptimizationStatisticsExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 5
    
  case class MyPorcEExperimentalCondition(
      run: Int,
      orcFile: File) 
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name")
    )
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> "false",
        "orc.numerics.preferLP" -> "true",
        "orc.porce.universalTCO" -> "true",
        "graal.TruffleCompilationThreshold" -> 800,
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", "3")
    
    override def toJvmArgs = Seq("-XX:+UseParallelGC", "-Xms16g", "-Xmx100g") ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val porce = for {
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
            "test_data/performance/swaptions/swaptions-naive-scala-swaption.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            "test_data/performance/sssp/sssp-batched-partitioned.orc",
            "test_data/performance/sssp/sssp-batched.orc",
            //"test_data/performance/canneal/canneal-naive.orc",
            //"test_data/performance/canneal/canneal-partitioned.orc",
            "test_data/performance/dedup/dedup-boundedchannel.orc",
            "test_data/performance/dedup/dedup.orc",
            )
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(0, new File("OrcTests/" + fn))
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
  def softTimeLimit: Double = 60 * 15
  override def hardTimeLimit: Double = 60 * 18
  
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
  
  val LastUsedStep = Seq()
  
  val Steps = Seq(
      Orctimizer.PeepholeOptimizations ++ Porc.PeepholeOptimizations,
      Orctimizer.Inlining ++ Porc.Inlining,
      Orctimizer.ForceElimination,
      Orctimizer.FutureElimination,
      PorcE.PolyInlineCaches,
      PorcE.SpecializeOnRuntimeStates,
      PorcE.OccationallySchedule ++ PorcE.AllowSpawnInlining ++ PorcE.InlineAverageTimeLimit,
      //LimitedPrecision,
      LastUsedStep,
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
        "orc.numerics.preferLP" -> "true"
        ) ++ 
        enabledOptions.flatMap(_.sysprop(true)) ++ 
        disabledOptions.flatMap(_.sysprop(false))
    
    def optoptString = {
      val opts = (enabledOptions.flatMap(_.optopt(true)) ++ disabledOptions.flatMap(_.optopt(false)))
      opts.map({ case (n, v) => s"$n=$v"}).mkString(",")
    }
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", "3", "--opt-opt", optoptString)
    
    override def toJvmArgs = Seq("-XX:+UseG1GC", "-Xms64g", "-Xmx100g") ++ super.toJvmArgs
  }
  
  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = Seq(24)
      val porce = for {
        nCPUs <- nCPUsValues
        fn <- Seq(
            //"test_data/performance/bigsort/bigsort.orc",
            "test_data/performance/canneal/canneal-naive.orc",
            "test_data/performance/sssp/sssp-batched.orc",
            //"test_data/performance/dedup/dedup.orc",
            "test_data/performance/black-scholes/black-scholes.orc",
            "test_data/performance/k-means/k-means.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            //"test_data/performance/Mandelbrot.orc",
            )
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


object PorcEInlineSpawnTimeExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 8
  override def hardTimeLimit: Double = 60 * 12
  
  case class MyPorcEExperimentalCondition(
      orcFile: File, 
      nCPUs: Int, 
      timeLimit: Double,
      allowSpawnInlining: Boolean,
      allowAllSpawnInlining: Boolean) 
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("timeLimit", "Inline Spawn Time Limit", "ms", ""),
      FactorDescription("allowSpawnInlining", "Allow Spawn Inlining", "", ""),
      FactorDescription("allowAllSpawnInlining", "Allow ALL Spawn Inlining", "", "This includes spawns which are marked as required."),
    )
    
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> "false",
        "orc.numerics.preferLP" -> "true",
        "orc.porce.inlineAverageTimeLimit" -> timeLimit,
        "orc.porce.allowSpawnInlining" -> allowSpawnInlining,
        "orc.porce.allowAllSpawnInlining" -> allowAllSpawnInlining
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", "3")
    
    override def toJvmArgs = Seq("-XX:+UseG1GC", "-Xms64g", "-Xmx100g") ++ super.toJvmArgs
  }
  
  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = Seq(24)
      val porce = for {
        nCPUs <- nCPUsValues
        fn <- Seq(
            //"test_data/performance/bigsort/bigsort.orc",
            //"test_data/performance/canneal/canneal-naive.orc",
            "test_data/performance/sssp/sssp-batched.orc",
            "test_data/performance/sssp/sssp-batched-partitioned.orc",
            //"test_data/performance/dedup/dedup.orc",
            "test_data/performance/black-scholes/black-scholes.orc",
            "test_data/performance/black-scholes/black-scholes-partitioned-seq.orc",
            "test_data/performance/k-means/k-means.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            //"test_data/performance/Mandelbrot.orc",
            )
        (timeLimit, allowSpawnInlining, allowAllSpawnInlining) <- 
          Seq((0.0, false, false)) ++ Seq(1000, 100, 10, 1, 0.1, 0.001, 0.000001, 0.00000001).map((_, true, true)) ++ Seq(100, 1, 0.000001).map((_, true, false)) 
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(new File("OrcTests/" + fn), nCPUs, timeLimit, allowSpawnInlining, allowAllSpawnInlining)
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


object PorcEInvokationOverheadsExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 10
    
  case class MyPorcEExperimentalCondition(
      orcFile: File,
      problemSize: Int)
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition {
    override def factorDescriptions = Seq(
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
    )
    
    override def systemProperties = super.systemProperties ++ Map(
        "orc.numerics.preferLP" -> "true",
        "orc.SimpleWorkStealingScheduler.overrideWorkers" -> 24,
        "orc.test.benchmark.problemSize" -> problemSize,
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", "3")
    
    override def toJvmArgs = Seq("-XX:+UseG1GC", "-Xms64g", "-Xmx115g") ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = (Seq(24)).reverse 
      val porce = for {
        (fn, size) <- Seq(
            ("test_data/performance/black-scholes/black-scholes-scala-compute.orc", 3),
            ("test_data/performance/black-scholes/black-scholes.orc", 3),
            ("test_data/performance/k-means/k-means-scala-inner.orc", 5),
            ("test_data/performance/k-means/k-means.orc", 1),
            //("test_data/performance/bigsort/bigsort.orc", 1),
            //("test_data/performance/bigsort/bigsort-partially-seq.orc", 1),
            ("test_data/performance/swaptions/swaptions-naive-scala-sim.orc", 5),
            ("test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc", 1),
            ("test_data/performance/sssp/sssp-batched-partitioned.orc", 3),
            ("test_data/performance/canneal/canneal-naive.orc", 1),
            ("test_data/performance/canneal/canneal-partitioned.orc", 1),
            //("test_data/performance/dedup/dedup-boundedchannel.orc", 1),
            //("test_data/performance/dedup/dedup.orc", 1),
            )
        nCPUs <- nCPUsValues
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(new File("OrcTests/" + fn), size)
      }
      porce 
    }
    runExperiment(experimentalConditions)
  }
}

object PorcEMemoryExperiment extends PorcEBenchmark {
  def softTimeLimit: Double = 60 * 1
  override def hardTimeLimit: Double = 60 * 3.5 
  
  trait HasRunNumber {
    def run: Int
  }
    
  case class MyPorcEExperimentalCondition(
      run: Int,
      orcFile: File, 
      nCPUs: Int, 
      gc: String,
      minMemory: Int,
      maxMemory: Int) 
      extends ArthursBenchmarkEnv.PorcEExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition with HasRunNumber {
    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("gc", "Which GC?", "", ""),
      FactorDescription("minMemory", "Minimum Java Heap Size", "gb", ""),
      FactorDescription("maxMemory", "Maximum Java Heap Size", "gb", "")
    )
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> false,
        "orc.porce.allowSpawnInlining" -> true,
        "orc.numerics.preferLP" -> "true"
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", "3")
    
    override def toJvmArgs = Seq(s"-XX:+Use$gc", s"-Xms${minMemory}g", s"-Xmx${maxMemory}g") ++ super.toJvmArgs
  }
  case class MyScalaExperimentalCondition(
      run: Int,
      benchmarkClass: Class[_], 
      nCPUs: Int,
      gc: String,
      minMemory: Int,
      maxMemory: Int) 
      extends ArthursBenchmarkEnv.ScalaExperimentalCondition with ArthursBenchmarkEnv.CPUControlExperimentalCondition with HasRunNumber {
    override def factorDescriptions = Seq(
      FactorDescription("run", "Run Number", "", ""),
      FactorDescription("benchmarkClass", "Benchmark Class", "", "The class run for this benchmark"),
      FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
      FactorDescription("gc", "Which GC?", "", ""),
      FactorDescription("minMemory", "Minimum Java Heap Size", "gb", ""),
      FactorDescription("maxMemory", "Maximum Java Heap Size", "gb", "")
    )
    
    override def toJvmArgs = Seq(s"-XX:+Use$gc", s"-Xms${minMemory}g", s"-Xmx${maxMemory}g") ++ super.toJvmArgs
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      val nCPUsValues = (Seq(8)) //, 24)).reverse 
      val gcValues = Seq("ParallelGC"/*, "G1GC"*/)
      val minValues = Seq(1) //, 4, 64)
      val maxValues = Seq(1) //, 4, 64)
      val runNum = 0 until 3
      val porce = for {
        run <- runNum 
        nCPUs <- nCPUsValues
        fn <- Seq(
            //"test_data/performance/Mandelbrot.orc",
            //"test_data/performance/8-queens.orc",
            //"test_data/performance/threadring.orc",
            //"test_data/performance/threadring2.orc",
            //"test_data/performance/black-scholes/black-scholes-partitioned-seq.orc",
            //"test_data/performance/black-scholes/black-scholes-scala-compute.orc",
            //"test_data/performance/black-scholes/black-scholes-scala-compute-partitioned-seq.orc",
            //"test_data/performance/black-scholes/black-scholes.orc",
            //"test_data/performance/k-means/k-means-scala-inner.orc",
            //"test_data/performance/k-means/k-means.orc",
            //"test_data/performance/bigsort/bigsort.orc",
            //"test_data/performance/bigsort/bigsort-partially-seq.orc",
            "test_data/performance/swaptions/swaptions-naive-scala-swaption.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-sim.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-subroutines-seq.orc",
            //"test_data/performance/swaptions/swaptions-naive-scala-subroutines.orc",
            //"test_data/performance/sssp/sssp-batched-partitioned.orc",
            //"test_data/performance/canneal/canneal-naive.orc",
            //"test_data/performance/canneal/canneal-partitioned.orc",
            //"test_data/performance/dedup/dedup-boundedchannel.orc",
            //"test_data/performance/dedup/dedup.orc",
            )
          gc <- gcValues
          minM <- minValues
          maxM <- maxValues
          if minM <= maxM
      } yield {
        assert(new File(fn).isFile(), fn)
        MyPorcEExperimentalCondition(run, new File("OrcTests/" + fn), nCPUs, gc, minM, maxM)
      }
      val scala = for {
        run <- runNum 
        nCPUs <- nCPUsValues
        benchmark <- Seq(
            //orc.test.item.scalabenchmarks.Mandelbrot,
            //orc.test.item.scalabenchmarks.NQueens,
            //orc.test.item.scalabenchmarks.ThreadRing,
            //orc.test.item.scalabenchmarks.blackscholes.BlackScholesPar,
            //orc.test.item.scalabenchmarks.kmeans.KMeansPar,
            //orc.test.item.scalabenchmarks.kmeans.KMeansParManual,
            //orc.test.item.scalabenchmarks.BigSortPar,
            orc.test.item.scalabenchmarks.swaptions.SwaptionsParSwaption,
            //orc.test.item.scalabenchmarks.swaptions.SwaptionsParTrial, 
            //orc.test.item.scalabenchmarks.sssp.SSSPBatchedPar, 
            //orc.test.item.scalabenchmarks.canneal.Canneal, 
            //orc.test.item.scalabenchmarks.dedup.DedupNestedPar, 
            //orc.test.item.scalabenchmarks.dedup.DedupBoundedQueue, 
            )
          gc <- gcValues
          minM <- minValues
          maxM <- maxValues
          if minM <= maxM
      } yield {
        val cls = Class.forName(benchmark.getClass.getCanonicalName.stripSuffix("$"))
        MyScalaExperimentalCondition(run, cls, nCPUs, gc, minM, maxM)
      }
      (porce ++ scala).sortBy(_.run)
    }
    runExperiment(experimentalConditions)
  }
}

