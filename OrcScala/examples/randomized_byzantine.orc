-- number of bad processes
val t = 2
-- total number of processes
val p = 8*t+1
-- list of process out channels
val channels = collect(lambda () = upto(p) >> Buffer())

-- Return a random boolean
def coin() = random(2) > 0

-- Return a default value if the first argument is null
def default(null, v) = v
def default(v, _) = v

-- Tally a list of votes and return
-- (majority vote value, majority vote count)
def tallyVotes(votes) =
  val table = Map()
  def tallyVote((mv, mt), v) =
    table(v) >t>
    t := default(t?, 0) + 1 >>
    if t? > mt then (v, mt+1)
    else (mv, mt)
  foldl(tallyVote, (0, 0), votes)
  
-- decision algorithm for a good process
def good(maj, tally) =
  val threshold = (if coin() then 5*t else 6*t)
  if tally > threshold then maj else 0
  
-- decision algorithm for a bad process
def bad(_, _) = random(2)

val nRounds = Ref(0)

-- generic process; pick is the decision algorithm
def process(pick)(out) =
  -- vote for a value
  def vote(value) = map(lambda (_) = out.put(value), channels)
  -- receive votes
  def receive() = map(lambda (c) = c.get(), channels)
  -- execute one round
  def round(value, n) =
    -- count the round
    nRounds := n >>
    -- collect and tally votes from every process
    tallyVotes(receive()) >(maj, tally)>
    pick(maj, tally) >newValue>
    vote(newValue) >>
    if tally > 7*t then newValue else round(newValue, n+1)
  random(2) >value>
  vote(value) >>
  round(value, 1)

  println("Bad: " + map(process(bad), take(t, channels))) >> stop
| println("Good: " + map(process(good), drop(t, channels))) >> stop
; println("Rounds: " + nRounds?) >> stop