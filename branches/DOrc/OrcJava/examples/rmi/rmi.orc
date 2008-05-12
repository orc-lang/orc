val remote = Remote("orc").newServer()

def test1() = c.put(1)>>null | c.put(2)>>null | c.get() | c.get()
	<c< Buffer() @ remote
	
def test2() = c.put(1)>>null | (c.put(2)>>null)@remote | c.get() | c.get()
	<c< Buffer()

def test3() = c.put(1)>>null | (c.put(2)>>null)@remote | c.get() | c.get()
	<c< Buffer() @ remote

def test4() = c.put(1)>>null | (c.put(2)>>null)@remote | c.get() | c.get()
	<c< Buffer() @ remote

def test5() = x
	<x< Rtimer(10000) | (Metronome() >> print(".") >> null)@remote
	
def test6() =
	val c = Buffer()
	def reader() = c.get() >x>
		(println("Got ", x) >> null
			| reader())
	def writer() = random(100) >x>
		(println("Put ", x) >> null
			| c.put(x) >> Rtimer(2000) >> writer())
	reader()@remote | writer()
	
test6()