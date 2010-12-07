//
// HTTPGet.scala -- Scala class HTTPGet
// Project OrcScala
//
// $Id$
//
// Created by Blake on Nov 8, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import orc.values.sites.{TotalSite, UntypedSite}
import orc.error.runtime.{ArgumentTypeMismatchException, ArityMismatchException}
import java.net.{URLConnection, URL, URLEncoder}
import scala.io.Source

/**
 *  HTTP get request for a RESTful web service
 *
 * @author Blake
 */
class HTTPGet extends TotalSite with UntypedSite {

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(s: String) => {
        val url = new URL(s)
        Source.fromURL(url).mkString
      }
      case List(z) => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

}
