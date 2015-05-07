{--
Write a function which conducts an auction given
a list of "bidder" sites and a starting bid. An
auction consists of a number of bidding rounds
performed in sequence. A bidding round consists
of calling every bidder site in parallel, passing
the current maximum bid. Each bidder site may
return a higher value (a bid). The first caller
to return a higher bid sets a new maximum bid for
the next round. If no callers return a bid within
5 seconds, the auction (and round) ends. Your
function should return the value of the winning
bid.
--}

type Bid = Number

def auction(List[lambda(Bid) :: Bid], Bid) :: Bid
def auction(bidders, max) =
  val (done, bid) =
    Ltimer(1) >> (true, max)
    | each(bidders) >bidder>
      bidder(max) >bid>
      if(bid > max) >>
      (false, bid)
  println("Current bid: " +  max) >>
  if done then max else auction(bidders, bid)
  

def bidder(Bid)(Bid) :: Bid
def bidder(max)(n) = if(n < max) >> n + 1

auction(map(lambda (x :: Bid) = bidder(x), range(0,10)), 1)

{-
OUTPUT:
Current bid: 1
Current bid: 2
Current bid: 3
Current bid: 4
Current bid: 5
Current bid: 6
Current bid: 7
Current bid: 8
Current bid: 9
9
-}
