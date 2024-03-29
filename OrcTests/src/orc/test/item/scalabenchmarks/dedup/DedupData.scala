//
// DedupData.scala -- Scala benchmark component DedupData
// Project OrcTests
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.dedup

import java.io.FileInputStream
import java.net.URL
import java.nio.file.{ Files, Paths }
import java.security.MessageDigest
import java.util.Arrays

object DedupData {
  val dataURL = new URL("http://old-releases.ubuntu.com/releases/17.04/ubuntu-17.04-server-i386.iso")
  private val localTargetFile = "dedup-input.iso"
  lazy val localInputFile = {
    val f = Paths.get(localTargetFile)
    if (!Files.isRegularFile(f)) {
      println(s"Downloading $dataURL as test data")
      val in = dataURL.openStream()
      Files.copy(in, f)
      in.close()
    }
    f.toAbsolutePath.toString
  }

  val localOutputFile = "dedup-output.dat"

  def check() = {
    val dataIn = new FileInputStream(DedupData.localOutputFile)
    val buf = Array.ofDim[Byte](1024 * 4)
    val dst = MessageDigest.getInstance("MD5")
    case object Done extends Exception
    try {
      while (true) {
        val n = dataIn.read(buf)
        if (n > 0)
          dst.update(buf, 0, n)
        else
          throw Done
      }
    } catch {
      case Done => ()
    }
    val h = Arrays.hashCode(dst.digest())
    println(h.formatted("%08x"))
    h == 0xc991c9e1
  }
}
