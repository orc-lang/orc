//
// ReadText.scala -- Scala site ReadText
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util;

import java.io.{ IOException, InputStreamReader }

import orc.error.runtime.JavaException
import orc.values.sites.TotalSite1Simple

/**
 * Read an InputStreamReader into a String. Reads all remaining characters from
 * the stream, and closes it.
 *
 * @author quark
 */
object ReadText extends TotalSite1Simple[InputStreamReader] {
  def eval(in: InputStreamReader) = {
    try {
        val out = new StringBuilder()
        val buff = Array.ofDim[Char](1024)
        var done = false
        while (!done) {
            val blen = in.read(buff)
            if (blen < 0) {
              done = true
            } else {
              out.appendAll(buff, 0, blen)
            }
        }
        in.close()
        out.toString()
    } catch {
      case e: IOException =>
        throw new JavaException(e)
    }
  }
}
