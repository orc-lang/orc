//
// WriteJSON.scala -- Scala object WriteJSON
// Project OrcScala
//
// Created by dkitchin on Mar 3, 2011.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import orc.error.runtime.SiteException
import orc.values.OrcRecord
import orc.values.sites.{ UntypedSite }
import orc.values.sites.TotalSite1Simple


/**
  * @author dkitchin
  */
object WriteJSON extends TotalSite1Simple[AnyRef] with UntypedSite {

  def eval(a: AnyRef): AnyRef = {
    convertToJson(a)
  }

  def convertToJson(a: Any): String = {
    if (a == null) { return "null" }
    a match {
      case l: List[_] => l map convertToJson mkString ("[", ",", "]")
      case OrcRecord(elements) => {
        val newElements =
          for ((key, value) <- elements) yield {
            "\"" + key + "\"" + ":" + convertToJson(value)
          }
        newElements mkString ("{", ",", "}")
      }
      case b: java.lang.Boolean => if (b.booleanValue()) { "true" } else { "false" }
      case n: Number => n.toString()
      case s: String => "\"" + jsonEncodedString(s) + "\""
      case v => throw new SiteException("Value " + v + " has no JSON counterpart.")
    }
  }

  def jsonEncodedString(s: String): String = {
    s.replace("\\", """\\""")
      .replace("/", """\/""")
      .replace("\b", """\b""")
      .replace("\f", """\f""")
      .replace("\n", """\n""")
      .replace("\r", """\r""")
      .replace("\t", """\t""")
      .replace("\"", "\\\"")
  }

}
