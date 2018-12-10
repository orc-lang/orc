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
  def apply(v: Boolean): String = "Boolean"
  def apply(v: Byte): String = "Byte"
  def apply(v: Char): String = "Char"
  def apply(v: Double): String = "Double"
  def apply(v: Float): String = "Float"
  def apply(v: Int): String = "Int"
  def apply(v: Long): String = "Long"
  def apply(v: Short): String = "Short"

  def apply(v: Any): String = {
    v match {
      case null => "Null"
      case _: Boolean => "Boolean"
      case _: Byte => "Byte"
      case _: Char => "Char"
      case _: Double => "Double"
      case _: Float => "Float"
      case _: Int => "Int"
      case _: Long => "Long"
      case _: Short => "Short"
      case _: Unit => "Unit"
      case o: Object => ofClass(o.getClass)
      case _ => "[type of \"" + v + "\"]"
    }
  }

  def ofClass(clazz: Class[_]): String = {
    clazz match {
      case java.lang.Boolean.TYPE => "Boolean"
      case java.lang.Byte.TYPE => "Byte"
      case java.lang.Character.TYPE => "Char"
      case java.lang.Double.TYPE => "Double"
      case java.lang.Float.TYPE => "Float"
      case java.lang.Integer.TYPE => "Int"
      case java.lang.Long.TYPE => "Long"
      case java.lang.Short.TYPE => "Short"
      case java.lang.Void.TYPE => "Unit"
      case _ if clazz.isArray => "Array[" + ofClass(clazz.getComponentType) + "]"
      case _ => (clazz.getEnclosingClass match {
        case null => { // Top-level class: Name is OK
          stripPackageName(clazz.getName) 
        }
        case ec if clazz.getDeclaringClass != null => { // Nested or inner class
          ofClass(ec) + "." + clazz.getName.substring(ec.getName.length + 1)
        }
        case _ => { // Local or anonymous class: Just use JVM binary name
          stripPackageName(clazz.getName)
        }
      }).stripSuffix("$")
    }
  }

  private def stripPackageName(binaryName: String): String = binaryName.substring(binaryName.lastIndexOf(".")+1)
}
