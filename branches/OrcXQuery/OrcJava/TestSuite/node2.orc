def receiveLoop =
  Receive() >x> (let(x) | receiveLoop())
  
def sendLoop =
  Rtimer(1000) >> (Send("http://localhost:3103", "hi node 3!") >> sendLoop)
  
  sendLoop| receiveLoop

