include "net.inc"

val Twitter = TwitterFactoryPropertyFile("twitter.properties")

val requestToken = Twitter.getOAuthRequestToken()
val pin = Browse(requestToken.getAuthorizationURL()) >> stop | Prompt("PIN or nothing if there is none")
val token = (if pin = "" then
  Twitter.getOAuthAccessToken()
else
  Twitter.getOAuthAccessToken(requestToken, pin)
) #

Println("val (TwitterAccessKey, TwitterAccessSecret) = " + (token.getToken(), token.getTokenSecret()))
