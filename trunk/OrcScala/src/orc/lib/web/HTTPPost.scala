//
// HTTPPost.scala -- Scala class/trait/object HTTPPost
// Project OrcScala
//
// $Id$
//
// Created by Blake on Dec 7, 2010.
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
import orc.values.sites.{TotalSite, Site}
import orc.values.OrcRecord
import orc.lib.xml.{XmlElementSite, XmlTextSite}
import orc.error.runtime.{ArgumentTypeMismatchException, ArityMismatchException}
import java.net.{URLConnection, URL, URLEncoder}
import java.io.{OutputStreamWriter, InputStreamReader}
import scala.xml._
import scala.io.Source
import java.lang.StringBuilder
/**
 * 
 *
 * @author Blake
 */
class HTTPPost extends TotalSite with Site {
  
   def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(s: String) => ConnectionSite(new URL(s))
      case List(z) => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }
}

case class ConnectionSite(url: URL) extends Site {
  
  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List(post: String) => {
        
        val conn = url.openConnection
        conn.setConnectTimeout(10000)
        conn.setReadTimeout(5000)
        conn.setDoOutput(true)
        conn.connect()
        
        val out = new OutputStreamWriter(conn.getOutputStream)
        out.write(post)
        out.close
  
        println(post)
        val in = new InputStreamReader(conn.getInputStream, "UTF-8")
        val result = new StringBuilder
        var buf = new Array[Char](1024)
        var blen = in.read(buf)
        while(blen >= 0) {
          blen = in.read(buf)
          result.append(buf)
          println(blen)
        } 
        in.close
        //val results = Source.fromInputStream(conn.getInputStream).foldLeft("")({ _ + _ })
        //println(results.toString)
        //val results = XML.load(conn.getInputStream)
        //println(results)
        //val result = Source.fromURL(url, "UTF-8").mkString
        callingToken.publish(result)
      }
      case List(z) => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }   
  } 
  
  //def 
}