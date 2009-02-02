{-
In order to run this demo, you must have:
1. an upcoming.yahoo.com developer account
2. in the classpath, /upcoming.properties with
   orc.lib.net.upcoming.key set to your API key
-}

include "date.inc"
class Upcoming = orc.lib.net.Upcoming

val upcoming = Upcoming("upcoming.properties")
val search = upcoming.eventSearch()
val tomorrow = LocalDateTime().plusDays(1).toLocalDate()
search.search_text := "band" >>
search.min_date := tomorrow >>
search.max_date := tomorrow >>
search.run() >events>
each(events) >event>
(event.start_time?, event.name?, event.venue_city?, event.venue_name?)