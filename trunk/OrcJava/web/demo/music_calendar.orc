-- Orchestrated music calendar demo
val p1 = "site:www.myspace.com 'Austin, TX' 'Band Members'"
val p2 = "site:www.myspace.com 'Austin, Texas' 'Band Members'"

(GoogleSearch(p1) | GoogleSearch(p2)) >results>
  each(results.getResultElements()) >result>
    result.getURL() >url>
    println(url) >>
    MySpace.scrapeMusicShows(url) >shows>
    AddMusicShows(shows)
