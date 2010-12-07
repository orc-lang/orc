//
// REST.scala -- Scala class REST
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Sep 27, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import scala.collection.immutable.List
import orc.TokenAPI
import orc.values.sites.{TotalSite, UntypedSite}
import orc.values.OrcRecord
import orc.error.runtime.{ArgumentTypeMismatchException, ArityMismatchException}
import java.net.{URLConnection, URL, URLEncoder}
import scala.xml._
import scala.io.Source

/**
 * 
 * Factory site for Orc representations of RESTful web services.
 *
 * @author dkitchin
 */

class REST extends TotalSite with UntypedSite {

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(s: String) => RESTfulSite(new URL(s))
      case List(z) => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

}

case class RESTfulSite(baseUrl: URL) extends UntypedSite {
  
  def convertToQuery(entries: Map[String, AnyRef]): String = {
    val nameValuePairs = 
      for ((name, v) <- entries; value = v.toString()) yield {
        URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
      }
    nameValuePairs reduceLeft { _ + "&" + _ }
  }
  
  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List(OrcRecord(entries)) => {
        val url = new URL(baseUrl + "?" + convertToQuery(entries))
        //val connection = url.openConnection
        //val result = Source.fromInputStream(connection.getInputStream).foldLeft("")({ _ + _ })
        //val result = XML.load(connection.getInputStream)
        val result = Source.fromURL(url, "UTF-8").mkString
        callingToken.publish(result)
      }
      case List(z) => throw new ArgumentTypeMismatchException(0, "Record", z.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }   
  }
  
  override def name: String = baseUrl.toString()
}
