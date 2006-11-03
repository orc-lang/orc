import "test/wsdecls.orc"

def Response(M, i, h) = 
	{ if(b) >> add(i, 1) >ip> Response(M, ip, h) 
	| not(b) >nb> if(nb) >> let(i) }
	where b in {
    	M("grammer") >> let(true) | 
    	Atimer(h) >> let(false)
    	}

dot(google,"spellcheck")>M>

clock >c> add(c, 60000) >n> Response(M, 0, n)


