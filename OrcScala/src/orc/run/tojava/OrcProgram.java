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
import orc.run.StandardOrcRuntime;
import orc.util.PrintVersionAndMessageException;

/**
 * 
 *
 * @author amp
 */
abstract public class OrcProgram {
  public abstract void call(Context ctx);

  /**
   * @param args
   * @throws PrintVersionAndMessageException
   */
  public static void runProgram(final String[] args, final OrcProgram prog) throws PrintVersionAndMessageException {
    final StandardOrcRuntime runtime = new StandardOrcRuntime("ToJava");
    final OrcCmdLineOptions options = new OrcCmdLineOptions();
    options.parseRuntimeCmdLine(args);
    Main.setupLogging(options);
    runtime.startScheduler(options);
    final Context ctx = new RootContext(runtime);
    runtime.schedule(new ContextSchedulableRunnable(ctx, new Runnable() {
      @Override
      public void run() {
        prog.call(ctx);
      }
    }));
    ctx.halt();
  }

}
