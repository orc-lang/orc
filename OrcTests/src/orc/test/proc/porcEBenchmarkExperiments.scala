package orc.test.proc

import orc.test.util.FactorDescription
import java.io.File

object PorcEStrongScalingExperiment extends PorcEBenchmark {
    
  val factors = Seq(
    FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
    FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
    FactorDescription("TruffleBackgroundCompilation", "Truffle Background Compilation", "", "Should truffle compile in the background"),
    FactorDescription("allowSpawnInlining", "Allow Spawn Inlining", "", ""),
    FactorDescription("optLevel", "Optimization Level", "", "-O level")
  )

  case class MyExperimentalCondition(orcFile: File, nCPUs: Int, truffleBackgroundCompilation: Boolean, allowSpawnInlining: Boolean, optLevel: Int) extends PorcEExperimentalCondition {
    override def factorDescriptions = factors
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> truffleBackgroundCompilation,
        "orc.porce.allowSpawnInlining" -> allowSpawnInlining
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", optLevel.toString)
    
    override def wrapperCmd = tasksetCommandPrefix(nCPUs)
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      for {
        optLevel <- Seq(3)
        nCPUs <- (Seq(1) ++ (4 to 24 by 4) ++ Seq(32, 48)).reverse 
        allowSpawnInlining <- Seq(true)
        fn <- args.toSeq
      } yield {
        assert(new File(fn).isFile())
        MyExperimentalCondition(new File(fn), nCPUs, false, allowSpawnInlining, optLevel)
      }
    }
    runExperiment(experimentalConditions)
  }
}