{- randomized-byzantine.orc -- Orc program with a randomized solution to Byzantine agreement
 -}

import class Map = "scala.collection.mutable.HashMap"

-- number of bad processes
val t = 2
-- total number of processes
val p = 8*t+1
-- list of process out channels
val channels = collect(lambda () = upto(p) >> Channel[Integer]())

-- Return a random boolean
def coin() = Random(2) :> 0

-- Return a default value if the first argument is None
def default[A](Option[A], A) :: A
def default(None(), v) = v
def default(Some(v), _) = v

-- Tally a list of votes and return
-- (majority vote value, majority vote count)
def tallyVotes(votes :: List[Integer]) =
  val table = Map[Integer, Integer]()
  def tallyVote((mv, mt) :: (Integer, Integer), v :: Integer) =
    val t = default(table.get(v) :!: Option[Integer], 0)
    val newt = t+1
    table.put(v, newt) >>
    ( 
    if newt :> mt then 
      (v, newt)
    else 
      (mv, mt) 
    )
  foldl(tallyVote, (0, 0), votes)
  
-- decision algorithm for a good process
def good(maj :: Integer, tally :: Integer) =
  val threshold = (if coin() then 5*t else 6*t)
  if tally :> threshold then maj else 0
  
-- decision algorithm for a bad process
def bad(_ :: Integer, _ :: Integer) = Random(2)

val nRounds = Ref[Integer](0)

-- generic process; pick is the decision algorithm
def process(pick :: lambda(Integer, Integer) :: Integer) :: lambda(Channel[Integer]) :: Integer = 
  lambda (out) = (
    -- vote for a value
    def vote(value :: Integer) = map(lambda (_::Top) = out.put(value), channels)
    -- receive votes
    def receive() = map(lambda (c :: Channel[Integer]) = c.get(), channels)
    -- execute one round
    def round(value :: Integer, n :: Integer) :: Integer =
      -- count the round
      nRounds := n >>
      -- collect and tally votes from every process
      tallyVotes(receive()) >(maj, tally)>
      pick(maj, tally) >newValue>
      vote(newValue) >>
      ( if tally :> 7*t then newValue else round(newValue, n+1) )  #
    Random(2) >value>
    vote(value) >>
    round(value, 1)
  )

  Println("Bad: " + map(process(bad), take(t, channels))) >> stop
| Println("Good: " + map(process(good), drop(t, channels))) >> stop
; Println("Rounds: " + nRounds?) >> stop
