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

import orc.run.tojava.Callable;
import orc.run.tojava.Context;
import orc.run.tojava.ContextBase;
import orc.run.tojava.ContextHandle;
import orc.run.tojava.CounterContextBase;
import orc.run.tojava.OrcProgram;
import orc.values.sites.Site;

/**
 * @author amp
 */
public class Example1 extends OrcProgram {
  static final Callable site_Ift     = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.builtin.Ift");
  static final Callable site_Add     = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.math.Add");
  static final Callable site_Sub     = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.math.Sub");
  static final Callable site_Greq    = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.comp.Greq");
  static final Callable site_Println = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.str.Println");
  static final Object const_a_2    = BigInteger.valueOf(2);
  static final Object const_b_1    = BigInteger.valueOf(1);

  @Override
  public void call(final Context ctx1) {
    // [(1 ;; 2) >x> (1 | 2) >y> x + y >v> Println("Print " + v)]
    {
      final class BranchContext1 extends ContextBase {
        BranchContext1(Context ctx) {
          super(ctx);
        }

        @Override
        public void publish(final Object x) {
          final class BranchContext2 extends ContextBase {
            BranchContext2(Context ctx) {
              super(ctx);
            }

            @Override
            public void publish(final Object y) {
              // [x + y >v> Println(v)]
              final class BranchContext3 extends ContextBase {
                BranchContext3(Context ctx) {
                  super(ctx);
                }

                @Override
                public void publish(final Object v) {
                  // [Println(v)]
                  //site_Println.call(Cons("Print " + v, Nil()), new ContextHandle(parent(), null));
                  System.out.println("Print " + v);
                  new ContextHandle(parent(), null).publish();
                }
              }
              final BranchContext3 ctx5 = new BranchContext3(parent());
              site_Add.call(ctx5, new Object[] { x, y });
            }
          }
          final BranchContext2 ctx4 = new BranchContext2(parent());
          ctx4.spawn((ctx) -> {
            ctx.publish(const_a_2);
          });
          ctx4.publish(const_b_1);
        }
      }
      final BranchContext1 ctx2 = new BranchContext1(ctx1);
      {
        //C We have 1 count in oldctx
        final class CounterContext1 extends CounterContextBase {
          CounterContext1(Context ctx) {
            super(ctx);
          }
          
          @Override
          public void onContextHalted() {
            ctx2.publish(const_a_2);
            super.onContextHalted();
          }
        }
        CounterContext1 ctx6 = new CounterContext1(ctx2);
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
    runProgram(args, new Example1());
  }
}
