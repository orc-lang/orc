def receiveLoop =
  Receive() >x> (let(x) | receiveLoop())
  
def sendLoop =
  Rtimer(1000) >> (Send("http://localhost:3101", "hi node 1!") >> sendLoop)
  
  sendLoop| receiveLoop

