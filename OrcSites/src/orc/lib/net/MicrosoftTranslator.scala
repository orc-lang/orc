//
// MicrosoftTranslator.scala -- Scala class MicrosoftTranslator
// Project OrcSites
//
// $Id$
//
// Created by amp on Oct 9, 2016.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
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

class MicrosoftTranslatorFactoryPropertyFile extends PartialSite with SpecificArity with TypedSite {
  val arity = 1

  def orcType() = {
    import Types._
    FunctionType(Nil, List(string),
      BingSearch.orcType)
  }

  def loadProperties(file: String): (String, String) = {
    val p = new Properties();
    val stream = classOf[MicrosoftTranslator].getResourceAsStream("/" + file);
    if (stream == null) {
      throw new FileNotFoundException(file);
    }
    p.load(stream);
    (p.getProperty("orc.lib.net.bing.username"),
      p.getProperty("orc.lib.net.bing.key"))
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(file: String) = args
    val (user, key) = loadProperties(file)
    Some(new MicrosoftTranslator(user, key))
  }
}

class MicrosoftTranslatorFactoryUsernameKey extends PartialSite with SpecificArity with TypedSite {
  val arity = 2

  def orcType() = {
    import Types._
    FunctionType(Nil, List(string, string),
      BingSearch.orcType)
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(user: String, key: String) = args
    Some(new MicrosoftTranslator(user, key))
  }
}

object MicrosoftTranslator {
  import Types._
  val orcType = FunctionType(Nil, List(string, string), string)
}

/**
  *
  * @author amp
  */
class MicrosoftTranslator(user: String, key: String) extends PartialSite with SpecificArity with TypedSite {
  val arity = 1

  def orcType() = BingSearch.orcType

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(text: String, target: String) = args
    val url = new URL("https://api.datamarket.azure.com/Bing/MicrosoftTranslator/v1/Translate?Text=%%27%s%%27&To=%27%s%27".format(
      URLEncoder.encode(text, "UTF-8"), URLEncoder.encode(target, "UTF-8")))

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

    Some(s)
  }
}
