package orc.util

import java.io.FileWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class DataLogger(output: DataOutput, identity: Seq[(String, String)]) {  
  def subLogger(identity: Seq[(String, String)]) = {
    require(identity.map(_._1).forall(k => !this.identity.map(_._1).contains(k)))
    new DataLogger(output, this.identity ++ identity)
  }
  
  def log(variable: String, value: Double): Unit = {
    val fields = identity :+ ("variable" -> variable) :+ ("value" -> value.toString()) 
    output.writeData(fields)
  }
}


abstract class DataOutput {
  var expectedKeys: Option[Seq[String]] = None
    
  private def writeHeader(data: Seq[(String, String)]): Unit = {
    expectedKeys = Some(data.map(_._1))
    writeData(data.map(v => (v._1, v._1)))
  }

  def writeData(data: Seq[(String, String)]): Unit = {
    expectedKeys match {
      case Some(ks) =>
        require(data.map(_._1).toSeq == ks)
      case None =>
        writeHeader(data)
    }
    writeLine(data.map(_._2).mkString("\t"))
  }
  
  def logger(identity: Seq[(String, String)]) = {
    new DataLogger(this, identity)
  }
  
  def close(): Unit  
  protected def writeLine(s: String): Unit
}

class FileDataOutput(filename: File, echo: Boolean = false) extends DataOutput {
  val writer = new FileWriter(filename)
  
  def writeLine(s: String): Unit = {
    if(echo)
      println(s"$filename< $s\n")
    writer.write(s)
    writer.write('\n')
  }

  def close(): Unit = {
    writer.close()
  }
}

class AutoFileDataOutput(prefix: String, echo: Boolean = false) extends FileDataOutput(AutoFileDataOutput.makeFilename(prefix), echo) {
}

object AutoFileDataOutput {
  private val dateFormatter = new SimpleDateFormat("yyyyMMdd-HHmm")
  private def makeFilename(prefix: String): File = {
    for(n <- 0 to 100) {
      val f = new File(s"${prefix}_${dateFormatter.format(new Date())}-$n.tsv")
      if(!f.exists())
        return f
    }
    throw new AssertionError("Could not find unused file time in 100 choices. Something is probably terribly wrong.")
  }
}