package orc.test.item.scalabenchmarks.dedup

import java.io.{ File, FileInputStream }
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Arrays

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

  def check() = {
    val dataIn = new FileInputStream(DedupData.localOutputFile)
    val buf = Array.ofDim[Byte](1024 * 4)
    val dst = MessageDigest.getInstance("MD5")
    case object Done extends Exception
    try {
      while(true) {
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
    println(h.formatted("%x"))
    h == 0xc991c9e1
    //fc0e31a0
  }
}