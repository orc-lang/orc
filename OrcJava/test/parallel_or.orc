def parallelOr(M, N) =
	let(z)
	where z in { if(x) | if(y) | or(x, y) };
		  x in M;
		  y in N
	
def A = Rtimer(3000) >> let(true)
def B = Rtimer(1000) >> let(false)
def C = Rtimer(500) >> let(true)

parallelOr(A, B) 
>!> 
parallelOr(A, C)
>!> 
parallelOr(B, B)
