//
// CumNormalInv.scala -- Scala object CumNormalInv
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.swaptions

object CumNormalInv {

  private[this] val a = Array(
    2.50662823884,
    -18.61500062529,
    41.39119773534,
    -25.44106049637)

  private[this] val b = Array(
    -8.47351093090,
    23.08336743743,
    -21.06224101826,
    3.13082909833)

  private[this] val c = Array(
    0.3374754822726147,
    0.9761690190917186,
    0.1607979714918209,
    0.0276438810333863,
    0.0038405729373609,
    0.0003951896511919,
    0.0000321767881768,
    0.0000002888167364,
    0.0000003960315187)

  def apply(u: Double): Double = {
    // Returns the inverse of cumulative normal distribution function.
    // Reference: Moro, B., 1995, "The Full Monte," RISK (February), 57-58.

    var r: Double = 0

    val x = u - 0.5;
    if (x.abs < 0.42) {
      r = x * x
      r = x * (((a(3) * r + a(2)) * r + a(1)) * r + a(0)) /
        ((((b(3) * r + b(2)) * r + b(1)) * r + b(0)) * r + 1.0);
      return (r);
    }

    r = u;
    if (x > 0.0) r = 1.0 - u;
    r = math.log(-math.log(r));
    r = c(0) + r * (c(1) + r *
      (c(2) + r * (c(3) + r *
        (c(4) + r * (c(5) + r * (c(6) + r * (c(7) + r * c(8))))))));
    if (x < 0.0) r = -r;

    r
  }

}
