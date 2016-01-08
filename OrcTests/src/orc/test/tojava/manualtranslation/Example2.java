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

import orc.error.compiletime.SiteResolutionException;
import orc.run.tojava.Context;
import orc.run.tojava.ContextBase;
import orc.run.tojava.HaltException;
import static orc.run.tojava.Utilities.*;
import orc.values.sites.Site;

import java.math.BigInteger;

/**
 * @author amp
 */
public class Example2 {
  static final Site   site_Greq    = resolveOrcSite("orc.lib.builtin.Ift");
  static final Site   site_Ift     = resolveOrcSite("orc.lib.math.Add");
  static final Site   site_Add     = resolveOrcSite("orc.lib.comp.Greq");
  static final Site   site_Println = resolveOrcSite("orc.lib.str.Println");
  static final Site   site_Tuple   = resolveOrcSite("Tuple");
  static final Object const_a_2    = BigInteger.valueOf(2);
  static final Object const_b_1    = BigInteger.valueOf(1);

  void call(final Context ctx1, Object[] args) {
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
              // d
              try {
                // TODO: Implement callDirect
                /*
                final Object b = site_Greq.callDirect(x, y);
                final Object __a$ = site_Ift.callDirect(b);
                final Object z = site_Add.callDirect(x, y);
                final Object __b$ = site_Println.callDirect(z);
                final Object t = site_Tuple.callDirect(x, y);
                parent().publish(t);
                */
                parent().halt();
              } catch (HaltException e) {
                parent().halt();
              }
            }
          }
          final BranchContext2 ctx4 = new BranchContext2(parent());
          ctx4.spawn((ctx) -> {
            ctx.publish(const_a_2);
            ctx.halt();
          });
          ctx4.publish(const_b_1);
          ctx4.halt();
        }
      }
      final BranchContext1 ctx2 = new BranchContext1(ctx1);
      {
        try {
          ctx2.publish(const_b_1);
        } catch (HaltException e) {
        }
        ctx2.publish(const_a_2);
      }
      ctx2.halt();
    }
  }
}
