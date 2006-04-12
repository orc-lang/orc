-- these are sample code from the paper, but have not been tested yet

def MailOnce(a) =
	Email(a, m)
	where m in { CNN | BBC }

def RepeatQuery =
	Metronome
	>> Query
	>x> Accept(x)

def Delayed(N, t) =
	Rtimer(t) >> let(u)
	where u in N
	
def Priority(M, N, t) =
	M | Delayed(N, t)

def tally(MS) =
	if(empty(MS)) >> let(0)
	| if(not(empty(MS))) >> {
		add(u, v)    -- add(u, v) returns the sum of u and v
		where 
			u in {
				call(first(M)) >> let(1) 
				| Rtimer(10000) >> let(0)
			};
			v in tally(rest(MS))
	}

def join(M, N) =
	let(u, v) >> signal
	where u in M;
		  v in N

def parallelOr(M, N) =
	let(z)
	where z in { if(x) | if(y) | or(x, y) };
		  x in M;
		  y in N

Metronome
