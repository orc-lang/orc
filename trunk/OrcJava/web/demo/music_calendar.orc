-- Orchestrated music calendar demo
val p1 = "site:www.myspace.com 'Austin, TX' 'Band Members'"
val p2 = "site:www.myspace.com 'Austin, Texas' 'Band Members'"

(GoogleSearch(p1) | GoogleSearch(p2)) >searchResults>
	each(searchResults.getResultElements()) >searchResult>
		searchResult.getURL() >!mySpaceURL>
		MySpace.scrapeMusicShows(mySpaceURL) >musicShows>
		AddMusicShows(musicShows)
