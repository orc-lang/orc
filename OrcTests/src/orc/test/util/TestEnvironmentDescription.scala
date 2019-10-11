//
// TestEnvironmentDescription.scala -- Scala class TestEnvironmentDescription
// Project OrcTests
//
// Created by jthywiss on Sep 4, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.{ IOException, OutputStreamWriter }
import java.lang.management.ManagementFactory
import java.lang.reflect.Modifier
import java.time.{ Duration, Instant }
import java.util.jar.Manifest

import scala.collection.JavaConverters.{ enumerationAsScalaIteratorConverter, mapAsScalaMapConverter }

import orc.util.{ ExecutionLogOutputStream, ShutdownHook }

/** Captures a snapshot of the execution environment state at time of
  * construction.
  *
  * @author jthywiss
  */
class TestEnvironmentDescription() extends FieldsToMap {

  val jvmDescription = new JvmDescription().toMap
  val classPathDescription = new ClassPathDescription().toMap
  val scmStateDescription = new ScmStateDescription().toMap
  val orcDescription = new OrcDescription().toMap

  class JvmDescription() extends FieldsToMap {

    @transient
    private val runtimeMXBean = ManagementFactory.getRuntimeMXBean

    @transient
    private val operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean

    def maybeSunOsMXBean[T](osMXBean: java.lang.management.OperatingSystemMXBean, f: com.sun.management.OperatingSystemMXBean => T, default: T): T = {
      osMXBean match {
        case sunOsMXBean: com.sun.management.OperatingSystemMXBean => f(sunOsMXBean)
        case _ => default
      }
    }

    @transient
    private val threadMXBean = ManagementFactory.getThreadMXBean

    @transient
    private val memoryMXBean = ManagementFactory.getMemoryMXBean

    //  @transient
    //  private val memoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans
    //
    //  @transient
    //  private val memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans
    //
    //  @transient
    //  private val garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans

    @transient
    private val classLoadingMXBean = ManagementFactory.getClassLoadingMXBean

    @transient
    private val compilationMXBean = ManagementFactory.getCompilationMXBean

    val hostname = java.net.InetAddress.getLocalHost.getHostName

    val jvmId = runtimeMXBean.getName

    val jvmArgs = runtimeMXBean.getInputArguments

    val environmentVariables = System.getenv

    val javaSystemProperties = System.getProperties

    val jvmStartTime_ms = runtimeMXBean.getStartTime
    val jvmStartTime_ISO8601 = Instant.ofEpochMilli(jvmStartTime_ms).toString

    val jvmUpTime_ms = runtimeMXBean.getUptime
    val jvmUpTime_ISO8601 = Duration.ofMillis(jvmUpTime_ms).toString

    val logicalCpuCoreCount = Runtime.getRuntime.availableProcessors

    val systemRunnableQueueDepth = operatingSystemMXBean.getSystemLoadAverage

    val systemCpuProportionNonidle = maybeSunOsMXBean(operatingSystemMXBean, _.getSystemCpuLoad, -1.0D)
    val processCumulativeCpuTime_ns = maybeSunOsMXBean(operatingSystemMXBean, _.getProcessCpuTime, -1L)
    val processCpuProportionUsed = maybeSunOsMXBean(operatingSystemMXBean, _.getProcessCpuLoad, -1.0D)

    val threadCountCurrent = threadMXBean.getThreadCount
    val threadCountCurrentDaemon = threadMXBean.getDaemonThreadCount
    val threadCountPeak = threadMXBean.getPeakThreadCount
    val threadCountTotalStarted = threadMXBean.getTotalStartedThreadCount

    val committedVirtualMemorySize_B = maybeSunOsMXBean(operatingSystemMXBean, _.getCommittedVirtualMemorySize, -1L)
    val totalSwapSpaceSize_B = maybeSunOsMXBean(operatingSystemMXBean, _.getTotalSwapSpaceSize, -1L)
    val freeSwapSpaceSize_B = maybeSunOsMXBean(operatingSystemMXBean, _.getFreeSwapSpaceSize, -1L)
    val freePhysicalMemorySize_B = maybeSunOsMXBean(operatingSystemMXBean, _.getFreePhysicalMemorySize, -1L)
    val totalPhysicalMemorySize_B = maybeSunOsMXBean(operatingSystemMXBean, _.getTotalPhysicalMemorySize, -1L)

    val heapInitSize_B = memoryMXBean.getHeapMemoryUsage.getInit
    val heapUsedSize_B = memoryMXBean.getHeapMemoryUsage.getUsed
    val heapCommittedSize_B = memoryMXBean.getHeapMemoryUsage.getCommitted
    val heapMaxSize_B = memoryMXBean.getHeapMemoryUsage.getMax

