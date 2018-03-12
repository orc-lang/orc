//
// StringExtension.scala -- Scala object StringExtension
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

object StringExtension {
  implicit class StringOperations(val s: String) extends AnyVal {
    def truncateTo(i: Int, marker: String = "[...]") = {
      assume(i > marker.length)
      if (s.length > i) {
        s.substring(0, i - marker.length) + marker
      } else {
        s
      }
    }
    
    def stripNewLines = {
      s.replace("\n\r", " ").replace("\r", " ").replace("\n", " ")
    }
  }
}
