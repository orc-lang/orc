package orc.test.item.scalabenchmarks.dedup

import java.net.URL
import java.io.File
import java.nio.file.Files

object DedupData {
  val dataURL = new URL("ftp://ftp.utexas.edu/pub/ubuntu-iso/zesty/ubuntu-17.04-server-i386.iso")
  private val localTargetFile = "dedup-input.iso"
  lazy val localInputFile = {
    val f = new File(localTargetFile)
    if (!f.isFile()) {
      println(s"Downloading $dataURL as test data")
      val in = dataURL.openStream()
      Files.copy(in, f.toPath())
      in.close()
    }
    f.getAbsolutePath
  }
  
  val localOutputFile = "dedup-output.dat"
}