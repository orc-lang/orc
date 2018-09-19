//
// YandexTranslate.scala -- Scala class YandexTranslate
// Project OrcSites
//
// Created by amp on Oct 9, 2016.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.net

import java.io.{ FileNotFoundException, OutputStreamWriter }
import java.net.{ HttpURLConnection, URL, URLEncoder }
import java.util.Properties

import scala.io.Source

import orc.util.ArrayExtensions.{ Array1, Array2 }
import orc.values.sites.{ SpecificArity }
import orc.values.sites.compatibility.{ ScalaPartialSite }

import org.codehaus.jettison.json.JSONObject

class YandexTranslateFactory extends ScalaPartialSite with SpecificArity {
  val arity = 1

  def loadProperties(file: String): String = {
    val p = new Properties();
    val stream = classOf[MicrosoftTranslator].getResourceAsStream("/" + file);
    if (stream == null) {
      throw new FileNotFoundException(file);
    }
    p.load(stream)
    p.getProperty("orc.lib.net.yandex.translate.key")
  }

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    val Array1(file: String) = args
    val key = try {
      loadProperties(file)
    } catch {
      case _: FileNotFoundException =>
        file
    }
    Some(new YandexTranslate(key))
  }
}

/** @author amp
  */
class YandexTranslate(key: String) extends ScalaPartialSite with SpecificArity {
  val arity = 2

  val url = new URL(
    s"https://translate.yandex.net/api/v1.5/tr.json/translate?key=${URLEncoder.encode(key, "UTF-8")}")

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    val Array2(text: String, target: String) = args

    val params = s"text=${URLEncoder.encode(text, "UTF-8")}&lang=${URLEncoder.encode(target, "UTF-8")}&options=1"

    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    conn.setConnectTimeout(10000) // 10 seconds is reasonable
    conn.setReadTimeout(5000) // 5 seconds is reasonable
    conn.setRequestProperty("accept", "*/*")
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    conn.setRequestMethod("POST")
    conn.setDoOutput(true)

    conn.connect()
    val outputWr = new OutputStreamWriter(conn.getOutputStream())
    outputWr.write(params)
    outputWr.close()

    val resp = conn.getResponseCode()

    val src = Source.fromInputStream(conn.getInputStream())
    val s = src.mkString("", "", "")

    if (resp == 200) {
      Some(JSONSite.wrapJSON(new JSONObject(s)))
    } else {
      throw new RuntimeException(s"Error translating with $url: $resp $s");
    }
  }
}
