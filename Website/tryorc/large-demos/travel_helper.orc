{-
PLEASE READ THE FOLLOWING BEFORE RUNNING

You will need a Google Calendar account, and you
will need popup blockers disabled.

This program will:
1. Ask you for temporary permission to read your Google
   Calendar.
2. Poll your calendar for any "trip to CITY, STATE"
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

type Trip = Trip(_, _)

{--
Given a calendar event description, return a probable
trip destination.  If none is found, halt.
--}
def extractLocation(description) =
  val desc = description.trim().toUpperCase()
  if(desc.startsWith("TRIP TO ")) >>
  desc.substring(8)

import site Geocoder = "orc.lib.net.GoogleGeocoder"
val geo = Geocoder("orc/orchard/orchard.properties")
{--
Given a trip, return a record representing
the weather forecast for that trip.  If the
location is not recognized, halt.
--}
def weather(Trip(time, location)) =
  import class NOAAWeather = "orc.lib.net.NOAAWeather"
  import class LocalDate = "org.joda.time.LocalDate"
  geo(location) >(lat, lon)>
  NOAAWeather.getDailyForecast(lat, lon, time.toLocalDate(), 1)
 
{--
Given a trip, return an array of event records during
that trip. If no events are found or the location is
not recognized, return an empty array.
--}
def events(Trip(time, location), term) =
  import class Upcoming = "orc.lib.net.Upcoming"
  val search = Upcoming("orc/orchard/orchard.properties").eventSearch()
  search.search_text := term >>
  search.min_date := time.toLocalDate() >>
  search.max_date := time.toLocalDate().plusDays(1) >>
  search.location := location >>
  search.run()

{--
Poll the calendar at the given interval for probable trips.
Call "put" with each trip the first time it is seen.
Never publishes.
--}
def poll(interval, put) =
  import class DateTime = "org.joda.time.DateTime"
  -- Buffer of trips to be processed
  val trips = Channel()
  -- Send unique trips
  def sendUniqueTrips() =
    import class Set = "scala.collection.mutable.HashSet"
    val seen = Set()
    def loop() =
      val trip = trips.get()
      val key = trip.toString()
      if seen.contains(key)
      then loop()
      else seen.add(key) >> put(trip) >> loop()
    loop()
  ------------- goal expression for poll() ----------------
  import class GoogleCalendar = "orc.lib.net.GoogleCalendar"
  val oauth = OAuthProvider("orc/orchard/orchard.properties")
  sendUniqueTrips()
  | Println("Authenticating...") >>
    GoogleCalendar.authenticate(oauth, "google") >calendar>
    calendar.getDefaultTimeZone() >tz>
    metronome(interval) >>
    Println("Polling...") >>
    -- find the next day in the user's calendar timezone
    DateTime().withZone(tz).withTime(0,0,0,0).plusDays(1) >start>
    -- get events for that day
    calendar.getEvents(start, start.plusDays(1)) >feed>
    -- add each trip to the buffer
    each(feed.getEntries()) >entry>
    extractLocation(entry.getTitle().getPlainText()) >location>
    DateTime(entry.getTimes().get(0).getStartTime().toString()) >time>
    trips.put(Trip(time, location)) >>
    stop

{--
Print trip information obtained by calling "get".
Repeats indefinitely, and never publishes.
--}
def notify(get) =
  get() >Trip(time, location) as trip> (
    val forecast = weather(trip)
    val events = events(trip, "museum")
    -- at least one of forecast or events must be present
    Let(forecast | events) >>
    Println("TRIP TO " + location + " on " + time) >>
    -- print the forecast if present
    (forecast >> Println("FORECAST:") >> Println(forecast) ; signal) >>
    -- then print the events if present
    Println("EVENTS:") >>
    ( each(events) >event>
      Println(event.start_time? + " " + event.name? + ": " + event.venue_name?)
      ; Println("None found.")
    ) >>
    stop
    ; notify(get)
  )

------------ Goal expression of program ---------

val buffer = Channel()
poll(10000, buffer.put) | notify(buffer.get)
