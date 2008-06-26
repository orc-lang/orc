
{- Cliff's Notes for the Turing Test -} 

def D("What is your favorite food?") = "breakfast tacos"
def D("Pick a movie you like:") = "Pan's Labyrinth"
def D("Second element: ") = "2"
def D("What's your favorite number?") = "42"
def D("Low priority response: ") = "low"
def D("Choose a color.") = "tie dye"
def D("How is the weather?") = "partly cloudy"
def D("What time is it?") = "nine in the afternoon"
def D("True or false?") = "true"
def D("Pick a number:" ) = "42"
def D(s) = "message not understood"

def A("What is your favorite food?") = "polytetrafluoroethylene"
def A("What is your favorite color?") = "green"
def A("Pick a movie you like:") = "The Graduate"
def A("First element: ") = "1"
def A("What's your favorite number?") = "7"
def A("High priority response: ") = "high"
def A("Choose a color.") = "green"
def A("How is the weather?") = "warm"
def A("What time is it?") = "midnight"
def A("True or false?") = "false"
def A("Pick a number:" ) = "7"
def A(s) = "I don't understand the question"
  
def David(s) = Rtimer(500*Random(1,12)) >> D(s)
def Adrian(s) = Rtimer(500*Random(2,7)) >> A(s)


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
