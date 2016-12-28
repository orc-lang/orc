package orc.lib.net

import twitter4j._
import twitter4j.auth.{OAuthSupport, OAuth2Support}

import java.util.Properties
import java.util.Collections
import java.io.FileNotFoundException

import orc.values.sites._
import orc.types._
import orc.values.sites.compatibility.{Types, Args, DotSite}
import orc.values.OrcRecord

import TwitterUtil._
import java.io.FileInputStream

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

class TwitterFactoryPropertyFile extends PartialSite with SpecificArity with TypedSite {    
  val arity = 1

  def orcType() = {
    import Types._
    FunctionType(Nil, List(string), JavaObjectType(classOf[Twitter]))
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(file: String) = args
    val i = new TwitterFactory().getInstance()
    i.loadAuth(file)
    Some(i)
  }
}

class TwitterFactoryKeySecret extends PartialSite with SpecificArity with TypedSite {
  val arity = 2

  def orcType() = {
    import Types._
    FunctionType(Nil, List(string, string), JavaObjectType(classOf[Twitter]))
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(key: String, secret: String) = args
    val i = new TwitterFactory().getInstance()
    i.loadAuth(key, secret)
    Some(i)
  }
}


class TwitterStreamFactoryPropertyFile extends PartialSite with SpecificArity with TypedSite {
  val arity = 1

  def orcType() = {
    import Types._
    FunctionType(Nil, List(string), JavaObjectType(classOf[TwitterStream]))
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(file: String) = args
    val i = new TwitterStreamFactory().getInstance()
    i.loadAuth(file)
    Some(i)
  }
}

class TwitterStreamFactoryKeySecret extends PartialSite with SpecificArity with TypedSite {
  val arity = 2

  def orcType() = {
    import Types._
    FunctionType(Nil, List(string, string), JavaObjectType(classOf[TwitterStream]))
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List(key: String, secret: String) = args
    val i = new TwitterStreamFactory().getInstance()
    i.loadAuth(key, secret)
    Some(i)
  }
}

// TODO: Implement channels so threads are not blocked. Should only be one thread per repeat(getter), so not too bad.
private class EventGetterSite extends PartialSite with SpecificArity {
  val arity = 0
  
  val channel = new scala.concurrent.Channel[AnyRef]()
  
  def put(v: AnyRef) = {
    channel.write(v)
  }
  
  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    val List() = args
    Some(channel.read)
  }
}

// TODO: Put this on the TwitterStream object directly.
class WrapTwitterStream extends TotalSite with SpecificArity with TypedSite {
  val arity = 1

  def orcType() = {
    import Types._
    FunctionType(Nil, List(JavaObjectType(classOf[TwitterStream])), bot)
  }

  def evaluate(args: List[AnyRef]): AnyRef = {
    val List(twitter: TwitterStream) = args
    object GetStreamListener extends TotalSite with SpecificArity {
      val arity = 0
    
      def evaluate(args: List[AnyRef]): AnyRef = {
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
