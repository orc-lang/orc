//
// OrcProgram.java -- Scala class/trait/object OrcProgram
// Project OrcScala
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
import orc.OrcEvent;
import orc.run.StandardOrcRuntime;
import orc.util.PrintVersionAndMessageException;
import scala.Function1;
import scala.NotImplementedError;
import scala.runtime.BoxedUnit;

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
  public Execution run(final ToJavaRuntime tjruntime, Function1<OrcEvent, BoxedUnit> k) {
    final Execution ctx = new Execution(tjruntime, k);
    tjruntime.schedule(new CounterSchedulableRunnable(ctx.c(), new Runnable() {
      @Override
      public void run() {
        try {
          call(ctx, ctx.p(), ctx.c(), ctx.t(), new Object[] {});
        } catch (KilledException e) {
        }
      }
    }));
    // Matched to: Initial count of Execution created above.
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
    throw new NotImplementedError("Implement this");
    //prog.run(runtime);
  }

}
