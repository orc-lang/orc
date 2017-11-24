//
// DistribScaleTestHtCondor.scala -- Scala object DistribScaleTestHtCondor
// Project OrcTests
//
// Created by jthywiss on Nov 2, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.io.{ File, FileWriter }
import java.nio.file.Paths

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable.ArrayBuffer

import orc.test.proc.DistribScaleTestCase.DistribScaleExperimentalCondition
import orc.test.util.{ ExpectedOutput, ExperimentalCondition, OsCommand, RemoteCommand, TestEnvironmentDescription, TestRunNumber, TestUtils }

/** Submits a run of distributed-Orc scaling tests to HTCondor.
  *
  * @author jthywiss
  */
object DistribScaleTestHtCondor {

  def buildCondorSubmissions(testRunNumber: String, experimentalConditions: Traversable[DistribScaleExperimentalCondition], testPrograms: Traversable[File]): Traversable[CondorJobCluster] = {
    val jobClusters = ArrayBuffer[CondorJobCluster]()
    for (experimentalCondition <- experimentalConditions) {
      configForExperimentalCondition(experimentalCondition)
      for (testProgram <- testPrograms) {
        jobClusters.append(new OrcCondorJobCluster(testRunNumber, testProgram, experimentalCondition))
      }
      if (experimentalCondition.dOrcNumRuntimes == 1) {
        /* Special case: Measure the Java WordCount in the 1 runtime (non-distributed) experimental condition */
        jobClusters.append(new JavaCondorJobCluster(testRunNumber, classOf[orc.test.item.distrib.WordCount], Seq.empty, experimentalCondition))
      }
    }
    jobClusters
  }

  protected def readTestConfig(testRunNumber: String) = {
    assert(DistribTestConfig.size > 0)

    /* Add extra variables to DistribTestConfig */
    DistribTestConfig.expanded.addVariable("currentJavaHome", System.getProperty("java.home"))
    //val currentJvmOpts = java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala
    //DistribTestConfig.expanded.addVariable("currentJvmOpts", currentJvmOpts)
    DistribTestConfig.expanded.addVariable("currentWorkingDir", System.getProperty("user.dir"))
    DistribTestConfig.expanded.addVariable("leaderHomeDir", OsCommand.getResultFrom(Seq("ssh", submitHostname, "pwd")).stdout.stripLineEnd)
    DistribTestConfig.expanded.addVariable("orcVersion", orc.Main.versionProperties.getProperty("orc.version"))
    DistribTestConfig.expanded.addVariable("testRunNumber", testRunNumber)

    /* Override testRootDir */
    val condorTestRootDir = HtCondorConfig("condorTestRootDir")
    DistribTestConfig.expanded.addVariable("testRootDir", condorTestRootDir)
  }

  protected def readExperimentalConditions() = DistribScaleTestCase.readExperimentalConditions()

  protected def getTestPrograms(): Traversable[File] = {
    val testProgramRoots = Seq(new File("test_data/performance/distrib"))
    val foundFiles = new java.util.LinkedList[File]()
    for (testProgramRoot <- testProgramRoots) {
      TestUtils.findOrcFiles(testProgramRoot, foundFiles)
    }
    foundFiles.asScala.filterNot(new ExpectedOutput(_).isEmpty)
  }

  protected def configForExperimentalCondition(experimentalCondition: DistribScaleTestCase.DistribScaleExperimentalCondition) = {
    val testContext = experimentalCondition.toMap
    if (testContext != null) {
      /* XXX */
      for ((key, value) <- testContext) DistribTestConfig.expanded.addVariable(key, value.toString())
    }

  }

  protected trait CondorJobCluster {
    val outFilenamePrefix: String
    def submitDesciptionString: String
  }

  protected class OrcCondorJobCluster(testRunNumber: String, testProgram: File, experimentalCondition: DistribScaleTestCase.DistribScaleExperimentalCondition) extends CondorJobCluster {

    val outFilenamePrefix: String = testProgram.getName.stripSuffix(".orc") + "_" + experimentalCondition.productIterator.mkString("_")

