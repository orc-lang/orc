//
// OrcProgram.java -- Scala class/trait/object OrcProgram
// Project OrcScala
//
// $Id$
//
// Created by amp on Jan 8, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.tojava;

import orc.Main;
import orc.OrcExecutionOptions;
import orc.run.StandardOrcRuntime;
import orc.util.PrintVersionAndMessageException;

/**
 * A Java base class for Orc programs. This This provides root context
 * generation and a utility method for implementation main in the subclass.
 * 
 * @author amp
 */
abstract public class OrcProgram implements Callable {
  /**
   * Run this program using the given runtime.
   */
  public RootContext run(final StandardOrcRuntime runtime) {
    final ToJavaRuntime tjruntime = new ToJavaRuntime(runtime);
    final RootContext ctx = new RootContext(tjruntime);
    runtime.schedule(new CounterSchedulableRunnable(ctx.c(), new Runnable() {
      @Override
      public void run() {
        call(tjruntime, ctx.p(), ctx.c(), ctx.t(), new Object[] {});
      }
    }));
    ctx.c().halt();
    return ctx;
  }

  /**
   * Run the given program instance without a runtime.
   * 
   * @param args The command line arguments used to start this program.
   * 
   * @throws PrintVersionAndMessageException
   */
  public static void runProgram(final String[] args, final OrcProgram prog) throws PrintVersionAndMessageException {
    final OrcCmdLineOptions options = new OrcCmdLineOptions();
    options.parseRuntimeCmdLine(args);
    Main.setupLogging(options);
    final StandardOrcRuntime runtime = new StandardOrcRuntime("ToJava");
    runtime.startScheduler(options);
    prog.run(runtime);
  }

}
