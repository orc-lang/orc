{-
PLEASE READ THE FOLLOWING BEFORE RUNNING

You will need a Google Calendar account, and you
will need popup blockers disabled.

This program will:
1. Ask you for temporary permission to read your Google
   Calendar.
2. Poll your calendar for "trip to LOCATION" events
   happening tomorrow.
3. Print weather conditions and local museum events at the
   destination of your trip.

We make use of a technology called "OAuth" to access your
Google Calendar without your username and password.  When
you run the program, you will be prompted to log in to
Google directly and then grant this program permission to
update your calendar.  You may revoke this permission at any
time through your "My Account" page on Google.
-}

------------ Helper functions follow ---------

{--
Given a calendar event description, return a probable
trip destination.  If none is found, halt.
--}
def extractLocation(description) =
  val parts = description.split(" to ")
  val len = parts.length()
  if len > 0 then parts(len-1)? else stop
  
{--
Given a probable location name, return a record representing
the weather forecast tomorrow at that location.  If the
location is not recognized, halt.
--}
def weatherTomorrow(location) =
  site GoogleWeather = orc.lib.net.GoogleWeather
  GoogleWeather(location) >a>
  if a.length() > 0 then a(1)? else stop
  
{--
Given a probable location name and search term, return an
array of event records occurring tomorrow at the location,
matching the given search term.  If no events are found or
the location is not recognized, return an empty array.
--}
def eventsTomorrow(location, term) =
  site LocalDateTime = orc.lib.date.LocalDateTimeFactory
  class Upcoming = orc.lib.net.Upcoming
  val tomorrow = LocalDateTime().plusDays(1).toLocalDate()
  val search = Upcoming("orc/orchard/upcoming.properties").eventSearch()
  search.search_text := term >>
  search.min_date := tomorrow >>
  search.max_date := tomorrow >>
  search.location := location >>
  search.run()

{--
Remove items from the given buffer, publishing each item
removed only the first time it is seen.  The effect is that
each value published by getUnique(b) is unique.
--}
def getUnique(b) =
  val seen = Set()
  def loop() =
    b.get() >x>
    if seen(x)?
    then loop()
    else seen(x) := true >> ( x | loop() )
  loop()

{--
Poll the user's calendar every 10 seconds and publish
each trip destination the first time it is noticed.
--}
def poll() =
  class GoogleCalendar = orc.lib.net.GoogleCalendar
  val oauth = OAuthProvider("orc/orchard/oauth.properties")
  val calendar = GoogleCalendar.authenticate(oauth, "google")
  -- Buffer of locations to be processed
  val locations = Buffer()
  getUnique(locations)
  | metronome(10000) >>
    calendar.getTomorrowEvents() >feed>
    each(feed.getEntries()) >entry>
    extractLocation(entry.getTitle().getPlainText()) >location>
    locations.put(location) >>
    stop

{--
Notify the user of unique trip destinations as they appear
in the locations buffer.  Never publishes.
--}
def notify(location) =
  val weather = weatherTomorrow(location)
  val events = eventsTomorrow(location, "museum")
  println(location) >>
  println("Weather: sky:" + weather.sky?
    + " high:" + weather.high? + "F"
    + " low:" + weather.low? + "F") >>
  each(events) >event>
  println(event.start_time? + " " + event.name? + ": " + event.venue_name?) >>
  stop

------------ Goal expression of program ---------

poll() >location> notify(location)
