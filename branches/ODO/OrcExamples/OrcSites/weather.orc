{- weather.orc -- Orc program that gets NOAA's daily weather forecast for a give address
 -}

include "net.inc"

import class LocalDate = "org.joda.time.LocalDate"
import class NOAAWeather = "orc.lib.net.NOAAWeather"

val date = LocalDate().plusDays(1)

Prompt("Address:") >addr>
GoogleGeocoder(addr) >(found, lat, lon)>
Println("NOAA weather forcast for "+found+" on "+date+":") >>
Println(NOAAWeather.getDailyForecast(lat, lon, date, 1)) >>
stop
