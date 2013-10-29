{- unbounded_rec_on_stop.orc
 - Test for unbounded recursion on stop in induction argument of specific prelude functions
 - This includes 2 functions in lists.inc. It would not hurt to add others to avoid regressions.
 - 
 - $Id$
 - 
 - Created by amp on Nov 16, 2012 3:17:44 PM
 -}

def haltsQuickly(lambda() :: Top) :: String
def haltsQuickly(s) = x <x< ((s() >> "Published" ; "Halted") | Rwait(100) >> "Timedout")

haltsQuickly(lambda () = sortUnique(stop)) |
haltsQuickly(lambda () = sort(stop))

{- 
OUTPUT:
"Halted"
"Halted"
-}
