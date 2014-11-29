{- eventful.orc -- Orc program that fetches performing_arts events in Austin tomorrow listed by Eventful
 - 
 - $Id$
 - 
 - Created by jthywiss on Jan 13, 2013 2:08:15 PM
 -}

{- In order to run this demo, you must have:
 - 1. An Eventful API application key http://api.eventful.com/
 - 2. In the class path, /webservice.properties with webservice.eventful
 -    entries as shown in webservice.sample.properties
 -}

import site KeyedHTTP = "orc.lib.net.KeyedHTTP"
val EventfulHTTP = KeyedHTTP("webservice.properties", "eventful")

val searchResponse = ReadXML(EventfulHTTP("http://api.eventful.com/rest/events/search", {.
  location = "Austin, TX",
  date = "Tomorrow",
  category = "performing_arts",
  page_size = 100
.}).get())

Println("PERFORMING ARTS EVENTS IN AUSTIN TOMORROW:  Events by Eventful  http://eventful.com/") >>
searchResponse
  >xml("search",xml("events", eventsChild))>
(
  val xml("event", xml("title", title)) = eventsChild
  val xml("event", xml("url", url)) = eventsChild
  val xml("event", xml("start_time", startTime)) = eventsChild
  val xml("event", xml("venue_name", venueName)) = eventsChild #
  (startTime, title, venueName, url)
)
