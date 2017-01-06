//
// MicrosoftTranslator.scala -- Scala class MicrosoftTranslator
// Project OrcSites
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
import java.net.HttpURLConnection
import scala.io.Source
import org.codehaus.jettison.json.JSONObject
import java.util.Properties
import java.io.FileNotFoundException
import java.io.OutputStreamWriter
import org.codehaus.jettison.json.JSONArray

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

/** @author amp
  */
class MicrosoftTranslator(user: String, key: String) extends PartialSite with SpecificArity with TypedSite {
  val arity = 2

  def orcType() = MicrosoftTranslator.orcType

  val authUrl = new URL("https://datamarket.accesscontrol.windows.net/v2/OAuth2-13")

  var tokenExpiration = Long.MaxValue
  var token = ""

  def getAccessToken() = {
    if (token == "" || System.currentTimeMillis() >= tokenExpiration ) {
      val conn = authUrl.openConnection().asInstanceOf[HttpURLConnection]
      conn.setConnectTimeout(10000) // 10 seconds is reasonable
      conn.setReadTimeout(5000) // 5 seconds is reasonable
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      conn.setRequestMethod("POST")
      conn.setDoOutput(true)

      conn.connect()
      val outputWr = new OutputStreamWriter(conn.getOutputStream())
      outputWr.write(s"grant_type=client_credentials&client_id=${URLEncoder.encode(user, "UTF-8")}&client_secret=${URLEncoder.encode(key, "UTF-8")}&scope=http://api.microsofttranslator.com")
      outputWr.close()

      val src = Source.fromInputStream(conn.getInputStream())
      val s = src.mkString("", "", "")

      val o = new JSONObject(s)
      if (o.has("error") && o.getBoolean("error")) {
        throw new RuntimeException(s"Error Authenticating with $authUrl: ${o.optString("error_description")}");
      }
      
      tokenExpiration = (o.getLong("expires_in") - 10) * 1000 + System.currentTimeMillis()
      token = o.getString("access_token")
    }

    token
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(text: String, target: String) = args
    val params = s"text=${URLEncoder.encode(text, "UTF-8")}&to=${URLEncoder.encode(target, "UTF-8")}"
    val url = new URL(s"http://api.microsofttranslator.com/V2/AJAX.svc/Translate?$params")

    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    conn.setConnectTimeout(10000) // 10 seconds is reasonable
    conn.setReadTimeout(5000) // 5 seconds is reasonable
    conn.setRequestProperty("accept", "*/*")
    conn.addRequestProperty("Authorization", "Bearer " + getAccessToken())
    conn.connect()
    
    val resp = conn.getResponseCode()

    val src = Source.fromInputStream(conn.getInputStream())
    val s = src.mkString("", "", "")
    
    if (resp == 200) {
      // Hack to parse bare JSON string. The stripPrefix is to remove the Unicode BOM.
      val o = new JSONArray(s"[${s.stripPrefix("\uFEFF")}]")
      Some(o.getString(0))
    } else {
      throw new RuntimeException(s"Error translating with $url: $resp $s");
    }
  }
}
