//
// DotUtils.scala -- Scala object DotUtils
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

object DotUtils {
  def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"
  def quote(s: String) = s.replace('"', '\'')

  type DotAttributes = Map[String, String]
  
  trait WithDotAttributes {
    def dotAttributes: DotAttributes

    def dotAttributeString = {
      s"[${dotAttributes.map(p => s"${p._1}=${'"'}${quote(p._2)}${'"'}").mkString(",")}]"
    }
  }
}
