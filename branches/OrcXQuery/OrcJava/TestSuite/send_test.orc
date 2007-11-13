def receiveLoop =
  Receive() >x> (let(x) | receiveLoop())
  
def sendLoop(count) =
  Rtimer(1000) >> (Send("http://localhost:3100", count) >> sendLoop(count+1))

sendLoop(0) | receiveLoop