    val submitDescription = {
      val testContext = experimentalCondition.toMap

      val javaCmd = DistribTestConfig.expanded("javaCmd")
      val dOrcClassPath = DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get.mkString(File.pathSeparator)
      val jvmOpts = DistribTestConfig.expanded.getIterableFor("jvmOpts").get.toSeq ++ (if (testContext != null) for ((k, v) <- testContext) yield s"-Dorc.test.$k=$v" else Seq.empty)
      val leaderWorkingDir = DistribTestConfig.expanded("leaderWorkingDir")
      val leaderClass = DistribTestConfig.expanded("leaderClass")
      val leaderOpts = DistribTestConfig.expanded.getIterableFor("leaderOpts").getOrElse(Seq())
      val followerClass = DistribTestConfig.expanded("followerClass")
      val followerOpts = DistribTestConfig.expanded.getIterableFor("followerOpts").getOrElse(Seq())

      val dOrcPortBase = DistribTestConfig.expanded.get("dOrcPortBase").get.toInt
      val dOrcPortMax = DistribTestConfig.expanded.get("dOrcPortMax").get.toInt

      val remoteRunOutputDir = DistribTestConfig.expanded("runOutputDir").stripSuffix("/")

      val jobEventNotification = HtCondorConfig("jobEventNotification")

      ("""# HTCondor submit description file for Orc run """ + testRunNumber + ", program " + testProgram.getName + ", factors " + experimentalCondition.toString + """
        |#
        |# DO NOT EDIT -- automatically generated by DistribScaleTestHtCondor.scala
        |#
        |
        |##
        |# HTCondor universe (execution environment type)
        |##
        |
        |universe = Parallel
        |
        |
        |##
        |# Job cluster variables
        |##
        |
        |executable = build/orc/test/item/orc-run-parallel.sh
        |
        |log = """ + remoteRunOutputDir + "/" + outFilenamePrefix + """_condor.log
        |
        |# For condor_q batch option; all jobs with the same batch name are summarized on one status line:
        |JobBatchName = Orc run """ + testRunNumber + """
        |
        |submit_event_notes = Orc run """ + testRunNumber + ", program " + testProgram.getName + ", factors " + experimentalCondition.toString + """
        |
        |
        |##
        |# Basic job description
        |##
        |
        |initialdir = """ + leaderWorkingDir + """
        |
        |orc_program = """ + testProgram.toString + """
        |
        |+JavaCmd = """" + javaCmd + """"
        |+JavaClassPath = """" + dOrcClassPath + """"
        |+JavaVMArguments = """" + jvmOpts.mkString(" ") + " -Dorc.executionlog.fileprefix=" + outFilenamePrefix + """_ -Dorc.executionlog.filesuffix=_$(Node)"
        |
        |+JavaMainClassNode0 = """" + leaderClass + """"
        |+JavaMainClassOtherNodes = """" + followerClass + """"
        |
        |+JavaMainArgumentsNode0 = """" + leaderOpts.mkString(" ") + """ --follower-sockets=#Orc:followerSockets# $(orc_program)"
        |+JavaMainArgumentsOtherNodes = """" + followerOpts.mkString(" ") + """ $(Node) $$(Machine):#Orc:port#"
        |
        |# arguments are ignored by orc-run-parallel.sh, but displayed in some places, so to make those more usable, we supply informational arguments here:
        |arguments = """ + testProgram.getName + " " + experimentalCondition.toString + """
        |
        |# For condor_q nobatch option; first 17 characters are displayed as "CMD":
        |description = Orc """ + testRunNumber + " " + outFilenamePrefix + """
        |
        |machine_count = """ + experimentalCondition.dOrcNumRuntimes + """
        |
        |
        |##
        |# Filename parts (for use in other definitions)
        |##
        |
        |job_filename_pre = """ + remoteRunOutputDir + "/" + outFilenamePrefix + """
        |node_filename_pre = $(job_filename_pre)_$(Node)
        |
        |
        |###
        |# Inter-node communication for Orc
        |###
        |
        |# Location of temporary file on submit node where listen socket addresses are exchanged:
        |+ListenerSockAddrFile = "$(job_filename_pre).listen-sockaddr"
        |
        |# Duration for leader to wait for all followers, in seconds:
        |+ListenerWaitTimeout = 1200
        |
        |# TCP listen port numbers for dOrc are in $(OrcPortBase) to $(OrcPortMax) range:
        |+OrcPortBase = """ + dOrcPortBase + """
        |+OrcPortMax = """ + dOrcPortMax + """
        |
        |
        |##
        |# Job event notification
        |##
        |
        |notification = """ + jobEventNotification + """
        |email_attributes = JobDescription,Iwd,JavaCmd,JavaClassPath,JavaVMArguments,JavaMainClassNode0,JavaMainClassOtherNodes,JavaMainArgumentsNode0,JavaMainArgumentsOtherNodes
        |
        |
        |##
        |# Termination
        |##
        |
        |want_graceful_removal = True
        |+ParallelShutdownPolicy = "WAIT_FOR_ALL"
        |
        |
        |##
        |# File I/O
        |##
        |
        |output = $(node_filename_pre).out
        |error = $(node_filename_pre).err
        |
        |want_remote_io = False
        |
        |# Use Condor Chirp to access files on the submit machine
        |+WantIOProxy = true
        |
        |
        |##
        |# Placement policy
        |##
        |
        |requirements = TARGET.HasJava
        |
        |
        |##
        |# Accounting
        |##
        |
        |# UTCS-specific:
        |+Group = "GRAD"
        |+Project = "PROGRAMMING_LANGUAGES"
        |+ProjectDescription = "Orc programming language project"
        |
        |
        |##
        |# Submit job
        |##
        |
        |queue
        |""").stripMargin
    }

    def submitDesciptionString = submitDescription.toString

  }

  /** Run a Java class's main method with Orc environment in HTCondor parallel universe, but with one machine */
  protected class JavaCondorJobCluster(testRunNumber: String, testItem: Class[_], mainArgs: Seq[String], experimentalCondition: DistribScaleTestCase.DistribScaleExperimentalCondition) extends CondorJobCluster {
    def getClassNameWithoutPackage(c: Class[_]) = c.getName.substring(c.getName.lastIndexOf(".") + 1)

    val outFilenamePrefix: String = getClassNameWithoutPackage(testItem) + "_" + experimentalCondition.productIterator.mkString("_")

    val submitDescription = {
      def q(s: String) = OsCommand.quoteShellLiterally(s)

      val testContext = experimentalCondition.toMap

      val javaCmd = DistribTestConfig.expanded("javaCmd")
      val dOrcClassPath = DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get.mkString(File.pathSeparator)
      val jvmOpts = DistribTestConfig.expanded.getIterableFor("jvmOpts").get.toSeq ++ (if (testContext != null) for ((k, v) <- testContext) yield s"-Dorc.test.$k=$v" else Seq.empty)
      val leaderWorkingDir = DistribTestConfig.expanded("leaderWorkingDir")

      val remoteRunOutputDir = DistribTestConfig.expanded("runOutputDir").stripSuffix("/")

      val jobEventNotification = HtCondorConfig("jobEventNotification")

      ("""# HTCondor submit description file for Orc run """ + testRunNumber + ", class " + testItem.getName + ", factors " + experimentalCondition.toString + """
        |#
        |# DO NOT EDIT -- automatically generated by DistribScaleTestHtCondor.scala
        |#
        |
        |##
        |# HTCondor universe (execution environment type)
        |##
        |
        |universe = Parallel
        |
        |
        |##
        |# Job cluster variables
        |##
        |
        |executable = """ + javaCmd + """
        |
        |log = """ + remoteRunOutputDir + "/" + outFilenamePrefix + """_condor.log
        |
        |# For condor_q batch option; all jobs with the same batch name are summarized on one status line:
        |JobBatchName = Orc run """ + testRunNumber + """
        |
        |submit_event_notes = Orc run """ + testRunNumber + ", class " + testItem.getName + ", factors " + experimentalCondition.toString + """
        |
        |
        |##
        |# Basic job description
        |##
        |
        |initialdir = """ + leaderWorkingDir + """
        |
        |arguments = "-cp """ + q(dOrcClassPath) + " " + jvmOpts.map(q(_)).mkString(" ") + " -Dorc.executionlog.fileprefix=" + outFilenamePrefix + "_ " + testItem.getName + " " + mainArgs.map(q(_)).mkString(" ") + """"
        |
        |# For condor_q nobatch option; first 17 characters are displayed as "CMD":
        |description = Orc """ + testRunNumber + " " + outFilenamePrefix + """
        |
        |machine_count = 1
        |
        |
        |##
        |# Filename parts (for use in other definitions)
        |##
        |
        |job_filename_pre = """ + remoteRunOutputDir + "/" + outFilenamePrefix + """
        |
        |
        |##
        |# Job event notification
        |##
        |
        |notification = """ + jobEventNotification + """
        |email_attributes = JobDescription,Iwd,Cmd,Args
        |
        |
        |##
        |# Termination
        |##
        |
        |want_graceful_removal = True
        |+ParallelShutdownPolicy = "WAIT_FOR_ALL"
        |
        |
        |##
        |# File I/O
        |##
        |
        |output = $(job_filename_pre).out
        |error = $(job_filename_pre).err
        |
        |want_remote_io = False
        |
        |# Use Condor Chirp to access files on the submit machine
        |+WantIOProxy = true
        |
        |
        |##
        |# Placement policy
        |##
        |
        |requirements = TARGET.HasJava
        |
        |
        |##
        |# Accounting
        |##
        |
        |# UTCS-specific:
        |+Group = "GRAD"
        |+Project = "PROGRAMMING_LANGUAGES"
        |+ProjectDescription = "Orc programming language project"
        |
        |
        |##
        |# Submit job
        |##
        |
        |queue
        |""").stripMargin
    }

