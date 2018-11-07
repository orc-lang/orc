//
// WordCount.scala -- Scala benchmark WordCount
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import java.io.{ BufferedReader, File, FileNotFoundException, FileReader }

object WordCount extends BenchmarkApplication[List[String], Long] with ExpectedBenchmarkResult[Long] {
  val numInputFiles = 24 // problemSizeScaledInt(1.2) -- Read(JavaSys.getProperty("orc.test.numInputFiles", "12"))
  val repeatRead = 1 // Read(JavaSys.getProperty("orc.test.repeatRead", "1"))
  val orcTestsPath = System.getProperty("orc.test.benchmark.datadir", "../OrcTests/")
  val holmesDataDirPath = orcTestsPath + "test_data/functional_valid/distrib/holmes_test_data"
  val inputDataDirPath = orcTestsPath + "test_data/performance/distrib/wordcount/wordcount-input-data/"
  val targetFileSize = 17895697 // {- bytes -} = 2 GiB / 120
  val numCopiesInputFiles = 120 // can be numInputFiles, if we delete and re-gen input files for every condition

  def checkReadableFile(file: File): Unit = {
    if (!file.canRead())
      throw new FileNotFoundException("Cannot read file: "+file+" in dir "+System.getProperty("user.dir"))
  }

  def listFileNamesRecursively(dirPathName: String): List[String] = {
    orc.test.item.distrib.WordCount.listFileNamesRecursively(new File(dirPathName)).toList
  }

  def createTestDataFiles() = {
    orc.test.item.distrib.WordCount.createTestFiles(holmesDataDirPath, inputDataDirPath, targetFileSize, numCopiesInputFiles)
  }

  // Lines: 6
  def countFile(file: File): Long = {
    checkReadableFile(file)
    val in = new BufferedReader(new FileReader(file))
    val count = orc.test.item.distrib.WordCount.countReader(in)
    in.close()
    count
  }

  // Lines: 4
  def repeatCountFilename(filename: String) = {
    def sumN(n: Int, f: () => Long): Long = if (n > 0) f() + sumN(n-1, f) else 0
    val file = new File(filename)
    sumN(repeatRead, () => countFile(file))
  }


  // Lines: 3
  def benchmark(data: List[String]) = {
    val wordCountList = data.par.map(repeatCountFilename)
    wordCountList.sum
  }

  def setup(): List[String] = {
    // Setup for all iterations.
    createTestDataFiles()

    listFileNamesRecursively(inputDataDirPath).take(size)
  }

  val name: String = "WordCount-par"

  val size: Int = 24

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x4c86c00,
      10 -> 0x4c86c00,
      100 -> 0x4c86c00,
      )
}
