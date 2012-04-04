//
// KeyedHTTP.scala -- Java class KeyedHTTP
// Project OrcSites
//
// $Id$
//
// Created by dkitchin on Apr 3, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.net

import java.io.FileNotFoundException
import java.util.Properties
import java.io.InputStream

import orc.values.sites.Site
import orc.error.runtime.ArgumentTypeMismatchException
import orc.values.OrcRecord
import orc.values.sites.TotalSite2
import orc.lib.web.HTTP

/**
 * 
 * An Orchard-specific factory for specializations of the HTTP site. 
 * It is intended to provide REST-style access to web services using
 * a developer key or other shared secret as a query parameter, while
 * protecting that shared secret from being exposed to the end user.
 * 
 * The site is invoked as follows:
 * 
 * KeyedHTTP("path/to/orchard.properties", "foo")
 * 
 * where "foo" is a fragment of a property name used to lookup
 * the properties containing the domain of interest, the parameter name, 
 * and the key itself. It must contain only alphanumeric characters.
 * 
 * The properties file should contain three properties of the form:
 * 
 * webservice.foo.domain: api.fooservice.com
 * webservice.foo.parameter.name: apikey
 * webservice.foo.parameter.value: jkn459njbfgl2j5k
 * 
 * If any of the three properties is absent, the call fails.
 *
 * If all are present, a specialized version of the HTTP site is returned,
 * with some modified behaviors:
 * 
 * 1) Any HTTP instance created by this site acquires an extra query parameter
 *    corresponding to the .name property, with a value given by the .value
 *    property.   
 * 2) The url field of HTTP instances created by this site is blank,
 *    to avoid exposing the .value embedded in the query string.
 * 3) HTTP instances created by this site must use the domain name given
 *    by the .domain property. If the domain name differs, the instance
 *    creation fails.
 * 4) This site must take exactly two parameters when constructing an
 *    HTTP instance.
 *
 * Given these restrictions, the secret embedded in .value is thus inaccessible
 * to end users.
 *
 *
 * @author dkitchin
 */
object KeyedHTTP extends TotalSite2 {

  def eval(x: AnyRef, y: AnyRef): AnyRef = {
    (x,y) match {
      case (file: String, service: String) => {
        val p = new Properties()
        val stream: InputStream = classOf[Site].getClassLoader().getResourceAsStream(file);
		if (stream == null) {
			throw new FileNotFoundException(file)
		}
		p.load(stream)
		
		if (service.matches("""\A\w+\z""")) {
			val domain = p.getProperty("webservice." + service + ".domain")
			val pname = p.getProperty("webservice." + service + ".parameter.name")
			val pvalue = p.getProperty("webservice." + service + ".parameter.value")
			new KeyedHTTPInstance(domain, pname, pvalue)
		}
		else {
			throw new IllegalArgumentException("Service name " + service + " is invalid; only alphanumeric characters are permitted")
		}
      }
      case (_ : String, a) => throw new ArgumentTypeMismatchException(1, "String", if (a != null) a.getClass().toString() else "null")
      case (a, _) => throw new ArgumentTypeMismatchException(0, "String", if (a != null) a.getClass().toString() else "null")
    }
  }
  
}
  
class KeyedHTTPInstance(domain: String, param: String, secret: String) extends TotalSite2 {
  
  def eval(x: AnyRef, y: AnyRef): AnyRef = {
    (x,y) match {
      case (s: String, query: OrcRecord) => {
        val queryWithSecret = query + OrcRecord(Map(param -> secret))
        val httpWithSecret: OrcRecord = HTTP.evaluate(List(s, queryWithSecret)).asInstanceOf[OrcRecord]
        httpWithSecret + OrcRecord(Map("url" -> ""))
      }
      case (_ : String, a) => throw new ArgumentTypeMismatchException(1, "Record", if (a != null) a.getClass().toString() else "null")
      case (a, _) => throw new ArgumentTypeMismatchException(0, "String", if (a != null) a.getClass().toString() else "null")
    }
  }
  
}