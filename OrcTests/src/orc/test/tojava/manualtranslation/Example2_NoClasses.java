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

import java.math.BigInteger;

import orc.run.tojava.BranchContext;
import orc.run.tojava.Callable;
import orc.run.tojava.Context;
import orc.run.tojava.Future;
import orc.run.tojava.OrcProgram;

/**
 * @author amp
 */
public class Example2_NoClasses extends OrcProgram {
  static final Callable site_Ift     = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.builtin.Ift");
  static final Callable site_Add     = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.math.Add");
  static final Callable site_Sub     = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.math.Sub");
  static final Callable site_Greq    = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.comp.Greq");
  static final Callable site_Println = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.str.Println");
  static final Object   const_a_2    = BigInteger.valueOf(2);
  static final Object   const_b_1    = BigInteger.valueOf(1);

  @Override
  public void call(final Context ctx1) {
    // [future(Rwait(100) >> 1 | 2) >f> ("Test" | force(f) >v> "Val " + v)
    final BranchContext ctx2 = new BranchContext(ctx1, (ctx2_, f) -> {
      // ["Test" | force(f) >v> "Val " + v]
        ctx1.spawn((ctx) -> {
          ctx.publish("Test");
        });
        // [force(f) >v> "Val " + v]
        final BranchContext ctx3 = new BranchContext(ctx1, (ctx3_, v) -> {
          site_Add.call(ctx1, new Object[] { "Val ", v });
        });
        ((Future) f).forceIn(ctx3);
      });
    // [future(Rwait(100) >> 1 | 2)]
    Future fut = ctx2.spawnFuture((ctx4) -> {
      // [Rwait(100) >> 1 | 2]
        final BranchContext ctx5 = new BranchContext(ctx4, (ctx5_, v) -> {
          // [1 | 2]
            ctx4.spawn((ctx) -> {
              ctx.publish(const_a_2);
            });
            ctx4.publish(const_b_1);
          });
        // [Rwait(100)]
        try {
          Thread.sleep(5000);
        } catch (Exception e) {
        }
        ctx5.publish(0);
      });
    ctx2.publish(fut);
  }

  public static void main(String[] args) throws Exception {
    runProgram(args, new Example2_NoClasses());
  }
}
