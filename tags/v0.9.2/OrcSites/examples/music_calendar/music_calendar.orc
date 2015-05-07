val GoogleDevKey = "YOURDEVKEY"
val GoogleCalendarUsername = "USER"
val GoogleCalendarPassword = "PASS"

-- imports
site MySpace = orc.lib.music_calendar.MySpace
site GoogleCalendar = orc.lib.music_calendar.GoogleCalendar
site Google = orc.lib.music_calendar.GoogleSearch

-- declarations
val Google = Webservice("http://api.google.com/GoogleSearch.wsdl")
val phrase1 = "site:www.myspace.com 'Austin, TX' 'Band Members'"
val phrase2 = "site:www.myspace.com 'Austin, Texas' 'Band Members'"

-- definitions
def GoogleSearch(keywords) = Google.doGoogleSearch(
	GoogleDevKey, keywords, 0, 10, true, "", true, "", "", "")
def AddMusicShows(musicShows) = GoogleCalendar.addMusicShows(
	GoogleCalendarUsername, GoogleCalendarPassword,	musicShows) >> null

-- execution
(GoogleSearch(phrase1) | GoogleSearch(phrase2)) >searchResults>
	each(searchResults.getResultElements()) >searchResult>
		searchResult.getURL() >!mySpaceURL>
		MySpace.scrapeMusicShows(mySpaceURL) >musicShows>
		AddMusicShows(musicShows)