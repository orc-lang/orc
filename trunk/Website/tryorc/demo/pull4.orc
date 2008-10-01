{-
Call two sites with random delay;
Print the site which returns first together with its delay.
Repeat the experiment 10 times.
-}

signals(10) >> (
    if(~b) >> println("first alternative after " + x + " milliseconds") >>stop
  | if(b)  >> println("second alternative after " + x + " milliseconds") >>stop
  
    <(x,b)<   random(1000) >x> Rtimer(x) >> (x,false)
            | random(1000) >x> Rtimer(x) >> (x,true)
)