    val nonheapInitSize_B = memoryMXBean.getNonHeapMemoryUsage.getInit
    val nonheapUsedSize_B = memoryMXBean.getNonHeapMemoryUsage.getUsed
    val nonheapCommittedSize_B = memoryMXBean.getNonHeapMemoryUsage.getCommitted
    val nonheapMaxSize_B = memoryMXBean.getNonHeapMemoryUsage.getMax

    val classCountTotalLoaded = classLoadingMXBean.getTotalLoadedClassCount
    val classCountCurrentLoaded = classLoadingMXBean.getLoadedClassCount
    val classCountUnloaded = classLoadingMXBean.getUnloadedClassCount

    val jitCompiler = compilationMXBean.getName
    val jitCumulativeCompilationTime_ms = compilationMXBean.getTotalCompilationTime
    val jitCumulativeCompilationTime_ISO8601 = Duration.ofMillis(jitCumulativeCompilationTime_ms).toString

    // Hotspot-specific:
    //    public native long getSafepointCount();
    //    public native long getTotalSafepointTime();
    //    public native long getSafepointSyncTime();
    //    public native long getTotalApplicationNonStoppedTime();
    //
    //    public native long getLoadedClassSize();
    //    public native long getUnloadedClassSize();
    //    public native long getClassLoadingTime();
    //    public native long getMethodDataSize();
    //    public native long getInitializedClassCount();
    //    public native long getClassInitializationTime();
    //    public native long getClassVerificationTime();

  }

  class ClassPathDescription() extends FieldsToMap {

    val manifests = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, String]]()

    for (manifestUrl <- Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF").asScala) {
      val is = manifestUrl.openStream()
      val manifest = new Manifest(is)
      is.close()
      manifests.put(manifestUrl.toExternalForm, manifest.getMainAttributes.asInstanceOf[java.util.Map[String, String]].asScala)
      /* Cast required because java.util.jar.Manifest.getMainAttributes's
       * declared type is Map[AnyRef,AnyRef] for compatibility reasons. */
    }

  }

  class ScmStateDescription() extends FieldsToMap {

    val gitDescribe = {
      try {
        OsCommand.runAndGetResult(Seq("git", "describe", "--dirty", "--tags", "--always")).stdout.stripLineEnd
      } catch {
        case _: IOException => null
      }
    }

    val gitLastCommit = {
      try {
        OsCommand.runAndGetResult(Seq("git", "log", "-1", "--format=%H")).stdout.stripLineEnd
      } catch {
        case _: IOException => null
      }
    }

    val gitStatus = {
      try {
        OsCommand.runAndGetResult(Seq("git", "status", "-vv")).stdout.stripLineEnd
      } catch {
        case _: IOException => null
      }
    }

  }

  class OrcDescription() extends FieldsToMap {
    val version = orc.Main.versionProperties
  }

}

object TestEnvironmentDescription {

  private var shutdownHookAdded = false

  def dumpAtShutdown(): Unit = synchronized {
    if (!shutdownHookAdded) {
      Runtime.getRuntime().addShutdownHook(TestEnvironmentDescriptionDumpThread)
      shutdownHookAdded = true
    }
  }

  def main(args: Array[String]): Unit = {
    JsonGenerator(System.out)(new TestEnvironmentDescription().toMap)
  }

  private object TestEnvironmentDescriptionDumpThread extends ShutdownHook("TestEnvironmentDescriptionDumpThread") {
    override def run = synchronized {
      val envDescOut = ExecutionLogOutputStream("envDescrip", "json", "Test environment description output file")
      if (envDescOut.isDefined) {
        val envDescWriter = new OutputStreamWriter(envDescOut.get, "UTF-8")
        JsonGenerator(envDescWriter)(new TestEnvironmentDescription().toMap)
        envDescWriter.close()
        envDescOut.get.close()
      }
    }
  }

}

protected trait FieldsToMap {

  def toMap: scala.collection.Map[String, Any] = mapOfAllFields(this)

  /** All fields as a Map */
  def mapOfAllFields(source: AnyRef): scala.collection.Map[String, Any] = {
    val allFields = scala.collection.mutable.Map[String, Any]()
    val fs = source.getClass.getDeclaredFields.filterNot({ f => (f.getModifiers & Modifier.STATIC) != 0 || (f.getModifiers & Modifier.TRANSIENT) != 0 || f.isSynthetic || f.getName.contains("$") })
    fs.foreach({ field =>
      allFields.put(field.getName, source.getClass.getMethod(field.getName).invoke(source))
    })
    allFields
  }

  /** All fields as a JSON string */
  override def toString: String = {
    val sb = new java.lang.StringBuilder(10000)
    JsonGenerator(sb)(toMap)
    sb.toString
  }

}
