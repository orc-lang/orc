{-
PLEASE READ THE FOLLOWING BEFORE RUNNING

You will need a Google Calendar account, and you
will need popup blockers disabled.

This program will:
1. Ask you for temporary permission to update your
   Google Calendar
2. Search MySpace for bands performing in Austin TX
3. Find perormances by those bands
4. Add those performances to your Google Calendar
   under the calendar "Orc Music Calendar"

TO REMOVE the added calendar entries:
- At the bottom of the "My Calendars" section to the
  left of your calendar, click on "settings".
- Find "Orc Music Calendar" under "Calendar Settings"
  and click the trash can to its right.

We make use of a technology called "OAuth" to
access your Google Calendar without your username
and password.  When you run the program, you will
be prompted to log in to Google directly and then
grant this program permission to update your
calendar.  You may revoke this permission at any
time through your "My Account" page on Google.

We offer no warranty or guarantees that running
this program will not completely trash your Google
Calendar.  This demo is intended only for those
with a sense of adventure.
-}

include "net.inc"

-- imports
import site MySpace = "orc.lib.music_calendar.MySpace"
import site GoogleCalendarFactory = "orc.lib.music_calendar.GoogleCalendar"
import site OAuth = "net.oauth.OAuth"

-- declarations
val oauth = OAuthProvider("orc/orchard/orchard.properties")
val Google = GoogleSearchFactory("orc/orchard/orchard.properties")
val GoogleCalendar = GoogleCalendarFactory(oauth, "google")
def phrases() =
    "site:www.myspace.com 'Austin, TX' 'Band Members'"
  | "site:www.myspace.com 'Austin, Texas' 'Band Members'"

-- execution
Println("Authenticating...") >>
GoogleCalendar.authenticate(oauth.authenticate("google", "scope", "http://www.google.com/calendar/feeds/")) >>
phrases() >phrase>
Google(phrase) >pages>
each(pages) >page>
each(page()) >result>
Println("Scraping " + result.url) >>
MySpace.scrapeMusicShows(result.url) >musicShows>
each(musicShows) >musicShow>
Ift(musicShow.getCity().toLowerCase().contains("austin")) >>
GoogleCalendar.addMusicShow(musicShow) >>
stop
; "DONE"
