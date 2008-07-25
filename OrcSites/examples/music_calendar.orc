{-
In order to run this demo, you must have:
1. a google OAuth developer account:
   http://code.google.com/apis/accounts/docs/OAuth.html
2. in the classpath, /oauth.properties (see oauth.sample.properties)
3. in the classpath, /oauth.jks (Java keystore containing your
   google OAuth private key)
-}

include "net.inc"

-- imports
site MySpace = orc.lib.music_calendar.MySpace
site GoogleCalendarFactory = orc.lib.music_calendar.GoogleCalendar

-- declarations
val GoogleCalendar = GoogleCalendarFactory("oauth.properties", "google")
val phrase1 = "site:www.myspace.com 'Austin, TX' 'Band Members'"
val phrase2 = "site:www.myspace.com 'Austin, Texas' 'Band Members'"

-- execution
GoogleCalendar.authenticate() >>
(Google(phrase1) | Google(phrase2)) >pages>
each(pages) >page>
each(page()) >result>
MySpace.scrapeMusicShows(result.url) >musicShows>
each(musicShows) >musicShow>
GoogleCalendar.addMusicShow(musicShow) >>
null