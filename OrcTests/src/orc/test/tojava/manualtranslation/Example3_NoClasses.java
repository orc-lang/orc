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
import orc.run.tojava.OrcProgram;

/**
 * @author amp
 */
public class Example3_NoClasses extends OrcProgram {
  static final Callable site_Add     = orc.run.tojava.Callable$.MODULE$.resolveOrcSite("orc.lib.math.Add");
  static final Object   const_a_2    = BigInteger.valueOf(2);
  static final Object   const_b_1    = BigInteger.valueOf(1);

  @Override
  public void call(final Context ctx1) {
    // [def f(x) = x + 2 # f(1) | f(2)]
    final Callable f = (ctx, args) -> {
      Object x = args[0];
      // [x + 2]
      site_Add.call(ctx, new Object[] { x, const_a_2 });
    };
    // [f(1) | f(2)]
    ctx1.spawn((ctx) -> {
      f.call(ctx, new Object[] { const_a_2 });
    });
    f.call(ctx1, new Object[] { const_b_1 });
  }

  public static void main(String[] args) throws Exception {
    runProgram(args, new Example3_NoClasses());
  }
}
