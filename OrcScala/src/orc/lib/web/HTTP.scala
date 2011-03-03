//
// HTTP.scala -- Scala class/trait/object HTTP
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Mar 3, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import orc.values.sites.{TotalSite, Site0, Site1}
import orc.values.OrcRecord
import orc.Handle
import orc.error.runtime.SiteException

import java.net.{URLConnection, URL, URLEncoder}
import java.io.{OutputStreamWriter, InputStreamReader}
import java.lang.StringBuilder
import java.net.URL

import scala.io.Source

/**
 * 
 *
 * @author dkitchin, Blake
 */
object HTTP extends TotalSite {
  
  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(s: String) => {
        createHTTPInstance(new URL(s))
      }
      case List(s: String, OrcRecord(q)) => {
        val query = if (q.isEmpty) { "" } else { "?" + convertToQueryPairs(q) }
        createHTTPInstance(new URL(s + query))
      }
      case List(url: URL) => {
        createHTTPInstance(url)
      }
      case _ => throw new SiteException("Malformed arguments to HTTP. Arguments should be (String), (String, {..}), or (URL).")
    }
  }
  
  def convertToQueryPairs(entries: Map[String, AnyRef]): String = {
    val nameValuePairs = 
      for ((name, v) <- entries; value = v.toString()) yield {
        URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
      }
    nameValuePairs reduceLeft { _ + "&" + _ }
  }
  
  def createHTTPInstance(url: URL) = {
    new OrcRecord(List(
      ("get", HTTPGet(url)),
      ("post", HTTPPost(url))
    ))
  }
  
  case class HTTPGet(url: URL) extends Site0 {
    def call(h: Handle) {
      val getAction = 
        new Runnable {
          def run() {
            h.publish(Source.fromURL(url).mkString)
          }
        }
      (new Thread(getAction)).start()
    }
  }
  
  case class HTTPPost(url: URL) extends Site1 {
    def call(a: AnyRef, h: Handle) {
      val post = a.toString
      val postAction =
        new Runnable {
          def run() {
            val conn = url.openConnection
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(5000)
            conn.setDoOutput(true)
            conn.connect()
            
            val out = new OutputStreamWriter(conn.getOutputStream)
            out.write(post)
            out.close
      
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
            
            h.publish(result.toString)
          }
        }
      (new Thread(postAction)).start()
    }
  }

}