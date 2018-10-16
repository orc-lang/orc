//
// GetScalaTypeName.scala -- Scala object GetScalaTypeName
// Project OrcScala
//
// Created by jthywiss on Oct 15, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

/** Gets the Scala syntax of the type of the given value (approximately).
  * Does not attempt to look up class manifests, etc.
  *
  * @author jthywiss
  */
object GetScalaTypeName {
  def apply(v: Any) = {
    v match {
      case null => "Null"
      case o: Object => Option(o.getClass.getCanonicalName).getOrElse(o.getClass.getName).stripSuffix("$") /*TODO: Convert arrays etc. to Scala syntax? */
      case _: Boolean => "Boolean"
      case _: Byte => "Byte"
      case _: Char => "Char"
      case _: Double => "Double"
      case _: Float => "Float"
      case _: Int => "Int"
      case _: Long => "Long"
      case _: Short => "Short"
      case _: Unit => "Unit"
      case _ => "[type of \"" + v + "\"]"
    }
  }
}
