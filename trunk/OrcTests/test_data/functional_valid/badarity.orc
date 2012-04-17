{- badarity.orc
 - 
 - Confirm that certain sites complain if
 - they receive too many arguments.
 - 
 - Created by dkitchin on Feb 23, 2012
 -}

(

Channel(0) >> "Channel arity checking is faulty." 
;
Cell(0) >> "Cell arity checking is faulty."
;
BoundedChannel(0,1) >> "BoundedChannel arity checking is faulty."
;
Ref(0,1) >> "Ref arity checking is faulty."
;
Semaphore(0,1) >> "Semaphore arity checking is faulty." 
;
Counter(0,1) >> "Counter arity checking is faulty."
 
) :!: Bot

{- 
OUTPUT:
Error: orc.error.runtime.ArityMismatchException: Arity mismatch, expected 0 arguments, got 1 arguments.
Error: orc.error.runtime.ArityMismatchException: Arity mismatch, expected 0 arguments, got 1 arguments.
Error: orc.error.runtime.ArityMismatchException: Arity mismatch, expected 1 arguments, got 2 arguments.
Error: orc.error.runtime.ArityMismatchException: Arity mismatch, expected 1 arguments, got 2 arguments.
Error: orc.error.runtime.ArityMismatchException: Arity mismatch, expected 1 arguments, got 2 arguments.
Error: orc.error.runtime.ArityMismatchException: Arity mismatch, expected 1 arguments, got 2 arguments.
-}
