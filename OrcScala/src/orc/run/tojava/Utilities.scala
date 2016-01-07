package orc.run.tojava

import orc.values.sites.Site
import orc.error.compiletime.SiteResolutionException
import orc.script.OrcBindings
import orc.CmdLineOptions
import orc.util.PrintVersionAndMessageException
import orc.util.CmdLineUsageException

/** @author amp
  */
object Utilities {
  def resolveOrcSite(n: String): Site = {
    try {
      return orc.values.sites.OrcSiteForm.resolve(n);
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }

  def Nil: List[AnyRef] = scala.Nil
  def Cons(h: AnyRef, t: List[AnyRef]): List[AnyRef] = h :: t
}

class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions {
  @throws(classOf[PrintVersionAndMessageException])
  @throws(classOf[CmdLineUsageException])
  def parseRuntimeCmdLine(args: Array[String]) {
    parseCmdLine(args.toSeq :+ "UNUSED FILE ARGUMENT")
  }
}