include "net.inc"

-- imports
site MySpace = orc.lib.music_calendar.MySpace
site GoogleCalendarFactory = orc.lib.music_calendar.GoogleCalendar

-- declarations
val oauth = OAuthProvider("orc/orchard/oauth.properties")
val GoogleCalendar = GoogleCalendarFactory(oauth, "google")
val phrases =
    "site:www.myspace.com 'Austin, TX' 'Band Members'"
  | "site:www.myspace.com 'Austin, Texas' 'Band Members'"

-- execution
(
    GoogleCalendar.authenticate() 
  | println("Authenticating...") >> null
) >>
phrases >phrase>
Google(phrase) >pages>
each(pages) >page>
each(page()) >result>
println("Scraping " + result.url) >>
MySpace.scrapeMusicShows(result.url) >musicShows>
each(musicShows) >musicShow>
GoogleCalendar.addMusicShow(musicShow) >>
null
