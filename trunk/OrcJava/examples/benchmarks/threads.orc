{-
This program creates 2^20 (or about 1 million) threads
and waits for them to terminate.
-}
val N = 20
def threads(n) = if n/=0 then n-1 >n'> (threads(n') | threads(n'))
threads(N) ; signal
