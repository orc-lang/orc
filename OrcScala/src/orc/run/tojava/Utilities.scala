package orc.run.tojava

import orc.CmdLineOptions
import orc.script.OrcBindings
import orc.util.{ CmdLineUsageException, PrintVersionAndMessageException }

/** A subclass of the command line parser which provides a method for calling
  * from Java 8.
  */
class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions {
  @throws(classOf[PrintVersionAndMessageException])
  @throws(classOf[CmdLineUsageException])
  def parseRuntimeCmdLine(args: Array[String]) {
    parseCmdLine(args.toSeq :+ "UNUSED FILE ARGUMENT")
  }
}