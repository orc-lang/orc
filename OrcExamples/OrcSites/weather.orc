{-
In order to run this demo, you must have:
1. a google developer account
2. in the classpath, /google.properties (see google.sample.properties)
-}

import site Geocoder = "orc.lib.net.GoogleGeocoder"
import class LocalDate = "org.joda.time.LocalDate"
import class NOAAWeather = "orc.lib.net.NOAAWeather"

val date = LocalDate().plusDays(1)

Geocoder("google.properties") >geo>
Prompt("Address:") >addr>
geo(addr) >(lat, lon)>
Println(NOAAWeather.getDailyForecast(lat, lon, date, 1)) >>
stop