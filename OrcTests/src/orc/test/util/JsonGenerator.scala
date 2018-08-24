//
// JsonGenerator.scala -- Scala object JsonGenerator
// Project OrcTests
//
// Created by jthywiss on Sep 5, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import scala.collection.JavaConverters.{ dictionaryAsScalaMapConverter, enumerationAsScalaIteratorConverter, iterableAsScalaIterableConverter, mapAsScalaMapConverter }

/** Generates JavaScript Object Notation (JSON) per RFC 7159 and ECMA-262.
  *
  * Strings and chars are written as strings, integer and floating point
  * types are written as "numbers", booleans are written as booleans, and
  * null is written as "null". Maps and Dictionaries are written as
  * "objects", and other Traversables, Iterables, and Enumerations are
  * written as "arrays".  All other types cause an exception.
  *
  * @author jthywiss
  */
object JsonGenerator {

  def apply(out: Appendable)(value: Any): Unit = {
    serializeJsonProperty(out)(value, "", "  ")
  }

  def serializeJsonProperty(out: Appendable)(value: Any, currIndent: String, addlIndent: String): Unit = {
    value match {
      //Maybe: case (object with toJson method) => object.toJson()
      case null => out.append("null")
      case true => out.append("true")
      case false => out.append("false")
      case s: String => writeQuotedJsonString(out)(s)
      case c: Char => writeQuotedJsonString(out)(c.toString)
      case _: Byte | _: Short | _: Int | _: Long | _: Float | _: Double => out.append(value.toString)
      case n: Number => out.append(n.toString)
      case o: scala.collection.Map[_,_] => serializeJsonObject(out)(o, currIndent, addlIndent)
      case o: java.util.Map[_,_] => serializeJsonObject(out)(o.asScala, currIndent, addlIndent)
      case a: TraversableOnce[_] => serializeJsonArray(out)(a, currIndent, addlIndent)
      case a: java.lang.Iterable[_] => serializeJsonArray(out)(a.asScala, currIndent, addlIndent)
      case a: java.util.Enumeration[_] => serializeJsonArray(out)(a.asScala, currIndent, addlIndent)
      case _ => throw new IllegalArgumentException(s"Can't write a ${value.getClass.getName} as JSON: $value")
      //Maybe: case _ => out.append(quoteJsonString(value.toString)) /* Fallback to toString */
    }
  }

  def writeQuotedJsonString(out: Appendable)(str: String): Unit = {
    out.append("\"")
    str.foreach(_ match {
      case '"' => out.append("\\\"")
      case '\\' => out.append("\\\\")
      case '\b' => out.append("\\b")
      case '\f' => out.append("\\f")
      case '\n' => out.append("\\n")
      case '\r' => out.append("\\r")
      case '\t' => out.append("\\t")
      case ch if ch < ' ' => {
        out.append("\\u")
        out.append(("0000" + ch.toHexString).takeRight(4))
      }
      case ch => out.append(ch)
    })
    out.append("\"")
  }

  def serializeJsonObject(out: Appendable)(value: Iterable[(_,_)], currIndent: String, addlIndent: String): Unit = {
    if (value.isEmpty) {
      out.append("{}")
    } else {
      val newIndent = currIndent + addlIndent
      var first = true

      val sortedProperties = scala.collection.mutable.SortedMap[String,Any]()
      value.foreach({ kv =>
        sortedProperties.put(kv._1.toString, kv._2)
      })

      out.append("{\n")
      out.append(newIndent)
      sortedProperties.foreach({ kv =>
        if (first) {
          first = false
        } else {
          out.append(",\n" + newIndent)
        }
        writeQuotedJsonString(out)(kv._1)
        out.append(": " )
        serializeJsonProperty(out)(kv._2, newIndent, addlIndent)
      })
      out.append("\n")
      out.append(currIndent)
      out.append("}")
    }
  }

  def serializeJsonArray(out: Appendable)(value: TraversableOnce[Any], currIndent: String, addlIndent: String): Unit = {
    if (value.isEmpty) {
      out.append("[]")
    } else {
      val newIndent = currIndent + addlIndent
      var first = true

      out.append("[\n" + newIndent)
      value.foreach({ element =>
        if (first) {
          first = false
        } else {
          out.append(",\n" + newIndent)
        }
        serializeJsonProperty(out)(element, newIndent, addlIndent)
      })
      out.append("\n" + currIndent + "]")
    }
  }

}
