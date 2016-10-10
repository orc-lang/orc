{- twitter.orc -- Orc program that streams and searches Twitter
 -}

{-
In order to run this demo, you must have:
1. a Twitter account and an associated application
2. in the classpath, /twitter.properties (see twitter.sample.properties)
-}

include "net.inc"

val Twitter = TwitterFactoryPropertyFile("twitter.properties")
val TwitterStream = TwitterStreamFactoryPropertyFile("twitter.properties")

def doOAuth() =
  val requestToken = Twitter.getOAuthRequestToken()
  val pin = Browse(requestToken.getAuthorizationURL()) >> stop | Prompt("PIN or nothing if there is none")
  val token = (if pin = "" then
    Twitter.getOAuthAccessToken()
  else
    Twitter.getOAuthAccessToken(requestToken, pin)
  ) #
  (token.getToken(), token.getTokenSecret())

val query = TwitterQuery("programming filter:safe")
--val fquery = TwitterFilterQuery("programming filter:safe")

--Println(doOAuth()) >>
Twitter.setOAuthAccessToken(TwitterAccessToken("4005547514-Clpri11lEjtr1YeFjUU8X4iD9KKDVXjMI5OY4jY", "LmyzXnMF9cEfKm2shLdRh7DVBhctK5TWQGrScWm38YjaS")) >>
TwitterStream.setOAuthAccessToken(TwitterAccessToken("4005547514-Clpri11lEjtr1YeFjUU8X4iD9KKDVXjMI5OY4jY", "LmyzXnMF9cEfKm2shLdRh7DVBhctK5TWQGrScWm38YjaS")) >>
(

(
val result = Twitter.search().search(query)
Println("Test") |
repeat(IterableToStream(result.getTweets())) >s>
Println(s.getUser().getScreenName() + ": " + s.getText())
) >> stop

|

(
val streams = WrapTwitterStream(TwitterStream).getStatusListener()
Println(streams) |
streams >> TwitterStream.sample() |
repeat(streams.status) >s>
Println("* " + s.getUser().getScreenName() + ": " + s.getText())
) >> stop

)