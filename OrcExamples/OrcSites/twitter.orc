{- twitter.orc -- Orc program that streams and searches Twitter
 -}

{-
In order to run this demo, you must have:
1. a Twitter account and an associated application
2. in the classpath, /twitter.properties (see twitter.sample.properties)
3. Run twitter-auth.orc and put the line it produces in twitter-access-token.inc in the Orc path.
-}

include "net.inc"
include "twitter-access-token.inc"

val Twitter = TwitterFactoryPropertyFile("twitter.properties")
val TwitterStream = TwitterStreamFactoryPropertyFile("twitter.properties")

Twitter.setOAuthAccessToken(TwitterAccessToken(TwitterAccessKey, TwitterAccessSecret)) >>
TwitterStream.setOAuthAccessToken(TwitterAccessToken(TwitterAccessKey, TwitterAccessSecret)) >>
(

(
val result = Twitter.search().search(TwitterQuery("programming filter:safe"))
Println("Test") |
repeat(IterableToStream(result.getTweets())) >s>
Println(s.getUser().getScreenName() + ": " + s.getText())
) >> stop

|

(
val streams = WrapTwitterStream(TwitterStream).getStatusListener()
Println(streams) |
streams >> TwitterStream.filter("programming") |
repeat(streams.status) >s> Iff(s.isPossiblySensitive() ; false) >>
Println("* " + s.getUser().getScreenName() + ": " + s.getText())
) >> stop

)