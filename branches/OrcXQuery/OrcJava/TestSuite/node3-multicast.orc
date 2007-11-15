def receiveLoop =
  Receive() >x> (let(x) | receiveLoop())
  
def sendLoop =
  Rtimer(2000) >> ((let (x,y) >> sendLoop where x in  Send("http://localhost:3101", "hi node 1! love,node3!")) where y in Send("http://localhost:3102", "hi node 2! love, node3"))
  
  sendLoop| receiveLoop

