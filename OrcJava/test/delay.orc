def Delayed(N, t) =
	Rtimer(t) >> let(u)
	where u in N

def Priority(M, N, t) =
    let(z)
    where
	z in M | Delayed(N, t)

def A = Rtimer(8000) >> let(42)
def B = let(99)

Priority(A, B, 10000)
