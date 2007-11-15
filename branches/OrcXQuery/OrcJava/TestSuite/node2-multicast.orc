def receiveLoop =
  Receive() >x> (let(x) | receiveLoop())
  
def sendLoop =
  Rtimer(2000) >> ((let (x,y) >> sendLoop where x in  Send("http://localhost:3101", "hi node 1! love,node2!")) where y in Send("http://localhost:3103", "hi node 3! love, node2"))
  
  sendLoop| receiveLoop