    def submitDesciptionString = submitDescription.toString

  }

  protected def setUpTest(experimentalConditions: Traversable[DistribScaleExperimentalCondition]) = {
    val remoteRunOutputDir = DistribTestConfig.expanded("runOutputDir").stripSuffix("/")

    /* Copy config dir to runOutputDir/../config */
    val localRunOutputDir = "../" + pathRelativeToTestRoot(remoteRunOutputDir)
    val orcConfigDir = "../" + pathRelativeToTestRoot(DistribTestConfig.expanded("orcConfigDir")).stripSuffix("/")
    new File(localRunOutputDir).mkdirs()
    OsCommand.checkExitValue(s"rsync of $orcConfigDir to $localRunOutputDir/../", OsCommand.getResultFrom(Seq("rsync", "-rlpt", orcConfigDir, localRunOutputDir + "/../")))

    copyFiles()

    RemoteCommand.mkdir(submitHostname, remoteRunOutputDir)

    ExperimentalCondition.writeExperimentalConditionsTable(experimentalConditions)

    TestEnvironmentDescription.dumpAtShutdown()
  }

  protected def pathRelativeToTestRoot(path: String): String = {
    Paths.get(DistribTestConfig.expanded("testRootDir")).normalize.relativize(Paths.get(path).normalize).toString
  }

