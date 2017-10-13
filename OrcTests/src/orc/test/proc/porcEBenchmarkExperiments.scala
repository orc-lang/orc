package orc.test.proc

import orc.test.util.FactorDescription
import java.io.File

object PorcETestExperiment extends PorcEBenchmark {
    
  val factors = Seq(
    FactorDescription("orcFile", "Orc File", "", "The Orc program file name"),
    FactorDescription("nCPUs", "Number of CPUs", "", "The number of CPUs to use"),
    FactorDescription("TruffleBackgroundCompilation", "Truffle Background Compilation", "", "Should truffle compile in the background"),
    FactorDescription("allowSpawnInlining", "Allow Spawn Inlining", "", ""),
    FactorDescription("optLevel", "Optimization Level", "", "-O level")
  )
  
  /** This is a list of the all the CPU ids in the order they should be assigned.
    * 
    */
  val cpuIDList = Seq(0,4,2,6,1,5,3,7)
  // TODO: This needs to be configurable.

  case class MyExperimentalCondition(orcFile: File, nCPUs: Int, truffleBackgroundCompilation: Boolean, allowSpawnInlining: Boolean, optLevel: Int) extends PorcEExperimentalCondition {
    override def factorDescriptions = factors
    override def systemProperties = super.systemProperties ++ Map(
        "graal.TruffleBackgroundCompilation" -> truffleBackgroundCompilation,
        "orc.porce.allowSpawnInlining" -> allowSpawnInlining
        )
        
    override def toOrcArgs = super.toOrcArgs ++ Seq("-O", optLevel.toString)
    
    override def wrapperCmd = Seq("taskset", "--cpu-list", cpuIDList.take(nCPUs).mkString(","))
  }

  def main(args: Array[String]): Unit = {
    val experimentalConditions = {
      for {
        allowSpawnInlining <- Seq(true)
        nCPUs <- Seq(1, 2, 3, 4, 8)
        optLevel <- Seq(2)
        fn <- args.toSeq
      } yield {
        assert(new File(fn).isFile())
        MyExperimentalCondition(new File(fn), nCPUs, false, allowSpawnInlining, optLevel)
      }
    }
    runExperiment(experimentalConditions)
  }
}