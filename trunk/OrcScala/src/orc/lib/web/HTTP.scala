//
// HTTP.scala -- Scala object HTTP
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Mar 3, 2011.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import orc.values.sites.{ TotalSite, Site0, Site1, Site2 }
import orc.values.OrcRecord
import orc.Handle
import orc.error.runtime.SiteException

import java.net.{ URLConnection, URL, URLEncoder }
import java.io.{ OutputStreamWriter, InputStreamReader }
import java.lang.StringBuilder
import java.net.URL

import scala.io.Source

/** The HTTP site provides a simple mechanism to send GET and POST requests to a URL.
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
      ("post", HTTPPost(url)),
      ("url", url.toString())))
  }

  private def charEncodingFromContentType(contentType: java.lang.String): java.lang.String = {
    //TODO: Don't break if quoted parameter values are used (see RFC 2045 section 5.1)
    val contentTypeParams = contentType.split(";").map(_.trim()).toList.tail
    val charsetValues = List("ISO-8859-1") /* default per RFC 2616 section 3.7.1 */ ++ (
      for (param <- contentTypeParams if param.toLowerCase.startsWith("charset="))
        yield param.substring(8)
      )
    charsetValues.last
  }

  lazy val userAgent = "Orc/" + orc.Main.versionProperties.getProperty("orc.version") +
      " Java/" + java.lang.System.getProperty("java.version")

  case class HTTPGet(url: URL) extends Site0 {
    def call(h: Handle) {
      val getAction =
        new Runnable {
          def run() {
            val conn = url.openConnection
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(5000)
            conn.setRequestProperty("User-Agent", userAgent)
            conn.connect()

            val contentType = conn.getContentType()
            //TODO: Confirm our assumption that the content is a character stream
            val charEncoding = charEncodingFromContentType(contentType)
            val in = Source.fromInputStream(conn.getInputStream, charEncoding)
            val result = in.mkString
            in.close
            h.publish(result)
          }
        }
      (new Thread(getAction)).start()
    }
  }

  case class HTTPPost(url: URL) extends Site2 {
    def call(a: AnyRef, b: AnyRef, h: Handle) {
      val post = a.toString
      val postContentType = b.toString +
          (if (b.toString.toLowerCase.contains("charset="))
            ""
          else
            "; charset=UTF-8")
      val postAction =
        new Runnable {
          def run() {
            val conn = url.openConnection
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(5000)
            conn.setDoOutput(true)
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Content-Type", postContentType)
            conn.connect()

            val out = new OutputStreamWriter(conn.getOutputStream, charEncodingFromContentType(postContentType))
            out.write(post)
            out.close

            val contentType = conn.getContentType()
            //TODO: Confirm our assumption that the content is a character stream
            val charEncoding = charEncodingFromContentType(contentType)
            val in = Source.fromInputStream(conn.getInputStream, charEncoding)
            val result = in.mkString
            in.close
            h.publish(result)
          }
        }
      (new Thread(postAction)).start()
    }
  }

}
