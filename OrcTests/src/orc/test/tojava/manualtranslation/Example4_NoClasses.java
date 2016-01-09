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
import orc.run.tojava.ContextHandle;
import orc.run.tojava.KilledException;
import orc.run.tojava.OrcProgram;
import orc.run.tojava.TerminatorContext;
import orc.values.Field;

/**
 * @author amp
 */
public class Example4_NoClasses extends OrcProgram {
  static final Callable site_Ift                = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.builtin.Ift");
  static final Callable site_Add                = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.math.Add");
  static final Callable site_Sub                = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.math.Sub");
  static final Callable site_Greq               = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.comp.Greq");
  static final Callable site_Example4_NoClasses = orc.run.tojava.Callable$.MODULE$.resolveJavaSite(Example4_NoClasses.class.getCanonicalName());
  static final Object   const_a_2               = BigInteger.valueOf(2);
  static final Object   const_b_1               = BigInteger.valueOf(1);

  @Override
  public void call(final Context ctx1) {
    final BranchContext ctx2 = new BranchContext(ctx1, (ctx2_, s) -> {
      final TerminatorContext ctx3 = new TerminatorContext(ctx1);
      try {
        // C We now call an expression the normal way but in the new
        // context
        // [x + y | x - y]
        ctx3.spawn((ctx) -> {
          try {
            Thread.sleep((int) (Math.random() * 100));
          } catch (Exception e) {
            e.printStackTrace();
          }
          orc.run.tojava.Callable$.MODULE$.coerceToCallable(s).call(ctx, new Object[] { "Test 1" });
        });
        try {
          Thread.sleep((int) (Math.random() * 100));
        } catch (Exception e) {
          e.printStackTrace();
        }
        orc.run.tojava.Callable$.MODULE$.coerceToCallable(s).call(ctx3, new Object[] { "Test 2" });
      } catch (KilledException e) {
        // just go from here. The exception means that the part of the
        // expression running in this thread has halted, but nothing else.
      }
    });
    site_Example4_NoClasses.call(ctx2, new Object[] { new Field("Println") });
  }

  public static void main(String[] args) throws Exception {
    runProgram(args, new Example4_NoClasses());
  }

  public static String Println(String s) {
    System.out.println("Printing " + s);
    return s;
  }
}
