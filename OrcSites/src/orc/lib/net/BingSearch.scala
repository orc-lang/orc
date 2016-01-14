//
// BingSearch.scala -- Scala class BingSearch
// Project OrcSites
//
// Created by amp on Dec 27, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.net

import orc.values.sites._
import orc.types._
import orc.values.sites.compatibility.Types
import java.net.URL
import java.net.URLEncoder
import sun.misc.BASE64Encoder
import java.net.HttpURLConnection
import scala.io.Source
import org.codehaus.jettison.json.JSONObject
import java.util.Properties
import java.io.FileNotFoundException

class BingSearchFactoryPropertyFile extends PartialSite with SpecificArity with TypedSite {
  val arity = 2

  def orcType() = {
    import Types._
    FunctionType(Nil, List(string, string),
      BingSearch.orcType)
  }

  def loadProperties(file: String): (String, String) = {
    val p = new Properties();
    val stream = classOf[BingSearch].getResourceAsStream("/" + file);
    if (stream == null) {
      throw new FileNotFoundException(file);
    }
    p.load(stream);
    (p.getProperty("orc.lib.net.bing.username"),
      p.getProperty("orc.lib.net.bing.key"))
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(file: String, source: String) = args
    val (user, key) = loadProperties(file)
    Some(new BingSearch(user, key, source))
  }
}

class BingSearchFactoryUsernameKey extends PartialSite with SpecificArity with TypedSite {
  val arity = 3

  def orcType() = {
    import Types._
    FunctionType(Nil, List(string, string, string),
      BingSearch.orcType)
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(user: String, key: String, source: String) = args
    Some(new BingSearch(user, key, source))
  }
}

object BingSearch {
  import Types._
  val orcType = FunctionType(Nil, List(string), list(bot))
  // TODO: This is not quite right and will probably not work when type checked.
}

/**
  * @author amp
  */
class BingSearch(user: String, key: String, source: String) extends PartialSite with SpecificArity with TypedSite {
  val arity = 1

  def orcType() = BingSearch.orcType

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(query: String) = args
    val url = new URL("https://api.datamarket.azure.com/Bing/Search/v1/%s?$format=json&Query=%%27%s%%27".format(
      URLEncoder.encode(source, "UTF-8"), URLEncoder.encode(query, "UTF-8")))

    val encoder = new BASE64Encoder()
    val credentialBase64 = (encoder.encode((user + ":" + key).getBytes())).replaceAll("\\s", "")

    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    conn.setConnectTimeout(10000) // 10 seconds is reasonable
    conn.setReadTimeout(5000) // 5 seconds is reasonable
    conn.setRequestProperty("accept", "*/*")
    conn.addRequestProperty("Authorization", "Basic " + credentialBase64)
    conn.connect()

    val src = Source.fromInputStream(conn.getInputStream())
    val s = src.mkString("", "", "")
    val j = new JSONObject(s)

    Some(JSONSite.wrapJSON(j))
  }
}
