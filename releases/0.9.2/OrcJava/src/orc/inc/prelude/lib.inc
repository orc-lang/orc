def lett(x) = if(x) >> true
def letf(x) = if(~x) >> false

def Or(a,b)  = z <z< (lett(a) | lett(b) | a || b)
def And(a,b) = z <z< (letf(a) | letf(b) | a && b)

def All(p,[]) = true
def All(p,h:t) = And(p(h),All(p,t))

def Exists(p,[]) = false
def Exists(p,h:t) = Or(p(h),Exists(p,t))

-- Note: Only supports the composition of single-argument functions
def compose(f,g) = 
  def h(x) = f(g(x))
    h 

def repeat(S) = S() >!v> repeat(S)