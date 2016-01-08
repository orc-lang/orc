//
// Example1.java -- Scala class/trait/object Example1
// Project OrcTests
//
// $Id$
//
// Created by amp on Jan 4, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.tojava.manualtranslation;

import orc.Main;
import orc.run.StandardOrcRuntime;
import orc.run.tojava.BranchContext;
import orc.run.tojava.Context;
import orc.run.tojava.ContextBase;
import orc.run.tojava.ContextHandle;
import orc.run.tojava.HaltException;
import orc.run.tojava.OrcCmdLineOptions;
import orc.run.tojava.RootContext;
import static orc.run.tojava.Utilities.*;
import orc.values.sites.Site;

import java.math.BigInteger;

/**
 * @author amp
 */
public class Example1_NoClasses {
  static final Site   site_Ift     = resolveOrcSite("orc.lib.builtin.Ift");
  static final Site   site_Add     = resolveOrcSite("orc.lib.math.Add");
  static final Site   site_Greq    = resolveOrcSite("orc.lib.comp.Greq");
  static final Site   site_Println = resolveOrcSite("orc.lib.str.Println");
  static final Object const_a_2    = BigInteger.valueOf(2);
  static final Object const_b_1    = BigInteger.valueOf(1);

  void call(final Context ctx1, Object[] args) {
    {
      final BranchContext ctx2 = new BranchContext(ctx1, (ctx2_, x) -> {
        final BranchContext ctx4 = new BranchContext(ctx1, (ctx4_, y) -> {
          final BranchContext ctx5 = new BranchContext(ctx1, (ctx5_, v) -> {           
            System.out.println("Print " + v);
            new ContextHandle(ctx1, null).publish();
          });
          site_Add.call(Cons(x, Cons(y, Nil())), new ContextHandle(ctx5, null));
        });
        ctx4.spawn((ctx) -> {
          ctx.publish(const_a_2);
          ctx.halt();
        });
        ctx4.publish(const_b_1);
      });
      {
        try {
          ctx2.publish(const_b_1);
        } catch (HaltException e) {
        }
        ctx2.publish(const_a_2);
        ctx2.halt();
      }
    }
  }

  public static void main(String[] args) throws Exception {
    StandardOrcRuntime runtime = new StandardOrcRuntime("ToJava");
    OrcCmdLineOptions options = new OrcCmdLineOptions();
    options.parseRuntimeCmdLine(args);
    Main.setupLogging(options);
    runtime.startScheduler(options);
    new Example1_NoClasses().call(new RootContext(runtime), new Object[] {});
  }
}