  protected def copyFiles(): Unit = {
    print(s"Copying Orc test files to submit host $submitHostname...")
    for (cpEntry <- DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get) {
      print(".")
      val localFilename = ".." + cpEntry.stripPrefix(DistribTestConfig.expanded("testRootDir")).stripSuffix("/*")
      val remoteFilename = cpEntry.stripSuffix("/*")
      RemoteCommand.mkdirAndRsync(localFilename, submitHostname, remoteFilename)
    }
    print(".")
    RemoteCommand.mkdirAndRsync("config/logging.properties", submitHostname, DistribTestConfig.expanded("loggingConfigFile"))
    print(".")
    val testDataDir = DistribTestConfig.expanded("testDataDir")
    RemoteCommand.mkdirAndRsync("../" + pathRelativeToTestRoot(testDataDir), submitHostname, testDataDir)
    println("done")
  }

  protected def runCondorSubmissions(testRunNumber: String, condorSubmissions: Traversable[CondorJobCluster]) = {
    println(s"Submitting Orc run $testRunNumber to HTCondor on $submitHostname as ${condorSubmissions.size} job clusters...")
    for (condorSubmission <- condorSubmissions) {
      val condorSubmissionName = condorSubmission.outFilenamePrefix + "_condor.sub"
      val subFile = new File(System.getProperty("orc.executionlog.dir"), condorSubmissionName)
      val subFileWriter = new FileWriter(subFile)
      try {
        subFileWriter.append(condorSubmission.submitDesciptionString)
      } finally {
        subFileWriter.close()
      }
      val remoteRunOutputDir = DistribTestConfig.expanded("runOutputDir").stripSuffix("/")
      RemoteCommand.rsyncToRemote(subFile.toString, submitHostname, remoteRunOutputDir + "/" + condorSubmissionName)

      val leaderWorkingDir = DistribTestConfig.expanded("leaderWorkingDir")
      OsCommand.run(Seq("ssh", submitHostname, s"cd '${leaderWorkingDir}'; condor_submit '${remoteRunOutputDir}/${condorSubmissionName}'"))
    }
    println(s"Orc run $testRunNumber HTCondor submission complete.  ${currentDateTimeString()}")
  }

  def currentDateTimeString(): String = java.text.DateFormat.getDateTimeInstance.format(System.currentTimeMillis())

  val submitHostname = HtCondorConfig("submitHostname")

  def main(args: Array[String]): Unit = {
    val testRunNumber = TestRunNumber.singletonNumber
    readTestConfig(testRunNumber)
    val experimentalConditions = readExperimentalConditions()
    val testPrograms = getTestPrograms()
    val condorSubmissions = buildCondorSubmissions(testRunNumber, experimentalConditions, testPrograms)
    setUpTest(experimentalConditions)
    runCondorSubmissions(testRunNumber, condorSubmissions)
  }
}
