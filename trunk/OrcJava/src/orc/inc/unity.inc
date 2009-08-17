{- UNITY execution scheduler -}

def unity(List[lambda() :: Top]) :: Bot
def unity(fs) =
  {- First, transfer all the functions from the argument list fs to array fnarray for easy indexing -}
  val arlen = length(fs)
  val fnarray = Array[lambda() :: Top](arlen)

  {- function populate() transfers from fs to fnarray -}
  def populate(Integer, List[lambda() :: Top]) :: Top
  def populate(_,[]) = signal
  def populate(i,g:gs) = fnarray(i) := g >> populate(i+1,gs)

  {- Execute a random statement and loop. Randomness guarantees fairness. -}
  def exec() :: Bot
  def exec() = random(arlen) >j> fnarray(j)?() >> stop ; exec()

  {- Initiate the work -}
  populate(0,fs) >> exec()
