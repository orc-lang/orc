//
// FirstNonNull.scala -- Scala object FirstNonNull
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 26, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

/** Applied to a list of possibly null arguments, returns the
  * first non-null argument, or null if they are all null.
  *
  * @author jthywiss
  */
object FirstNonNull {
  // When Scala ticket #237 <https://lampsvn.epfl.ch/trac/scala/ticket/237> is fixed
  // These can become just:
  // def apply[T](alternatives: (=> T)*): T

  def apply[T](alternative1: => T, alternative2: => T): T = {
    val alt1 = alternative1
    if (alt1 != null) alt1 else alternative2
  }

  def apply[T](alternative1: => T, alternative2: => T, alternative3: => T): T = {
    val alt1 = alternative1
    if (alt1 != null) alt1 else apply(alternative2, alternative3)
  }

  def apply[T](alternative1: => T, alternative2: => T, alternative3: => T, alternative4: => T): T = {
    val alt1 = alternative1
    if (alt1 != null) alt1 else apply(alternative2, alternative3, alternative4)
  }

  def apply[T](alternative1: => T, alternative2: => T, alternative3: => T, alternative4: => T, alternative5: => T): T = {
    val alt1 = alternative1
    if (alt1 != null) alt1 else apply(alternative2, alternative3, alternative4, alternative5)
  }

}
