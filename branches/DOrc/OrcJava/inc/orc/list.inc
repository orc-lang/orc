def each([]) = null
def each(h:t) = h | each(t)

def map(f,[]) = []
def map(f,h:t) = f(h):map(f,t)

def rev(l) = 
	def tailrev([],x) = x
	def tailrev(h:t,x) = tailrev(t,h:x)
	tailrev(l,[]) 

def append([],l) = l
def append(h:t,l) = h:append(t,l)

def foldl(f,[],s) = s
def foldl(f,h:t,s) = foldl(f,t,f(h,s))

def foldr(f,l,s) = foldl(f,rev(l),s)

def zip([],_) = []
def zip(_,[]) = []
def zip(x:xs,y:ys) = (x,y):zip(xs,ys)

def unzip([]) = ([],[])
def unzip((x,y):z) = (x:xs,y:ys) <(xs,ys)< unzip(z)

def length([]) = 0
def length(h:t) = 1 + length(t)




 