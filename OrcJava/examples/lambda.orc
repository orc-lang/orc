def Counter(n) =
	val nr = Buffer()
	nr.put(n) >>
	(lambda () =
		nr.get() >!n>
		nr.put(n+1) >> null)

Counter(0) >c> Metronome() >> c()