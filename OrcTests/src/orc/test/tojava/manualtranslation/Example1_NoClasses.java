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

import static orc.run.tojava.Utilities.Cons;
import static orc.run.tojava.Utilities.Nil;
import static orc.run.tojava.Utilities.resolveOrcSite;

import java.math.BigInteger;

import orc.run.tojava.BranchContext;
import orc.run.tojava.Context;
import orc.run.tojava.ContextHandle;
import orc.run.tojava.CounterContext;
import orc.run.tojava.OrcProgram;
import orc.values.sites.Site;

/**
 * @author amp
 */
public class Example1_NoClasses extends OrcProgram {
  static final Site   site_Ift     = resolveOrcSite("orc.lib.builtin.Ift");
  static final Site   site_Add     = resolveOrcSite("orc.lib.math.Add");
  static final Site   site_Greq    = resolveOrcSite("orc.lib.comp.Greq");
  static final Site   site_Println = resolveOrcSite("orc.lib.str.Println");
  static final Object const_a_2    = BigInteger.valueOf(2);
  static final Object const_b_1    = BigInteger.valueOf(1);

  @Override
  public void call(final Context ctx1) {
    // [(1 ;; 2) >x> (1 | 2) >y> x + y >v> Println("Print " + v)]
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
        });
        ctx4.publish(const_b_1);
      });
      {
        //C We have 1 count in oldctx
        CounterContext ctx6 = new CounterContext(ctx2, (ctx) -> ctx2.publish(const_a_2));
        //C Now we have 2 counts in oldctx. One for this execution and one for the nested counter.
        try {
          ctx6.publish(const_b_1);
        } finally {
          // The finally is required so that the counter is properly decr when a kill happens.
          //C We are leaving the scope of ctx. f may not be completed. But we can notify that this execution has stopped.
          ctx6.halt();
        }
        //C we return the count in oldctx to the caller.
      }

    }
  }


  public static void main(String[] args) throws Exception {
    runProgram(args, new Example1_NoClasses());
  }
}
