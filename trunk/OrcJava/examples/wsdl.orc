site Google = orc.lib.music_calendar.GoogleSearch
val GoogleDevKey = "dceJmvRQFHKEMK0xSxG9FqYOwqQVRDqQ"
def GoogleSearch(keywords) = Google.doGoogleSearch(
	GoogleDevKey, keywords, 0, 10, true, "", true, "", "", "")

GoogleSearch("test")