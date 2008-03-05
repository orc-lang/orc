def lett(x) = if(x) >> true
def letf(x) = if(~x) >> false

def Or(a,b)  = z <z< (lett(a) | lett(b) | a || b)
def And(a,b) = z <z< (letf(a) | letf(b) | a && b)
