{- kill_detection.orc -- Orc program kill_detection
 -
 - Created by amp on Dec 6, 2014 1:43:10 PM
 -}

site runOnKillHandler(check :: lambda() :: Top, callback :: lambda() :: Top) =
  (check() ; callback()) >> stop | signal

-- Call callable when this def call is killed.
-- As a side effect this will never terminate.
def runOnKill(callback :: lambda() :: Top) = 
  site check() :: Top = Rwait(1000) >> Println("1 sec") -- This is a hack. It should really block forever.
  runOnKillHandler(check, callback)

-- Test

def simulatedRead() = 
  Println("Open") >>
  runOnKill({ Println("Closed") }) >> 
  repeat({ "line" })

{| simulatedRead() |}

{-
OUTPUT:
Open
"line"
Closed
-}
{-
OUTPUT:
Open
Closed
"line"
-}