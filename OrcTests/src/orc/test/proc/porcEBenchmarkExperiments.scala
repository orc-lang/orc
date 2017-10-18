package orc.test.proc

import orc.test.util.FactorDescription
import java.io.File

object PorcEStrongScalingExperiment extends PorcEBenchmark {
    
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
      val porce = for {
        optLevel <- Seq(3)
        nCPUs <- (Seq(1) ++ (4 to 24 by 4) ++ Seq(32, 48)).reverse 
        allowSpawnInlining <- Seq(true)
        fn <- args.toSeq
      } yield {
        assert(new File(fn).isFile())
        MyPorcEExperimentalCondition(new File(fn), nCPUs, false, allowSpawnInlining, optLevel)
      }
      val scala = for {
        nCPUs <- (Seq(1) ++ (4 to 24 by 4) ++ Seq(32, 48)).reverse 
        clsName <- Seq(
            "orc.test.item.scalabenchmarks.Mandelbrot",
            "orc.test.item.scalabenchmarks.swaptions.SwaptionsParTrial", 
            "orc.test.item.scalabenchmarks.swaptions.SwaptionsParSwaption"
            )
      } yield {
        val cls = Class.forName(clsName)
        assert(cls != null)
        MyScalaExperimentalCondition(cls, nCPUs)
      }
      scala ++ porce
    }
    runExperiment(experimentalConditions)
  }
}