class LocalDate = org.joda.time.LocalDate
class Geocoder = orc.lib.net.Geocoder
class NOAAWeather = orc.lib.net.NOAAWeather

val date = LocalDate().plusDays(1)

Prompt("City:") >city>
Prompt("State:") >state>
Geocoder.locateCity(city, state) >(lat, lon)>
println(NOAAWeather.getDailyForecast(lat, lon, date, 1)) >>
stop