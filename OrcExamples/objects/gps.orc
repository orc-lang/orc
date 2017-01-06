{- gps.orc -- A small example of how concurrent parts of a GPS might interact
 -}

class GPS {
	-- Publish positions as they are detected
	def position() = repeat({ readData() })
	
	-- Theoretically read data from real GPS device
	def readData() = Rwait(500) >> (1, 1)
}

class UI {
	def announce(s :: String) :: Signal = stop
	def updatePosition(s :: String) :: Signal = stop
}

class def Route(steps :: List[Top]) :: Route {
	def nextManeuver(p) = (steps, p)
}

def computeRoute(s, d) = Rwait(250) >> Route([s, d])

class Navigation {
	val gps :: GPS
	val ui :: UI
	val destination :: (Integer, Integer)

	val route = Ref()
	val position = Ref(null)
	
	-- This is not safe. It can call newPosition multiple times at the same time. However fixing it is complicated.
	-- There is a higher-order function that can deal with this problem easily.
	val _ = gps.position() >p> (if p /= position? then position := p >> newPosition(p) else stop)
	
	def newPosition(p) = route := computeRoute(p, destination)
					   | (route?).nextManeuver(p) >c> ui.announce(c)
					   | ui.updatePosition(p)
}

{|
val mygps = new GPS {
	val p = Ref((1,1))
	def readData() = super.readData() >> p?
} #

(new Navigation {
	val gps = mygps
	val ui = new UI {
		def announce(s :: String) :: Signal = Rwait(10) >> Println(s)
		def updatePosition(s :: String) :: Signal = Println("Pos: " + s)
	}
	val destination = (2, 10)
}) >> stop |
Rwait(900) >> mygps.p := (1,2) >> Println("Moved") >> stop |
Rwait(1500)
|} >> stop

{-
OUTPUT:
Pos: (1, 1)
([(1, 1), (2, 10)], (1, 1))
Moved
Pos: (1, 2)
([(1, 1), (2, 10)], (1, 2))
-}
