val phrase1 = "site:www.myspace.com 'Austin, TX' 'Band Members'"
val phrase2 = "site:www.myspace.com 'Austin, Texas' 'Band Members'"

(GoogleSearch(phrase1) | GoogleSearch(phrase2)) >searchResults>
	each(searchResults.getResultElements()) >searchResult>
		searchResult.getURL() >!mySpaceURL>
		MySpace.scrapeMusicShows(mySpaceURL) >musicShows>
		AddMusicShows(musicShows)