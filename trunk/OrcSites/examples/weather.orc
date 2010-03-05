{-
In order to run this demo, you must have:
1. a google developer account
2. in the classpath, /google.properties (see google.sample.properties)
-}

site Geocoder = orc.lib.net.GoogleGeocoder
class LocalDate = org.joda.time.LocalDate
class NOAAWeather = orc.lib.net.NOAAWeather

val date = LocalDate().plusDays(1)

Geocoder("google.properties") >geo>
Prompt("Address:") >addr>
geo(addr) >(lat, lon)>
println(NOAAWeather.getDailyForecast(lat, lon, date, 1)) >>
stop