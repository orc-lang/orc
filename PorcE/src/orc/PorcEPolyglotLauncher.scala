//
// PorcEPolyglotLauncher.scala -- Scala object PorcEPolyglotLauncher
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

//import scala.collection.JavaConverters._
import orc.run.porce.PorcELanguage
import orc.script.OrcBindings
import java.io.File
import java.io.PrintWriter
import orc.util.ExecutionLogOutputStream
import java.io.OutputStreamWriter
import orc.util.DumperRegistry
import org.graalvm.polyglot.{ Context, Source }

object PorcEPolyglotLauncher {
  class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions

  var orcOptions: Option[OrcOptions] = None

  def main(args: Array[String]): Unit = {
    val options = new OrcCmdLineOptions()
    options.parseCmdLine(args)
    orcOptions = Some(options)
    val context = Context.newBuilder().out(System.out).err(System.err).build()
    val engine = context.getEngine()

    val profilePorcNodes = true && System.getProperty("orc.executionlog.dir") != null

    if (profilePorcNodes) {
      import orc.run.porce.instruments.PorcNodeExecutionProfiler
      val profiler = PorcNodeExecutionProfiler.get(engine)

      DumperRegistry.register { name =>
        val out = ExecutionLogOutputStream(s"porc-profile-$name", "csv", "Porc profile dump")
        if (out.isDefined) {
          val pout = new PrintWriter(new OutputStreamWriter(out.get))
          profiler.dump(pout)
        }
        profiler.reset()
      }
    }

    val profilePorcENodes = false && System.getProperty("orc.executionlog.dir") != null

    if (profilePorcENodes) {
      import orc.run.porce.instruments.PorcENodeExecutionProfiler
      val profiler = PorcENodeExecutionProfiler.get(engine)

      DumperRegistry.register { name =>
        val out = ExecutionLogOutputStream(s"porce-profile-$name", "csv", "Porc profile dump")
        if (out.isDefined) {
          val pout = new PrintWriter(new OutputStreamWriter(out.get))
          profiler.dump(pout)
        }
        profiler.reset()
      }
    }

    val profilePorcEClasses = false && System.getProperty("orc.executionlog.dir") != null

    if (profilePorcEClasses) {
      import orc.run.porce.instruments.PorcENodeClassExecutionProfiler
      val profiler = PorcENodeClassExecutionProfiler.get(engine)

      DumperRegistry.register { name =>
        val out = ExecutionLogOutputStream(s"porce-class-profile-$name", "csv", "PorcE class profile dump")
        if (out.isDefined) {
          val pout = new PrintWriter(new OutputStreamWriter(out.get))
          profiler.dump(pout)
        }
        profiler.reset()
      }
    }

    context.eval(Source.newBuilder("orc", new File(options.filename)).mimeType(PorcELanguage.MIME_TYPE).build())

    engine.close()
    context.close()
    System.exit(0);
  }
}
