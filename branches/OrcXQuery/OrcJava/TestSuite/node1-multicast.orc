def receiveLoop =
  Receive() >x> (let(x) | receiveLoop())
  
def sendLoop =
  Rtimer(2000) >> ((let (x,y) >> sendLoop where x in  Send("http://localhost:3102", "hi node 2! love,node1!")) where y in Send("http://localhost:3103", "hi node 3! love, node1"))
  
  sendLoop| receiveLoop

