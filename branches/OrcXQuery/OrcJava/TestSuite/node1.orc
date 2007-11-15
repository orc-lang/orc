def receiveLoop =
  Receive() >x> (let(x) | receiveLoop())
  
def sendLoop =
  Rtimer(1000) >> (Send("http://localhost:3102", "hi node 2!") >> sendLoop)
  
  sendLoop| receiveLoop

