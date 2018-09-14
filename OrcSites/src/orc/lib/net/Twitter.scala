//
// Twitter.scala -- Twitter API for Orc
// Project OrcSites
//
// Created by amp on Oct, 2016.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net

import java.io.FileInputStream
import java.util.Properties

import orc.types.{ FunctionType, JavaObjectType }
import orc.util.ArrayExtensions.{ Array0, Array1, Array2 }
import orc.values.OrcRecord
import orc.values.sites.{ TypedSite, SpecificArity }
import orc.values.sites.compatibility.{ ScalaPartialSite, TotalSite }

import TwitterUtil.OAuthAdd
import twitter4j.{ StallWarning, Status, StatusDeletionNotice, StatusListener, Twitter, TwitterFactory, TwitterStream, TwitterStreamFactory }
import twitter4j.auth.{ OAuth2Support, OAuthSupport }

object TwitterUtil {
  private def loadProperties(file: String): (String, String) = {
    val p = new Properties();
    val stream = {
      val s = classOf[TwitterFactoryPropertyFile].getResourceAsStream("/" + file);
      if (s == null) {
        //throw new FileNotFoundException(file);
        new FileInputStream(file)
      } else {
        s
      }
    }
    p.load(stream);
    (p.getProperty("orc.lib.net.twitter.key"),
      p.getProperty("orc.lib.net.twitter.secret"))
  }

  implicit class OAuthAdd(val twitter: OAuthSupport) extends AnyVal {
    def loadAuth(file: String): Unit = {
      val (key, secret) = loadProperties(file)
      loadAuth(key, secret)
    }

    def loadAuth(key: String, secret: String): Unit = {
      twitter.setOAuthConsumer(key, secret)
    }
  }

  implicit class OAuth2Add(val twitter: OAuth2Support) extends AnyVal {
    def authConsumer(): Unit = {
      val tok = twitter.getOAuth2Token()
      twitter.setOAuth2Token(tok)
    }
  }
}

class TwitterFactoryPropertyFile extends ScalaPartialSite with SpecificArity with TypedSite {
  val arity = 1

  def orcType() = {
    import orc.values.sites.compatibility.Types._
    FunctionType(Nil, List(string), JavaObjectType(classOf[Twitter]))
  }

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    val Array(file: String) = args
    val i = new TwitterFactory().getInstance()
    i.loadAuth(file)
    Some(i)
  }
}

class TwitterFactoryKeySecret extends ScalaPartialSite with SpecificArity with TypedSite {
  val arity = 2

  def orcType() = {
    import orc.values.sites.compatibility.Types._
    FunctionType(Nil, List(string, string), JavaObjectType(classOf[Twitter]))
  }

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    val Array2(key: String, secret: String) = args
    val i = new TwitterFactory().getInstance()
    i.loadAuth(key, secret)
    Some(i)
  }
}

class TwitterStreamFactoryPropertyFile extends ScalaPartialSite with SpecificArity with TypedSite {
  val arity = 1

  def orcType() = {
    import orc.values.sites.compatibility.Types._
    FunctionType(Nil, List(string), JavaObjectType(classOf[TwitterStream]))
  }

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    val Array1(file: String) = args
    val i = new TwitterStreamFactory().getInstance()
    i.loadAuth(file)
    Some(i)
  }
}

class TwitterStreamFactoryKeySecret extends ScalaPartialSite with SpecificArity with TypedSite {
  val arity = 2

  def orcType() = {
    import orc.values.sites.compatibility.Types._
    FunctionType(Nil, List(string, string), JavaObjectType(classOf[TwitterStream]))
  }

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    val Array(key: String, secret: String) = args
    val i = new TwitterStreamFactory().getInstance()
    i.loadAuth(key, secret)
    Some(i)
  }
}

// TODO: Implement channels so threads are not blocked. Should only be one thread per repeat(getter), so not too bad.
private class EventGetterSite extends ScalaPartialSite with SpecificArity {
  val arity = 0

  val channel = new scala.concurrent.Channel[AnyRef]()

  def put(v: AnyRef) = {
    channel.write(v)
  }

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    val Array0() = args
    Some(channel.read)
  }
}

// TODO: Put this on the TwitterStream object directly.
class WrapTwitterStream extends TotalSite with SpecificArity with TypedSite {
  val arity = 1

  def orcType() = {
    import orc.values.sites.compatibility.Types._
    FunctionType(Nil, List(JavaObjectType(classOf[TwitterStream])), bot)
  }

  def evaluate(args: Array[AnyRef]): AnyRef = {
    val Array(twitter: TwitterStream) = args
    object GetStreamListener extends TotalSite with SpecificArity {
      val arity = 0

      def evaluate(args: Array[AnyRef]): AnyRef = {
        val status = new EventGetterSite()
        twitter.addListener(new StatusListener {
          def onDeletionNotice(e: StatusDeletionNotice): Unit = ()
          def onScrubGeo(a: Long, b: Long): Unit = ()
          def onStallWarning(e: StallWarning): Unit = ()
          def onStatus(s: Status): Unit = status.put(s)
          def onTrackLimitationNotice(e: Int): Unit = ()
          def onException(e: Exception): Unit = ()
        })
        new OrcRecord(
          "status" -> status)
      }
    }
    new OrcRecord(
      "getStatusListener" -> GetStreamListener)
  }
}
