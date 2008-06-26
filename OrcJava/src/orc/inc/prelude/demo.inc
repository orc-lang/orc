
{- Support for instant messaging using Jabber -}

val (orchardUID, orchardPass) = ("orchardserver", "ckyogack")

site XMPPConnection = orc.lib.xmpp.XMPPConnection

val chatConnection = 
  XMPPConnection("talk.google.com", 5222, "gmail.com") >c>
  c.connect() >>
  c.login(orchardUID, orchardPass) >> 
  c
 

{- def chatParse(s) = r <r< (parseBool(s); parseInt(s); s) -}
  

def orchardChatQuery(recipient, query) =
  val chat = chatConnection.chat(recipient)
  chat.send(query) >> chat.receive() {- >s> chatParse(s) -}
  


def Adrian(q) = orchardChatQuery("adrianquark@gmail.com", q)
def David(q) = orchardChatQuery("dkitchin@gmail.com", q)

def Dynamic(s) = lambda (q) = orchardChatQuery(s,q)

def Email(address, payload) = ("email", address, payload)

val william = "wcook@cs.utexas.edu"

-- Generate a random number in the range [i,j] inclusive
def Random(i,j) = random(j - i) + i + 1

-- Determine whether a number is prime
def isPrime(n) = 
  def primeat(i) = 
      if(b) >> (n % i /= 0) && primeat(i+1)
    | if(~b) >> true
        <b< i * i <= n
  primeat(2)


-- for "Music Calendar" demo
site MySpace = orc.lib.music_calendar.MySpace
site GoogleCalendar = orc.lib.music_calendar.GoogleCalendar
site Google = orc.lib.music_calendar.GoogleSearch
val GoogleDevKey = "dceJmvRQFHKEMK0xSxG9FqYOwqQVRDqQ"
val GoogleCalendarUsername = "adrian-orchard-server@sixfingeredman.net"
val GoogleCalendarPassword = "ckyogack"
def GoogleSearch(keywords) = Google.doGoogleSearch(
	GoogleDevKey, keywords, 0, 10, true, "", true, "", "", "")
def AddMusicShows(musicShows) = GoogleCalendar.addMusicShows(
	GoogleCalendarUsername, GoogleCalendarPassword,	musicShows) >> null

{-
def David(s) = Rtimer(random(5) * 500) >> "breakfast tacos"
def Adrian(s) = Rtimer(random(5) * 500) >> "polytetrafluoroethylene"
-}
