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

-- imports
import site GoogleCalendarFactory = "orc.lib.music_calendar.GoogleCalendar"
import site OAuthProvider = "orc.lib.orchard.OAuthProviderSite"
import site KeyedHTTP = "orc.lib.net.KeyedHTTP"

-- declarations
val oauth = OAuthProvider("orc/orchard/orchard.properties")
val GoogleCalendar = GoogleCalendarFactory(oauth, "google")
val SongKickHTTP = KeyedHTTP("orc/orchard/orchard.properties", "songkick")

{- Use the SongKick Web API to look up shows -}
def findShows(city, numberOfShows) =
  
  {- Given a city name, find the corresponding metro area ID -}
  def lookupMetro(city) =
    val base = "http://api.songkick.com/api/3.0/search/locations.xml"
	   val query = {. query = city .}
	   val response = SongKickHTTP(base,query).get() 
    ReadXML(response) 
      >xml("resultsPage",xml("results", xml("location", m)))>
    m 
      >XMLElement("metroArea", {. id = n .}, _)>
    n
    
		{- Given a metro ID, publish the first n events in that metro area -}
		def metroShows(metroID, n) =
		  val base = "http://api.songkick.com/api/3.0/metro_areas/" + metroID + "/calendar.xml"
		  val query = {. per_page = n .}
		  val response = SongKickHTTP(base, query).get()
		  ReadXML(response) 
		    >xml("resultsPage",xml("results", m))>
		  m 
		    >XMLElement("event", _, _)>
		  m
		
		{- 
		  Extract an xs:datetime or xs:date, depending on which is available. 
		  Halt if argument is silent.  
		-}
		def extractDT(info) =
		  info.datetime >dt> Iff(dt.isEmpty()) >> dt ;
		  info.date >d> Iff(d.isEmpty()) >> d
		  
  metroShows(lookupMetro(city), numberOfShows) >event>
  (
  val xattr(_, {. displayName = title .}) = event
  val xattr(_, {. uri = uri .}) = event
  val xml(_, XMLElement("location", {. city = city .}, _)) = event
  val xml(_, XMLElement("venue", {. displayName = venue .}, _)) = event
  val xml(_, XMLElement("start", startInfo, _)) = event
  val xml(_, XMLElement("end", endInfo, _)) = event
  {.
    title = title,
    content = uri,
    location = venue + (" in " + city ; ""),
    startTime = extractDT(startInfo),
    endTime = extractDT(endInfo) ; ""
  .}
  )
  
-- execution
Println("Authenticating...") >>
GoogleCalendar.authenticate(oauth.authenticate("google", "scope", "http://www.google.com/calendar/feeds/")) >>
findShows(Prompt("Search in city: "), 20) >show> 
GoogleCalendar.addEventToCalendar(
  show.title, 
  show.content, 
  show.location, 
  show.startTime, 
  show.endTime) >>
stop
; "DONE"

