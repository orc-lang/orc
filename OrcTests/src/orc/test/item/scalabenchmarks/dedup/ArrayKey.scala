//
// ArrayKey.scala -- Scala class ArrayKey
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.dedup

import java.util.Arrays

class ArrayKey(val array: Array[Byte]) {
  override def hashCode() = Arrays.hashCode(array)
  override def equals(o: Any): Boolean = o match {
    case o: ArrayKey => Arrays.equals(array, o.array)
    case _ => false
  }
  
  override def toString(): String = {
    array.view.map(b => (b.toInt & 0xFF).formatted("%02x")).mkString("")
  }
}
