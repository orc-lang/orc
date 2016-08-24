-- As shown in OrcO: A Concurrency-First Approach to Objects Figure 2

def findAirline(name) =
  { Read(Prompt("Enter "+name+"’s price")) }

def askAirline(airline) =
  val response =
    {| (airline() ; 99999) |
       (Rwait(15000) >> 99999)
    |}
  if ~(response = 99999) then response else stop
  
def betterPrice(best, airline) =
  val price = askAirline(airline)
  min(best, price) ; best

betterPrice(
  betterPrice(
    betterPrice(99999, findAirline("Delta")),
    findAirline("United")
  ), findAirline("Southwest")
)