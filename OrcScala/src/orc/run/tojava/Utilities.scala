package orc.run.tojava

import orc.values.sites.Site
import orc.error.compiletime.SiteResolutionException
import orc.script.OrcBindings
import orc.CmdLineOptions
import orc.util.PrintVersionAndMessageException
import orc.util.CmdLineUsageException

class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions {
  @throws(classOf[PrintVersionAndMessageException])
  @throws(classOf[CmdLineUsageException])
  def parseRuntimeCmdLine(args: Array[String]) {
    parseCmdLine(args.toSeq :+ "UNUSED FILE ARGUMENT")
  }
}