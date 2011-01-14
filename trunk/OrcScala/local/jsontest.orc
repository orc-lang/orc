{-

-}




{- Twitter Translate
-    Blake Davis
-}

site get = orc.lib.web.HTTPGet
site rest = orc.lib.web.REST
site JSON = orc.lib.web.ReadJSON


val gTranslate = rest("https://www.googleapis.com/language/translate/v2")

def translate(s, src, tgt) = 
  val query = {. key = "AIzaSyBBbALsLpeLE8YGSfYpspUF8kd_YhdC3Q4",
         q = s, source = src, target = tgt .}
  gTranslate(query)

def twitter() = get("http://api.twitter.com/1/statuses/public_timeline.json")

val userTweet = rest("http://api.twitter.com/1/users/show.json")
def getUser(s) = 
  val q = {. screen_name = s .}
  userTweet(q)

val user = getUser("w7cook")

 --println(JSON(twitter())) >> stop
 --| println(twitter()) >> stop
--println(user) >> stop
--| println(JSON(user)) >> stop

JSON("{ \"prop\":\"\\u003C\" }")